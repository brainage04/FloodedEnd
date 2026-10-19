# Test review

## Frozen rubric

- Keep: exercises production behavior using Minecraft or real files, with an observable outcome rather than just registration.
- Redundant: another named test covers the same production path and outcome. The Fabric and NeoForge adapters are not redundant: both loaders must load the mod independently.
- Tautological: checks only wiring, a constant, a copied implementation or a mock echo. A vanilla resource existing is not evidence that FloodedEnd uses it correctly.

## Original inventory

Each name below existed once on Fabric and once on NeoForge. There were no unit tests or client GameTests.

- `endGeneratesItsOcean`: initially retained as data-pack coverage, then flagged as tautological by the frozen-rubric judge because it only reads settings values. Removed after `generatedOceanAndProbe` covered the actual generated water, its floor and its surface.
- `presetInstallsAnOcean`: keep. Installs real files, decodes the resulting noise settings with Minecraft's codec and removes the pack.
- `floodReadbackCommandIsRegistered`: tautological. Only checks that the root command exists, not permissions, results or feedback.
- `endCityIsFloodedEnds`: tautological. Only checks registry/class wiring, not placement or mooring.
- `endShipTemplateResolves`: tautological. Only checks that Minecraft's vanilla ship template has a positive size, not that a generated ship is moored.

The first three flagged tests were removed on both loaders, including their shared assertions and NeoForge test registrations/resources. The later frozen-rubric judge agreed on those three, retained `presetInstallsAnOcean` and flagged `endGeneratesItsOcean`; that fourth test was removed once actual generated-block coverage existed. No original pair of tests was classified as redundant.

## Coverage additions

Both loader suites now exercise permission rejection, live status, preset listing, unknown preset feedback, loaded and unloaded probes, actual ocean blocks in the central void, dry/capped columns with air gaps, both preset files through command dispatch, all ship rotations at both waterlines, reference-radius fallback, generated city buildings and dry-world parity against vanilla across sixteen deterministic structure seeds. File changes and column changes are restored in `finally` blocks.

Remaining gaps: a full world restart with a preset selected, the dragon fight and portal lifecycle, and visual ship/hull placement across a broad range of world seeds. These need more than an isolated GameTest; the historical manual observations are in `THE_END_UNDER_WATER.md`. There are no mod gamerules, config screen, renderer, background threads or shutdown resources.

## Code review fixes

- Preset metadata declares its minimum and maximum supported pack format; the preset scenario reads it with Minecraft's server metadata decoder.
- Clearing an absent preset returns failure instead of claiming to remove a pack; the preset scenario checks the result and feedback.
- Preset listing describes the files installed on disk separately from the live generator; applying a pack does not change the running waterline before restart.
- Mooring checks every column under the bounding box instead of twenty-five snapped samples. The ship scenario checks every resulting hull column at the main island's coast in all rotations, as well as the open-water cases.

Judgement calls left unchanged: mooring candidates use an eight-block search grid rather than a Euclidean nearest-point search, and command feedback is English literals rather than translated components. The mod is server-only, so a new translation key would not resolve on an unmodified client.

The old city-placement explanation was backwards: water can raise the world-surface height and admit a city, not lower it and reject one. The terrain-based ocean-floor implementation remains unchanged; its explanation is corrected.
