#!/usr/bin/env node
/**
 * Aether 测试门禁（Claude Code PreToolUse hook）
 *
 * 拦截 git push 前的检查逻辑：
 *   1. 命令含 --no-verify            → 放行（逃生口）
 *   2. 本次推送仅文档改动（md/txt）   → 放行
 *   3. .claude/.test-pass.json 有效   → 放行（HEAD 匹配 + 24 小时内）
 *   4. 其余情况                       → deny，提示先跑 /test
 *
 * stdin：Claude Code hook 标准 JSON（含 tool_input.command，字符串化）
 * stdout：单行 JSON（hookSpecificOutput），不得输出其他内容
 */
'use strict';

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

/** 输出 hook 判定结果（stdout 只能有这一行 JSON） */
function reply(decision, reason) {
  const out = {
    hookSpecificOutput: {
      hookEventName: 'PreToolUse',
      permissionDecision: decision,
    },
  };
  if (reason) out.hookSpecificOutput.permissionDecisionReason = reason;
  process.stdout.write(JSON.stringify(out));
}

/** 执行 shell 命令，失败返回 null */
function run(cmd) {
  try {
    return execSync(cmd, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
  } catch (e) {
    return null;
  }
}

try {
  // 解析 hook 输入：cwd 定位仓库根，tool_input 提取命令
  let command = '';
  try {
    const json = JSON.parse(fs.readFileSync(0, 'utf8') || '{}');
    process.chdir(json.cwd || process.cwd());
    const ti = typeof json.tool_input === 'string' ? JSON.parse(json.tool_input) : json.tool_input || {};
    command = ti.command || '';
  } catch (e) {
    reply('allow'); // 输入不可解析：fail-open，不阻塞开发
    process.exit(0);
  }

  // 1. --no-verify 逃生口
  if (/\s--no-verify(\s|$)/.test(command)) {
    reply('allow');
    process.exit(0);
  }

  // 2. 纯文档改动（md/txt）放行：@{u}..HEAD 差异，首次推送回退 HEAD 提交文件
  let files = run('git diff --name-only @{u}..HEAD');
  if (files === null) files = run('git diff-tree --no-commit-id --name-only -r HEAD');
  if (files !== null) {
    const list = files.split('\n').filter(Boolean);
    const docsOnly = list.length > 0 && list.every((f) => /\.(md|txt)$/.test(f));
    if (docsOnly) {
      reply('allow');
      process.exit(0);
    }
  }

  // 3. 测试通过标记：HEAD 匹配 + 24 小时内有效
  const passPath = path.join(process.cwd(), '.claude', '.test-pass.json');
  let pass = null;
  try {
    pass = JSON.parse(fs.readFileSync(passPath, 'utf8'));
  } catch (e) {
    pass = null;
  }
  const head = run('git rev-parse HEAD');
  const valid = pass && head && pass.head === head &&
    typeof pass.timestamp === 'number' &&
    Date.now() - pass.timestamp < 24 * 3600 * 1000;
  if (valid) {
    reply('allow');
    process.exit(0);
  }

  reply('deny', '测试门禁未通过：请先运行 /test 全部维度通过后再 push（紧急情况可 git push --no-verify 跳过）');
  process.exit(0);
} catch (e) {
  reply('allow'); // 门禁脚本自身异常：fail-open，避免阻塞开发
  process.exit(0);
}
