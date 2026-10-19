# Test review

## Frozen rubric

- Keep: exercises production behavior using Minecraft or real files, with an observable outcome rather than just registration.
- Redundant: another named test covers the same production path and outcome. The Fabric and NeoForge adapters are not redundant: both loaders must load the mod independently.
- Tautological: checks only wiring, a constant, a copied implementation or a mock echo. A vanilla resource existing is not evidence that FloodedEnd uses it correctly.

## Original inventory

Each name below existed once on Fabric and once on NeoForge. There were no unit tests or client GameTests.

- `endGeneratesItsOcean`: keep. Reads the game's decoded End noise settings and checks its fluid and waterline.
- `presetInstallsAnOcean`: keep. Installs real files, decodes the resulting noise settings with Minecraft's codec and removes the pack.
- `floodReadbackCommandIsRegistered`: tautological. Only checks that the root command exists, not permissions, results or feedback.
- `endCityIsFloodedEnds`: tautological. Only checks registry/class wiring, not placement or mooring.
- `endShipTemplateResolves`: tautological. Only checks that Minecraft's vanilla ship template has a positive size, not that a generated ship is moored.

The three flagged tests were removed on both loaders, including their shared assertions and NeoForge test registrations/resources. No test was classified as redundant.
