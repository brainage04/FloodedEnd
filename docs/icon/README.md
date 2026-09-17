# FloodedEnd icon

## What this is

`icon.png` is FloodedEnd's project icon: a 1024x1024 PNG (1,524,680 bytes,
RGBA), sha256 `0f797da0515c205c5ddf8394dc60ffa2904dc6eb0c4e5afdf956f3d8229e8396`.

It shows the delivered mod's most isolated moored End ship seen from astern over
open water: hull, waterline and mast against the End sky, with the island shelf
low at the right edge.

## How it was made

**A real in-game capture**, not a render and not generated pixel art.

| | |
| --- | --- |
| Capture | Minecraft **26.2** production (non-dev) client, offline-mode, username `IconCapture` |
| Loader | Fabric Loader **0.19.3**, Fabric API `0.156.0+26.2` |
| Mods | Iris `1.11.4`, Sodium `0.9.2`, the built mod `floodedend-1.0.0.jar` (sha256 `7614d7568e54398c5b455b014090899b7834da4afea8d9b9490cefda3ed23457`, loaded identically by client and server) |
| Shader pack | `ComplementaryUnbound_r5.8.1.zip` (sha256 `bb89b1fc54687d4147a837fb2e3c3f7261a13bee51819761e9b6a91cb7915965`), End lighting left at the pack's delivered defaults |
| Java / GL | OpenJDK 25.0.4.1, Mesa 26.1.8 **llvmpipe** software OpenGL (no GPU) |
| Display | private Xvfb `:213`, `1600x1600x24`; audio on an owned PipeWire null sink `round3_floodedend` |
| Client options | render distance 16, simulation distance 6, GUI scale 3, particles off, `fovEffectScale` 1.0 |
| Screenshot | native Minecraft F2 at **1600x1600**, F1 used to hide the HUD, no retouching |

**World.** The mod's own dedicated Fabric server, world `FloodProbe`
(`level-name=FloodProbe`, `level-seed=1234567890`, `level-type=normal`), dimension
`minecraft:the_end`. FloodedEnd's preset raises the End's sea level to 58, so the
water surface sits at y=57. This is the world the mod generates in
`fabric/run/server/FloodProbe`, copied byte-for-byte into the capture runtime as
`runtime/saves/Flooded End`.

**Subject.** The chosen moored End ship: origin `[1136, 54, 1328]`, 29x24x13,
keel y=54, deck y=61, mast top y=77; 89% open water within 64 blocks, nearest dry
land 34 blocks, nearest other structure 88 blocks, its End city 120 blocks away.

**Camera** (spectator player `IconCapture`, moved from the *server* console
because the offline-mode client cannot send commands):

```
/execute in minecraft:the_end run tp IconCapture 1114 61 1334 facing 1152 61 1334
```

x=1114, y=61, z=1334, yaw 270.0, pitch -0.0, **vertical FOV 70** (`options.txt`
`fov:0.5`, `fovEffectScale:1.0`), looking +x along the ship's length from abaft
the stern.

**Time and weather.** The End has no day cycle and no weather, so neither was set;
the delivered frame set varies camera height and sky angle instead of time of day.
The frame itself is the raw F2 PNG `runtime/screenshots/2026-09-17_15.04.24.png`.

**The icon is a crop.** The native frame is 1600x1600; the icon is that frame with
288 px removed from every edge (crop `(288, 288, 1312, 1312)`) and **no
resampling**. `provenance/crop-icon.py` performs exactly that crop and is verified
to reproduce `icon.png` byte for byte.

## Provenance files

| File | What it is |
| --- | --- |
| `source-frame/cine-03-stern-on.png` | the uncropped 1600x1600 native F2 frame the icon was cut from |
| `crop-icon.py` | the crop step, with the expected icon sha256 asserted; its output is `docs/icon/icon.png`, which is not duplicated under `provenance/` |
| `manifest.json` | the capture agent's per-frame record: for this frame, its camera, world, seed, method and the frame's own sha256 |
| `reproduce.json` | the capture agent's end-to-end reproduction steps for the cinematic set, including the world-generation console commands |
| `blockers.json` | constraints met and documented limitations from that capture pass |
| `cleanup-report.json` | which processes/display/audio sink were started and stopped, the isolation used, and the author-script sha256 list |
| `harness/launch.sh`, `harness/client.args` | how the client was actually launched (`--width 1600 --height 1600`, version 26.2, game dir, mods) |
| `harness/control.py`, `harness/hud.py` | the xdotool/chat control helpers used to drive the client and to check the HUD was hidden |
| `harness/shoot.sh`, `harness/shoot-set.sh` | press F2 and move the new native screenshot into `renders/`; re-shoot the whole set |
| `harness/verify-captures.py` | the check that every delivered frame is square, has no crosshair and no boss bar |
| `config/options.txt`, `config/iris.properties`, `config/sodium-options.json`, `config/sodium-mixins.properties` | the client's live settings when the frame was shot (FOV, render distance, shader pack selection, Sodium) |
| `config/ComplementaryUnbound_r5.8.1.zip.txt` | the shader pack's settings file as used, including its End lighting values |
| `evidence/chosen-ship.json` | why this ship was chosen (isolation measurements) |
| `evidence/client-run.txt` | client stdout proof: loader, llvmpipe renderer, shader pack, world join, screenshot lines, and the captured mod jar's sha256 |
| `evidence/screenshot-verification.json` | per-frame verification of the eight cinematic frames (`allSquare`, `allCrosshairsHidden`, `noBossBar` true) |
| `verification.json` | sha256 of `icon.png`, of the uncropped frame, of every copied file, and the crop check — all copies are byte-identical to their round3 sources |

