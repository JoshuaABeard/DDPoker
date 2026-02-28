#!/usr/bin/env bash
# Convert desktop audio files to web-optimized MP3 + OGG
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/../../poker/src/main/resources/config/poker/audio"
DST="$SCRIPT_DIR/../public/audio"
mkdir -p "$DST"

# Skip music files (music2a, music2b, music3a, music3b)
SKIP="music2a|music2b|music3a|music3b"

for f in "$SRC"/*.wav "$SRC"/*.aif; do
  [ -f "$f" ] || continue
  base=$(basename "${f%.*}")
  echo "$base" | grep -qE "^($SKIP)$" && continue

  echo "Converting $base..."
  ffmpeg -y -i "$f" -codec:a libmp3lame -b:a 128k "$DST/$base.mp3" 2>/dev/null
  ffmpeg -y -i "$f" -codec:a libvorbis -qscale:a 3 "$DST/$base.ogg" 2>/dev/null
done

echo "Done. $(ls "$DST"/*.mp3 2>/dev/null | wc -l) MP3 + $(ls "$DST"/*.ogg 2>/dev/null | wc -l) OGG files created."
