# Development

FloodedEnd is server-side only. Client GameTests are disabled because the mod has no client entrypoint, renderer or client fixtures. CI builds both loader jars and runs the production server GameTests on Fabric and NeoForge.

## Building

```shell
./gradlew --no-daemon -Pfabricmoddingconventions.devClientOptions=/nonexistent build
./gradlew --no-daemon -Pfabricmoddingconventions.devClientOptions=/nonexistent runAllGameTests
```

Run these through `heavy` in the Minecraft workspace. `runAllGameTests` runs the production server suites for Fabric and NeoForge; the individual tasks are `:fabric:runProductionServerGameTest` and `:neoforge:runProductionServerGameTest`.

