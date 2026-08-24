# -*- coding: utf-8 -*-
"""
Aether python-agent 服务（阶段0 骨架）

承担 Java 不便实现的 AI agent 任务（BackEnd-Plan §7.2）：
- 书籍 txt 自动分章分节（启发式预分 + LLM 精调）
- 歌词时间轴 AI 对齐（可选增强，BackEnd-Plan §7.6）

部署：docker-compose 中 python-agent 容器，仅内网可达（BackEnd-Plan §11）。
本阶段仅搭建骨架，分章逻辑在阶段5实现。
"""

from fastapi import FastAPI

app = FastAPI(title="Aether Agent", version="0.1.0")


@app.get("/health")
def health() -> dict:
    """存活探针"""
    return {"status": "UP"}


@app.post("/split-book")
def split_book(payload: dict) -> dict:
    """书籍分章接口（占位，阶段5实现）"""
    return {"status": "NOT_IMPLEMENTED", "chapters": []}


@app.post("/align-lyric")
def align_lyric(payload: dict) -> dict:
    """歌词时间轴对齐接口（占位，可选增强）"""
    return {"status": "NOT_IMPLEMENTED", "lyrics": []}
