# FloodedEnd

A server-side Fabric/NeoForge mod for Minecraft 26.2 that puts the ocean back into the End.

The End normally generates as scattered islands over an endless void. FloodedEnd restores the sea the dimension
is described as having had: `minecraft:end` now generates with **water** as its default fluid and a global **End
sea level of 58**, so every basin, hollow and gap between the islands floods and the main island and the outer
islands rise out of open water.

Nothing is required on the client: the mod ships world generation data and a command, and the client renders plain
vanilla water.

## Features

### The End's ocean

No terrain is reshaped – the islands are the vanilla islands, now drowned up to y=57.
Sea level 58 was chosen from the measured terrain. The main island's ground around the exit portal sits between
y=59 and y=63, so a water surface at y=57 keeps the island, the exit portal and the obsidian pillars dry while the
island's slopes, every hollow and the whole surrounding void flood.

### End cities and their ships

FloodedEnd generates `minecraft:end_city` itself (`floodedend:end_city`) and asks the **ocean floor** heightmap
for the ground beneath the water. The city uses vanilla's templates; FloodedEnd tries to lower each ship onto the
sea, leaving it in the sky if no suitable mooring is found nearby.

The sea level, the fluid and the heightmaps are all read back out of the running chunk generator, so a world using
a different preset moors its ships at that world's waterline, and a dry End keeps vanilla's ship placement exactly.

### Sea-level presets

Minecraft decides the End's sea level from data packs when a world is loaded, so a preset is a data pack the mod
writes into the world:

- `moored`: sea level 58, the shipped ocean.
- `drowned`: sea level 72, above the main island and arrival platform.

`apply` writes `datapacks/floodedend-preset` in the world and says so; the new waterline applies to chunks
generated after the next server start. `clear` removes it again.

## Getting started

- Minecraft 26.2, Java 25
- Fabric Loader 0.19.3+ with Fabric API, or NeoForge 26.2.0.88+
- Server side only; installing it on the client changes nothing.

## Commands

All commands require gamemaster permission (level 2).

- `/floodedend status` - reports the End's live sea level and default fluid, and fails if the ocean is not active.
- `/floodedend probe [x z]` - reads a loaded End column: its surface block, water depth and highest non-water block.
- `/floodedend preset list` - lists the presets and the live ocean settings.
- `/floodedend preset apply <moored|drowned>` - installs a sea-level preset for the next server start.
- `/floodedend preset clear` - removes the installed preset for the next server start.

## Configuration

Use the preset commands above. No configuration file or gamerules.

## Compatibility

Other data packs that replace `minecraft:end` noise settings or `minecraft:end_city` can override FloodedEnd's generation.

## Documentation

- [Test review](docs/TEST_REVIEW.md) - test classifications and coverage.
- [Underwater End notes](docs/THE_END_UNDER_WATER.md) - terrain measurements, ship placement and dragon fight observations.
- [Development](docs/DEVELOPMENT.md) - building and GameTests.
- [Release automation](docs/RELEASE.md)
- [Modrinth publishing](docs/MODRINTH.md)

The design follows the concepts in [*The End Used to Be Full of Water*](https://www.youtube.com/watch?v=uFRwAIN5jAo).

MIT.
