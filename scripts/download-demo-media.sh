#!/usr/bin/env bash
# =====================================================================
# Aether 演示测试数据 · 媒体下载脚本
#
# 下载 seed-demo-data.sql（backend/src/main/resources/db/）引用的真实
# 网络媒体到本地存储目录 backend/data/storage/files/seed/：
#   图片 7 张（webp/jpg/png）+ 视频 3 段（mp4）+ 音频 3 首（mp3）
#
# 用法：bash scripts/download-demo-media.sh
# 注意：
#   * 媒体文件不入库（backend/data/ 已 gitignore），每台开发机执行一次
#   * 下载完成后文件 SHA-256 应与 seed-demo-data.sql 中 storage_file
#     记录一致（改动素材后需同步更新 SQL 中的哈希/尺寸/时长）
#   * 清理：rm -rf backend/data/storage/files/seed/ + 执行
#     seed-demo-data-cleanup.sql
# =====================================================================

set -eu

SEED_DIR="backend/data/storage/files/seed"
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

mkdir -p "$SEED_DIR"
cd "$SEED_DIR"

# 下载单个文件：$1=保存名 $2=URL（带下载进度显示）
dl() {
  if [ -s "$1" ]; then
    echo "跳过（已存在）：$1"
    return 0
  fi
  echo "下载：$1"
  curl -sL --retry 3 --max-time 600 -A "$UA" -o "$1" "$2" || {
    echo "下载失败：$1 <- $2" >&2
    rm -f "$1"
    return 1
  }
  [ -s "$1" ] || { echo "文件为空：$1" >&2; rm -f "$1"; return 1; }
}

# ===== 图片（7 张） =====
dl img-huaban-01.webp        "https://gd-hbimg.huaban.com/08aaeb96f1f7360a2016ab5da1d6dd2d8f9933b62f9137-uqfbvd_fw658webp"
dl img-699pic-6903-wh300.jpg "https://img95.699pic.com/photo/50166/6903.jpg_wh300.jpg"
dl img-699pic-9776-wh860.jpg "https://img95.699pic.com/photo/50464/9776.jpg_wh860.jpg"
dl img-699pic-8720-wh860.jpg "https://img95.699pic.com/photo/50059/8720.jpg_wh860.jpg"
dl img-699pic-4467-wh860.jpg "https://img95.699pic.com/photo/50465/4467.jpg_wh860.jpg"
dl img-shetu66-01.png        "https://img.shetu66.com/2023/07/04/1688453333865029.png"
dl img-699pic-9854-wh300.jpg "https://img95.699pic.com/photo/50064/9854.jpg_wh300.jpg"

# ===== 视频（3 段，较大，耐心等待） =====
dl video-cri-01.mp4 "https://v2.cri.cn/cb5a6d96-d0c4-4fd0-a895-b6135667d84a/video/6e5a600297f14ccda8993a411eced1ed.mp4"
dl video-cri-02.mp4 "https://v2.cri.cn/c89e53c6-0bc7-45ca-ac11-a385002d7d11/cb5a6d96-d0c4-4fd0-a895-b6135667d84a/video/080600d2-b03d-4b60-b2af-70f58be469e3.mp4"
dl video-cri-03.mp4 "https://v2.cri.cn/cb5a6d96-d0c4-4fd0-a895-b6135667d84a/video/4ed3009c4e07406f9c7bb7b2cfaed89e.mp4"

# ===== 音频（3 首，SoundHelix 测试音源，较慢） =====
dl audio-sh-01.mp3 "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
dl audio-sh-03.mp3 "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
dl audio-sh-08.mp3 "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"

echo ""
echo "全部下载完成。文件 SHA-256（应与 seed-demo-data.sql 中 storage_file 记录一致）："
sha256sum * 2>/dev/null || { for f in *; do printf "%s %s\n" "$(sha256sum "$f" 2>/dev/null || certutil -hashfile "$f" SHA256 | tail -1)" "$f"; done; }
