#!/usr/bin/env python3
"""Make sure the client HUD is hidden before a capture.

Presses F1 (refocusing the owned window first) and verifies that the Ender Dragon
boss bar strip is gone, retrying until the HUD really is off.
"""
from __future__ import annotations

import subprocess
import sys
import time
from pathlib import Path

from PIL import Image, ImageGrab

RUNTIME = Path(__file__).resolve().parents[1]
DISPLAY = ":213"
ENV = {"DISPLAY": DISPLAY, "PATH": "/run/current-system/sw/bin:/usr/bin:/bin"}


def window() -> str:
    out = subprocess.check_output(["xdotool", "search", "--name", "Minecraft"], env=ENV, text=True).split()
    return out[-1]


def hud_visible(path: Path) -> bool:
    image = ImageGrab.grab(xdisplay=DISPLAY).convert("RGB")
    image.save(path)
    strip = image.crop((520, 0, 1100, 60))
    # the Ender Dragon boss bar renders as a saturated magenta strip with no green.
    return any(50 <= colour[0] <= 140 and colour[1] < 20 and 30 <= colour[2] <= 120
               for _, colour in strip.getcolors(maxcolors=1 << 20))


def main() -> int:
    want_hidden = (len(sys.argv) < 2 or sys.argv[1] == "hide")
    probe = RUNTIME / "work" / "hud-probe.png"
    for attempt in range(4):
        visible = hud_visible(probe)
        if visible != want_hidden:
            print(f"HUD hidden={not visible} (attempt {attempt})")
            return 0
        subprocess.run(["xdotool", "mousemove", "--sync", "800", "600"], env=ENV, check=True)
        subprocess.run(["xdotool", "windowfocus", "--sync", window()], env=ENV, check=True)
        time.sleep(0.4)
        subprocess.run(["xdotool", "key", "F1"], env=ENV, check=True)
        time.sleep(2.5)
    print("could not reach the wanted HUD state", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
