"""Drive the owned :213 Minecraft client: chat, keys, screenshots.

Verifies the client is routed to the owned null sink before touching the window, so
no input ever reaches the primary desktop or its audio.
"""
from __future__ import annotations

import os
import subprocess
import sys
import time
from pathlib import Path

RUNTIME = Path(__file__).resolve().parents[1]
DISPLAY = ":213"
SINK = "round3_floodedend"
ENV = dict(os.environ, DISPLAY=DISPLAY)


def verify_audio() -> str:
    sinks = subprocess.check_output(["pactl", "list", "short", "sinks"], text=True)
    sink_id = next(line.split()[0] for line in sinks.splitlines() if f"\t{SINK}\t" in line)
    streams = subprocess.check_output(["pactl", "list", "short", "sink-inputs"], text=True)
    assert any(line.split()[1] == sink_id for line in streams.splitlines()), \
        "client is not routed to the owned null sink"
    (RUNTIME / "evidence").mkdir(exist_ok=True)
    (RUNTIME / "evidence" / "audio-routing.txt").write_text(sinks + "\nSINK INPUTS\n" + streams)
    return sink_id


def window() -> str:
    windows = subprocess.check_output(["xdotool", "search", "--name", "Minecraft"], env=ENV, text=True).split()
    assert windows, "no Minecraft window on the owned display"
    return windows[-1]


def xdotool(*args: str) -> None:
    subprocess.run(["xdotool", *args], env=ENV, check=True)


def key(name: str, hold: float = 0.2) -> None:
    xdotool("keydown", name)
    time.sleep(hold)
    xdotool("keyup", name)
    time.sleep(0.4)


def main() -> int:
    verify_audio()
    xdotool("windowfocus", window())
    mode = sys.argv[1]
    if mode == "chat":
        key("t")
        time.sleep(0.5)
        xdotool("key", "ctrl+a")
        xdotool("type", "--clearmodifiers", "--delay", "10", sys.argv[2])
        key("Return")
        with (RUNTIME / "evidence" / "client-commands.txt").open("a") as out:
            out.write(time.strftime("%Y-%m-%dT%H:%M:%S") + " " + sys.argv[2] + "\n")
    elif mode == "key":
        key(sys.argv[2])
    elif mode == "shot":
        from PIL import ImageGrab
        ImageGrab.grab(xdisplay=DISPLAY).save(sys.argv[2])
    elif mode == "wait":
        time.sleep(float(sys.argv[2]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
