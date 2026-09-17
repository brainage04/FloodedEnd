#!/bin/sh
# Re-shoot the whole End set from the client: teleport, let the chunks render, capture.
set -eu
CAP=/home/thomas/01_TM/Coding/Websites/brainage04.github.io/.local-icon-variants/round3/captures-floodedend
PY="PYTHONPATH=/nix/store/4v9j9wbzyhrlx9980ygbr812313mazy0-python3.13-pillow-12.3.0/lib/python3.13/site-packages python3"

shoot() {
  name=$1
  x=$2 y=$3 z=$4 tx=$5 ty=$6 tz=$7
  env PYTHONPATH=/nix/store/4v9j9wbzyhrlx9980ygbr812313mazy0-python3.13-pillow-12.3.0/lib/python3.13/site-packages \
    python3 "$CAP/tools/control.py" chat "/execute in minecraft:the_end run tp @s $x $y $z facing $tx $ty $tz" >/dev/null
  sleep 9
  "$CAP/tools/shoot.sh" "$name"
}

shoot main-island-01-wide     0    80   140   0    64   0
shoot main-island-02-shelf    118  74   -118  0    66   0
shoot main-island-03-pillars  0    65.5 62    0    70   -90
shoot main-island-04-rim      0    71   96    0    68   -20
shoot outer-island-01-city    -896 110  -790  -896 85   -864
shoot outer-island-02-chorus  -770 74   -770  -896 84   -864
shoot outer-island-03-aerial  -800 148  -790  -896 58   -864
shoot outer-island-04-water   -790 62   -940  -880 82   -880
