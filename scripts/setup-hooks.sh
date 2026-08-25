#!/usr/bin/env bash
# Aether 测试门禁初始化：注册 git 原生 pre-push 兜底 hook
#
# 说明：core.hooksPath 写入仓库本地 .git/config，不随仓库提交，
#       每个克隆者/新机器需执行一次本脚本。
# 用法：bash scripts/setup-hooks.sh

set -e

git config core.hooksPath .githooks
echo "已注册 git 兜底门禁：core.hooksPath = $(git config core.hooksPath)"
echo "Claude Code 侧门禁（.claude/hooks/pre-push-gate.js）随项目自动生效，无需额外配置"
