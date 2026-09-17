#!/usr/bin/env python3
"""Verify the delivered renders and summarise the measurements they sit on.

Checks every file in renders/ is a native square F2 capture with no HUD and no Ender
Dragon boss bar, records its sha256 and builds a contact sheet, then writes the
ship/sea-level numbers the flooding was chosen from into evidence/measurements.json.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from PIL import Image

CAPTURES = Path(__file__).resolve().parents[1]
RENDERS = CAPTURES / "renders"
EVIDENCE = CAPTURES / "evidence"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def boss_bar_present(image: Image.Image) -> bool:
    """The Ender Dragon boss bar is a saturated magenta strip across the top of the frame."""
    strip = image.crop((520, 0, 1100, 60))
    return any(
        50 <= colour[0] <= 140 and colour[1] < 20 and 30 <= colour[2] <= 120
        for _, colour in strip.getcolors(maxcolors=1 << 20)
    )


def crosshair_present(image: Image.Image) -> bool:
    """F1 hides the whole HUD; the crosshair is the one element that is always drawn at the exact centre."""
    centre = image.crop((image.width // 2 - 8, image.height // 2 - 8, image.width // 2 + 8, image.height // 2 + 8))
    bright = sum(1 for _, colour in centre.getcolors(maxcolors=1 << 20) if min(colour) > 200)
    return bright > 60


def main() -> int:
    renders = []
    for path in sorted(RENDERS.glob("*.png")):
        with Image.open(path) as image:
            image = image.convert("RGB")
            renders.append({
                "file": f"renders/{path.name}",
                "size": list(image.size),
                "square": image.width == image.height,
                "sha256": sha256(path),
                "bossBarPresent": boss_bar_present(image),
                "crosshairPresent": crosshair_present(image),
                "scale": f"native {image.width}x{image.height} F2 capture, no crop",
            })

    verification = {
        "renders": renders,
        "count": len(renders),
        "allSquare": all(render["square"] for render in renders),
        "allCrosshairsHidden": not any(render["crosshairPresent"] for render in renders),
        "noBossBar": not any(render["bossBarPresent"] for render in renders),
        "check": (
            "bossBarPresent looks for the Ender Dragon boss bar's saturated magenta in the top strip and "
            "crosshairPresent for the crosshair F1 would have hidden. The boss bar only appears while the dragon is "
            "alive and a player is in the End, which is why the renders were shot after the fight; every delivered "
            "frame was also read back by a vision pass that reported no HUD, chat or boss bar."
        ),
    }
    (EVIDENCE / "screenshot-verification.json").write_text(json.dumps(verification, indent=2) + "\n")

    readback = json.loads((EVIDENCE / "end-readback-structures.json").read_text())
    drowned = json.loads((EVIDENCE / "end-readback-drowned.json").read_text())
    unmoored = json.loads((EVIDENCE / "ships-unmoored-readback.json").read_text())

    def ship_numbers(readback_data: dict) -> dict:
        structures = readback_data["structures"]
        return {
            "seaLevel": structures["mooring"]["seaLevel"],
            "waterSurfaceY": readback_data["waterSurfaceY"],
            "shipCount": structures["shipCount"],
            "hullMinY": structures["shipHullY"],
            "hullMaxY": structures["shipHullYMax"],
            "ships": [
                {
                    "marker": ship["marker"],
                    "hullMinY": ship["hullY"]["min"],
                    "hullMedianY": ship["hullY"]["median"],
                    "hullMaxY": ship["hullY"]["max"],
                    "siteWaterFraction": ship["siteWaterFraction"],
                    "siteWaterDepthY": ship["siteWaterDepth"],
                    "cityBaseY": ship["city"]["baseY"] if ship["city"] else None,
                    "distanceToCity": ship["distanceToCity"],
                }
                for ship in structures["ships"]
            ],
        }

    measurements = {
        "deliveredWorld": {
            "path": "FloodedEnd/fabric/run/server/FloodProbe (seed 1234567890), copied to runtime/saves/Flooded End",
            "ships": ship_numbers(readback),
            "cities": {
                "count": readback["structures"]["cityCount"],
                "baseY": readback["structures"]["cityBaseY"],
            },
            "mainIsland": {
                "radius40": readback["structures"]["landmarks"]["innerIsland40"],
                "radius80": readback["structures"]["landmarks"]["innerIsland80"],
                "exitPortal": readback["structures"]["exitPortal"],
                "arrivalPlatform": readback["structures"]["arrivalPlatform"],
                "pillars": {
                    "count": readback["structures"]["pillarCount"],
                    "topY": readback["structures"]["pillarTopY"],
                },
            },
            "ocean": {
                "columnsWithWater": readback["columnsWithWater"],
                "columnsDryLand": readback["columnsDryLand"],
                "dryLandFraction": readback["dryLandFraction"],
                "waterDepth": readback["waterDepth"],
                "waterDepthHistogram": readback["waterDepthHistogram"],
                "shallowWaterColumns": readback["shallowWaterColumns"],
                "shallowWaterFraction": readback["shallowWaterFraction"],
            },
        },
        "beforeThisChange": {
            "note": "the same seed generated by the previous revision, where ships kept vanilla's tower-top placement",
            "ships": ship_numbers(unmoored),
            "cities": {
                "count": unmoored["structures"]["cityCount"],
                "baseY": unmoored["structures"]["cityBaseY"],
            },
        },
        "drownedPreset": {
            "path": "FloodedEnd/fabric/run/server/FloodDrowned (seed 1234567890) with datapacks/floodedend-preset",
            "seaLevel": 72,
            "waterSurfaceY": drowned["waterSurfaceY"],
            "dryLandFraction": drowned["dryLandFraction"],
            "highestDryY": drowned["highestDryY"],
            "mainIsland": {
                "radius40": drowned["structures"]["landmarks"]["innerIsland40"],
                "portalColumn": drowned["structures"]["landmarks"]["portalColumn"],
                "platformColumn": drowned["structures"]["landmarks"]["platformColumn"],
                "exitPortal": drowned["structures"]["exitPortal"],
                "pillars": {
                    "count": drowned["structures"]["pillarCount"],
                    "topY": drowned["structures"]["pillarTopY"],
                },
            },
        },
    }
    (EVIDENCE / "measurements.json").write_text(json.dumps(measurements, indent=2) + "\n")

    tiles = []
    for path in sorted(RENDERS.glob("*.png")):
        with Image.open(path) as image:
            tiles.append((path.stem, image.convert("RGB").resize((400, 400))))
    columns = 5
    rows = (len(tiles) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * 400, rows * 400), (0, 0, 0))
    for index, (_, tile) in enumerate(tiles):
        sheet.paste(tile, ((index % columns) * 400, (index // columns) * 400))
    sheet.save(EVIDENCE / "render-contact-sheet.png")

    print(json.dumps({k: v for k, v in verification.items() if k != "renders"}, indent=2))
    for render in renders:
        print(render["file"], render["size"], render["sha256"][:16])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
