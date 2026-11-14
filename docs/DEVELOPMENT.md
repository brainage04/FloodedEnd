# Development

FloodedEnd is server-side only. Client GameTests are disabled because the mod has no client entrypoint, renderer or client fixtures. CI builds both loader jars and runs the production server GameTests on Fabric and NeoForge.

## Building

```shell
./gradlew --no-daemon build
./gradlew --no-daemon runAllGameTests
```

Run these through `heavy` in the Minecraft workspace. `runAllGameTests` runs the production server suites for Fabric and NeoForge; the individual tasks are `:fabric:runProductionServerGameTest` and `:neoforge:runProductionServerGameTest`.

## Unit tests

```shell
./gradlew --no-daemon :fabric:test
./gradlew --no-daemon :fabric:pitest
```

The unit tests check the placement maths with jqwik over made-up terrain, without a server. `EndShipMooringTest` checks that a moored ship stands only in chunks that reference its city's start, and that the search moors it at the nearest candidate where it floats, or leaves it where it is. `EndCityAtSeaTest` checks that over a dry End a city starts exactly where vanilla's `EndCityStructure` starts one, and that over a flooded End it measures the ocean floor at vanilla's corners. `EndIslandStructuresTest` checks that vanilla's small island writes only into the chunks around its own, that the FEATURES step serves every chunk the structure scan reads, and which blocks the island keeps out of.

`:fabric:pitest` runs mutation tests on `EndShipMooring`, `EndCityAtSea` and `EndIslandStructures` against those tests. It is not part of `check`; the report is written to `fabric/build/reports/pitest`.

