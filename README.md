# FloodedEnd

A server-side Fabric/NeoForge mod for Minecraft 26.3 that puts the ocean back into the End.

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
y=59 and y=63 (seed 1234567890, measured again on Minecraft 26.3 by the GameTest `main_island_stays_above_the_water`,
which also checks that no ground within 40 blocks of the portal is under water), so a water surface at y=57 keeps the
island, the exit portal and the obsidian pillars dry while the island's slopes, every hollow and the whole surrounding
void flood.

### End cities and their ships

FloodedEnd generates `minecraft:end_city` itself (`floodedend:end_city`) and asks the **ocean floor** heightmap
for the ground beneath the water. The city uses vanilla's templates; FloodedEnd tries to lower each ship onto the
sea, leaving it in the sky if no suitable mooring is found nearby.

The sea level, the fluid and the heightmaps are all read back out of the running chunk generator, so a world using
a different preset moors its ships at that world's waterline, and a dry End keeps vanilla's ship placement exactly.

Only a dimension that generates from the `minecraft:end` noise settings gets these changes: the End, and data pack
dimensions that reuse those settings. End cities in any other dimension, such as a Single Biome world set to an End
biome, generate as in vanilla.

### Sea-level presets

Minecraft decides the End's sea level from data packs when a world is loaded, so a preset is a data pack the mod
writes into the world:

- `moored`: sea level 58, the shipped ocean.
- `drowned`: sea level 72, above the main island and arrival platform.

`apply` writes `datapacks/floodedend-preset` in the world and says so; the new waterline applies to chunks
generated after the next server start. `clear` removes it again.

The preset holds the End's noise settings in the format of the Minecraft version that wrote it, and a world whose
data packs do not decode refuses to load. When a world is opened, FloodedEnd therefore rewrites a preset written for
an older Minecraft version (for example one applied on 26.2) for the running version and keeps its sea level; the log
says `FloodedEnd: rewrote the drowned preset (End sea level 72) ...`. Without FloodedEnd installed, such a world only
loads after `datapacks/floodedend-preset` is deleted.

## Getting started

- Minecraft 26.3, Java 25
- Fabric Loader 0.19.5+ with Fabric API, or NeoForge 26.3.0.99+
- Not needed on the client. Installed in a client, it floods the End chunks that every singleplayer world opened with
  it generates, and those chunks keep their water after the mod is removed.

## Existing worlds

FloodedEnd changes world generation, so it only affects End chunks generated while it is installed:

- End chunks generated before it was installed stay dry. Where a new, flooded chunk meets an old one, the water
  stands as a wall at the chunk border, because the End generates without aquifers and its water gets no fluid
  updates. The wall flows into the dry chunk when a block next to it is updated.
- End cities generated before it was installed keep their ships in the sky.
- Removing the mod leaves the water in every chunk generated while it was installed. Chunks generated afterwards
  are dry again.

For a fully flooded End, install it in a new world or in a world whose End has not been explored.

## Commands

All commands require gamemaster permission (level 2).

- `/floodedend status [dimension]` – reports a dimension's live sea level and default fluid (the End by default), and
  fails if it has no active ocean.
- `/floodedend probe [x z [dimension]]` – reads a loaded column: its surface block, water depth and highest non-water
  block. Without coordinates it reads where you stand. With coordinates it reads the End unless a dimension is given.
- `/floodedend preset list` – lists the presets and the live ocean settings, and says whether the preset data pack is
  installed, and whether `/datapack disable` turned it off.
- `/floodedend preset apply <moored|drowned>` – installs a sea-level preset for the next server start.
- `/floodedend preset clear` – removes the installed preset for the next server start.

## Configuration

Use the preset commands above. No configuration file or gamerules.

## Compatibility

Other data packs that replace `minecraft:end` noise settings or `minecraft:end_city` can override FloodedEnd's generation.

FloodedEnd gives `minecraft:end_city` its own structure type, `floodedend:end_city`, so mods that change End cities
through a mixin into vanilla's `EndCityStructure` no longer affect them. When two mods or data packs replace the same
file, the one loaded last wins.

## Known issues

- A small End island that vanilla generates next to a moored ship, between y=55 and y=70, can replace part of the
  ship's hull with end stone.
- One world generated on Minecraft 26.2 has a lone bow of an End ship standing in the sea at (-1728, 54, 45). Its
  cause is unknown.

Both are described in the [Underwater End notes](docs/THE_END_UNDER_WATER.md#known-ship-defects).

## Documentation

- [Underwater End notes](docs/THE_END_UNDER_WATER.md) - terrain measurements, ship placement and dragon fight observations.
- [Development](docs/DEVELOPMENT.md) - building and GameTests.
- [Release automation](docs/RELEASE.md)
- [Modrinth publishing](docs/MODRINTH.md)

The design follows the concepts in [*The End Used to Be Full of Water*](https://www.youtube.com/watch?v=uFRwAIN5jAo).

MIT.
