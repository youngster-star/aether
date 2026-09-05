# -*- coding: utf-8 -*-
"""
Aether python-agent 服务（BackEnd-Plan §7.2）

承担 Java 不便实现的 AI agent 任务：
- 书籍 txt 自动分章分节（启发式预分 + LLM 精调，LLM 失败静默回退启发式）
- 歌词时间轴 AI 对齐（可选增强，BackEnd-Plan §7.6，阶段 5 不实装）

LLM 精调配置（环境变量）：
- AETHER_AGENT_LLM: ollama / disabled（默认 disabled，纯启发式）
- AETHER_AGENT_LLM_URL: Ollama 地址（默认 http://localhost:11434）
- AETHER_AGENT_LLM_MODEL: 模型名（默认 qwen2.5:7b）

部署：docker-compose python-agent 容器，仅内网可达（BackEnd-Plan §11）。
"""

import json
import os
import re
from typing import Optional

import requests
from fastapi import FastAPI

app = FastAPI(title="Aether Agent", version="0.5.0")

# ===== 配置（环境变量驱动，默认纯启发式） =====
LLM_PROVIDER = os.getenv("AETHER_AGENT_LLM", "disabled")
LLM_URL = os.getenv("AETHER_AGENT_LLM_URL", "http://localhost:11434")
LLM_MODEL = os.getenv("AETHER_AGENT_LLM_MODEL", "qwen2.5:7b")
LLM_TIMEOUT_SECONDS = 90

# ===== 启发式规则（与 Java SplitHeuristics 同构，降级互备） =====

# 章标题：第X章/回/卷/部/篇、序章/楔子/引子/尾声/后记/番外、Chapter N
CHAPTER_TITLE = re.compile(
    r"^(第[0-9一二三四五六七八九十百千零〇两]+[章节回卷部篇]).*"
    r"|^(序章|序言|序|自序|前言|楔子|引子|尾声|后记|跋|番外).{0,40}"
    r"|^(Chapter|CHAPTER)\s+\d+.*"
)

# 节标题：第X节 → level 2
SECTION_TITLE = re.compile(r"^(第[0-9一二三四五六七八九十百千零〇两]+节).{0,40}")

# 标题行最大长度（超过视为正文引用"第X章"字样，不切分）
MAX_TITLE_LENGTH = 50


@app.get("/health")
def health() -> dict:
    """存活探针"""
    return {"status": "UP", "llm": LLM_PROVIDER}


@app.post("/split-book")
def split_book(payload: dict) -> dict:
    """
    书籍分章接口（BackEnd-Plan §7.2）

    入参: { "text": "全文或分块", "chunkIndex": 0, "chunkTotal": 1 }
    出参: { "chapters": [ { "title": "第1章 ...", "level": 1, "paragraphs": ["...", "..."] } ], "source": "heuristic+llm" }
    """
    text = payload.get("text", "")
    if not text.strip():
        return {"chapters": [], "source": "heuristic"}

    # 第一步：启发式预分
    chapters = heuristic_split(text)
    source = "heuristic"

    # 第二步：LLM 精调（未配置/失败/输出不合法 → 静默回退启发式结果）
    refined = llm_refine(text, chapters)
    if refined is not None:
        chapters = refined
        source = "heuristic+llm"

    return {"chapters": chapters, "source": source}


def heuristic_split(text: str) -> list[dict]:
    """启发式预分：标题正则切章/节，空行分段（组内行以空格连接为一段）"""
    chapters: list[dict] = []
    current_title: Optional[str] = None
    current_level = 1
    current_paragraphs: list[str] = []
    line_buffer: list[str] = []

    def flush_paragraph() -> None:
        if line_buffer:
            current_paragraphs.append(" ".join(line_buffer))
            line_buffer.clear()

    for raw_line in re.split(r"\r\n|\n|\r", text):
        line = raw_line.strip()
        if not line:
            flush_paragraph()
            continue
        level = match_title_level(line)
        if level is not None:
            flush_paragraph()
            if current_title is not None:
                chapters.append(
                    {"title": current_title, "level": current_level, "paragraphs": current_paragraphs}
                )
                current_paragraphs = []
            current_title = line
            current_level = level
            continue
        line_buffer.append(line)

    flush_paragraph()
    if current_title is not None:
        chapters.append(
            {"title": current_title, "level": current_level, "paragraphs": current_paragraphs}
        )
    elif not chapters:
        # 无任何标题：全文单章兜底（管理端可编辑拆分）
        chapters.append({"title": "全文", "level": 1, "paragraphs": current_paragraphs})
    return chapters


def match_title_level(line: str) -> Optional[int]:
    """标题行判定（返回 1 章 / 2 节；非标题返回 None）"""
    if len(line) > MAX_TITLE_LENGTH:
        return None
    if SECTION_TITLE.match(line):
        return 2
    if CHAPTER_TITLE.match(line):
        return 1
    return None


def llm_refine(text: str, heuristic: list[dict]) -> Optional[list[dict]]:
    """
    LLM 精调（Ollama 优先）：输入全文与启发式预分结果，要求输出结构化 JSON 分章

    失败（未启用/网络错误/超时/JSON 不合法）一律返回 None（静默回退启发式）。
    """
    if LLM_PROVIDER != "ollama":
        return None
    prompt = (
        "你是中文书籍编辑。请根据以下全文重新划分章节（章 level=1，节 level=2），"
        "识别标题行（如 第X章/第X节/序章/尾声），正文按段落拆分。"
        "只输出 JSON，格式：{\"chapters\":[{\"title\":\"...\",\"level\":1,\"paragraphs\":[\"...\"]}]}\n\n"
        f"启发式预分结果（供参考）：\n{json.dumps(heuristic, ensure_ascii=False)[:2000]}\n\n"
        f"全文：\n{text}"
    )
    try:
        response = requests.post(
            f"{LLM_URL}/api/generate",
            json={"model": LLM_MODEL, "prompt": prompt, "stream": False,
                  "format": "json", "options": {"temperature": 0.1}},
            timeout=LLM_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        content = response.json().get("response", "")
        data = json.loads(content)
        chapters = data.get("chapters")
        if not isinstance(chapters, list) or not chapters:
            return None
        cleaned = []
        for chapter in chapters:
            title = str(chapter.get("title", "")).strip()
            paragraphs = [str(p).strip() for p in chapter.get("paragraphs", []) if str(p).strip()]
            level = 2 if chapter.get("level") == 2 else 1
            if title:
                cleaned.append({"title": title, "level": level, "paragraphs": paragraphs})
        return cleaned or None
    except Exception:
        # LLM 不可用：静默回退启发式（断网降级是完成标准）
        return None


@app.post("/align-lyric")
def align_lyric(payload: dict) -> dict:
    """歌词时间轴对齐接口（占位，§7.6 可选增强，后续阶段实装）"""
    return {"status": "NOT_IMPLEMENTED", "lyrics": []}
