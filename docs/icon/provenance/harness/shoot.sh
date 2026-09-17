#!/bin/sh
# Press F2 in the owned client and move the new native screenshot where it belongs.
# usage: shoot.sh <name> [destination directory under the captures folder, default renders]
set -eu
CAP=/home/thomas/01_TM/Coding/Websites/brainage04.github.io/.local-icon-variants/round3/captures-floodedend
export DISPLAY=:213
name=$1
dest=${2:-renders}
mkdir -p "$CAP/$dest"
before=$(ls -t "$CAP"/runtime/screenshots/*.png 2>/dev/null | head -1 || true)
xdotool key F2
i=0
while [ "$i" -lt 30 ]; do
  sleep 1
  newest=$(ls -t "$CAP"/runtime/screenshots/*.png 2>/dev/null | head -1 || true)
  if [ -n "$newest" ] && [ "$newest" != "$before" ]; then
    mv "$newest" "$CAP/$dest/$name.png"
    echo "$name <- $(basename "$newest")"
    exit 0
  fi
  i=$((i + 1))
done
echo "no screenshot appeared for $name" >&2
exit 1