## How to regenerate

Working directory: the mod repository (`FloodedEnd/`). The copied scripts contain
absolute paths into the round3 capture workspace
(`.../.local-icon-variants/round3/captures-floodedend/`), which no longer exists;
point them at whatever directory you use for `CAP`.

1. Build the mod (it must be the build whose jar hash is in
   `evidence/client-run.txt` if you want the identical world):

   ```
   JAVA_HOME=<openjdk-25.0.4.1> ./gradlew build
   ```

2. Generate the world `FloodProbe` with a dedicated server and the preset's
   forceload procedure, then stop it (full command list in
   `provenance/reproduce.json`, step 2):

   ```
   systemd-run --user --scope -p CPUQuota=200% -p CPUWeight=20 ./gradlew :fabric:runServer
   # console: floodedend status ; forceload add ... (each city centre, then its
   #          quadrants) ; forceload add -120 -120 120 120 ; save-all flush ; stop
   ```

3. Start the display and the owned audio sink:

   ```
   Xvfb :213 -screen 0 1600x1600x24 -nolisten tcp -ac +extension GLX &
   pactl load-module module-null-sink sink_name=round3_floodedend
   ```

4. Restart the server and start the production client (edit the absolute paths in
   `launch.sh`/`client.args` to your own capture directory first):

   ```
   systemd-run --user --scope -p CPUQuota=200% -p CPUWeight=20 ./gradlew :fabric:runServer
   DISPLAY=:213 PULSE_SINK=round3_floodedend sh <CAP>/launch.sh
   ```

5. Put the capture player in spectator mode and aim the camera from the **server
   console** (the client cannot run commands), leaving the client to send only
   Escape (End poem), F1 and F2:

   ```
   gamemode spectator IconCapture
   execute in minecraft:the_end run tp IconCapture 1114 61 1334 facing 1152 61 1334
   ```

6. Wait for chunks to render, press F1 then F2, and file the native screenshot as
   `cine-03-stern-on.png` (this is what `harness/shoot.sh cine-03-stern-on` does).

7. Verify the frame set, then cut the icon:

   ```
   python3 <CAP>/tools/verify-captures.py
   PYTHONPATH=/nix/store/4v9j9wbzyhrlx9980ygbr812313mazy0-python3.13-pillow-12.3.0/lib/python3.13/site-packages \
     python3 provenance/crop-icon.py provenance/source-frame/cine-03-stern-on.png icon.png
   ```

   The last command prints the icon's sha256 and fails if it is not
   `0f797da0515c205c5ddf8394dc60ffa2904dc6eb0c4e5afdf956f3d8229e8396`.

## Notes

- The camera was moved from the dedicated server console with
  `/execute in minecraft:the_end run tp ...`; the offline-mode client's chat is
  restricted, so it only ever sent Escape, F1 and F2. One earlier capture pass
  picked up a stale open chat window, which is why the whole set was reshot with
  the GUI verified hidden.
- `harness/client.args` also puts a scratch orthographic-projection mod
  (`/tmp/orthocam/orthocam-1.0.0.jar`) on the classpath. That mod only produced
  the two parallel-projection frames, is not part of this icon, and can simply be
  dropped from `-Dfabric.addMods` when reproducing this frame.
- The End has no day cycle: its "time of day" is fixed, and the shader pack's End
  lighting is at its default values. The set varies camera height and sky angle
  instead.
- `provenance/from-round3/manifest.json` (the gallery ledger) records `yaw=90.00` for this icon.
  That is a transposition in the ledger: the capture-time record in
  `provenance/manifest.json`, `provenance/reproduce.json` and the executed
  teleport all say **yaw 270.0**, which is the direction that faces the ship from
  x=1114 toward x=1152.
- `source-frame/cine-03-stern-on.png` is 5.2 MiB, above the 5 MB per-file
  guideline of this packaging pass. It is kept because the uncropped native frame
  is the only record of exactly what was captured before the crop.
- **Two inputs are too large to ship here and are described instead of copied:**
  the Minecraft 26.2 client runtime and the generated `FloodProbe` world (hundreds
  of MB of region files). Both are rebuilt rather than archived: the world by
  step 2 above with the mod's own generator, the runtime by a normal Fabric
  launcher install for 26.2.
- Deliberately **not** copied: the Minecraft runtime, the world saves, logs,
  candidates superseded by the owner's choice (`renders-v1-4x3/`, the parallel
  projection proof frames under `work/` and their scratch mod), contact sheets,
  video reference frames, the built mod jar (a build output, reproducible from the
  repository), and the world-measurement scripts (`region-readback.py`,
  `export-ship.py`) that chose the ship but are not needed to reproduce the camera
  and the frame.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
