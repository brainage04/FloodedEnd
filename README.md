# FloodedEnd

A server-side Fabric/NeoForge mod for Minecraft 26.2 that puts the ocean back into the End.

The End normally generates as scattered islands over an endless void. FloodedEnd restores the sea the dimension
is described as having had: `minecraft:end` now generates with **water** as its default fluid and a global **End
sea level of 58**, so every basin, hollow and gap between the islands floods and the main island and the outer
islands rise out of open water. End cities, End ships and chorus groves keep their vanilla generation, and the
ships are **moored at the waterline** so a city reads as standing on a coast with its ship sailing below it.

Nothing is required on the client: the mod ships world generation data and a command, and the client renders plain
vanilla water.

## How it works

The End's terrain is produced by the `minecraft:end` noise settings, whose `default_fluid` is normally air and
whose `sea_level` is 0. FloodedEnd overrides that file
(`common/src/main/resources/data/minecraft/worldgen/noise_settings/end.json`) with the vanilla End noise router and
exactly two changes:

| setting | vanilla | FloodedEnd |
| --- | --- | --- |
| `default_fluid` | `minecraft:air` | `minecraft:water` |
| `sea_level` | `0` | `58` |

Because the End's noise settings disable aquifers, the chunk generator uses the global fluid picker directly:
every y below the sea level that is not solid becomes the default fluid, and everything at or above it stays air.
No terrain is reshaped — the islands are the vanilla islands, now drowned up to y=57.

Sea level 58 was chosen from the measured terrain. The main island's ground around the exit portal sits between
y=59 and y=63, so a water surface at y=57 keeps the island, the exit portal and the obsidian pillars dry while the
island's slopes, every hollow and the whole surrounding void flood. The measurements, the per-ship numbers and the
fight observations are in [THE_END_UNDER_WATER.md](docs/THE_END_UNDER_WATER.md).

## End cities and their ships

An End city only generates where its five-by-five placement box finds ground at y=60 or higher, and vanilla asks
the **world surface** heightmap for that. Once the End has an ocean that heightmap answers with the water surface
instead, which would quietly delete every city whose placement box touches flooded ground. FloodedEnd generates
`minecraft:end_city` itself (`floodedend:end_city`) and asks the **ocean floor** heightmap, which is the same
answer vanilla gets on a dry End — the same seed therefore keeps the same cities.

The same structure moors the ships. Vanilla builds the End ship at the top of the city's tower, where it floats in
the sky 60 to 90 blocks above the water. FloodedEnd lowers it onto the sea instead: the hull is dropped until its
keel is three blocks under the water surface, at the nearest column whose seabed leaves the whole hull clear, so
the ship sits in the water rather than in a hillside. The sea level, the fluid and the heightmaps are all read back
out of the running chunk generator, so a world using a different preset moors its ships at that world's waterline,
and a dry End keeps vanilla's ship placement exactly.

## Commands

`/floodedend status` and `/floodedend probe [x z]` (permission level: gamemasters) read the running server back:
the first reports the End's live sea level and default fluid, the second reports the real contents of a vertical
column in the End — its surface block, how many water blocks it holds and the highest non-water block.
`/floodedend status` reports a failure when the ocean is not active, so a server that did not load the world
generation data says so instead of silently generating a dry End.

`/floodedend preset list|apply <id>|clear` moves the waterline. Minecraft decides the End's sea level from data
packs when a world is loaded, so a preset is a data pack the mod writes into the world:

| preset | sea level | what it produces |
| --- | --- | --- |
| `moored` | 58 | the shipped ocean: the main island, the exit portal and the pillars stay dry, ships are moored at the waterline |
| `drowned` | 72 | the waterline eight blocks above the island's highest ground: the island, the arrival platform and every city base are under water, the portal is rebuilt on the surface |

`apply` writes `datapacks/floodedend-preset` in the world and says so; the new waterline applies to chunks
generated after the next server start. `clear` removes it again. The preset carries the mod's own `minecraft:end`
noise settings with the sea level changed, so the shipped world generation data stays the single source of truth.

## What flooding does to the dragon fight

The short version, measured by running the fight in both presets:

- **`moored` is vanilla.** The dragon spawns and flies at y=67–84, the ten crystals heal it at the vanilla 2 health
  per second, the pillars still perch and the portal still activates. The only difference a player meets is
  arriving into nine blocks of water and swimming up.
- **`drowned` still works.** The pillars' tops stay above the water, so perching and the crystals are unchanged,
  and the fight rebuilds the exit portal on the water surface rather than under it, which keeps the End leaveable.
  What changes is the arena floor: 8 to 12 blocks of water over the island and 22 blocks over the arrival
  platform, which is more than one breath.

The full numbers, the arrival and departure tests and the reason submerged flora is not in this release are in
[docs/THE_END_UNDER_WATER.md](docs/THE_END_UNDER_WATER.md).

## Building

```shell
./gradlew build          # common + fabric + neoforge
./gradlew :fabric:runProductionServerGameTest
./gradlew :neoforge:runGameTestServer
```

The GameTests assert the contracts the mod ships: that `minecraft:end` resolves to a water default fluid above a
positive sea level, that `minecraft:end_city` generates through this mod and its ship template resolves, that the
readback command is registered, and that a preset installs a data pack the game reads back as an End ocean at the
requested sea level.

## Requirements

- Minecraft 26.2, Java 25
- Fabric Loader 0.19.3+ with Fabric API, or NeoForge 26.2.0.41-beta
- Server side only; installing it on the client changes nothing.

## Concepts

The design follows the concepts in
[*The End Used to Be Full of Water*](https://www.youtube.com/watch?v=uFRwAIN5jAo):

- **The End was an ocean.** Implemented: the End generates with a global sea again, and the waterline is a preset.
- **Ocean-like elements in the End.** End stone reads as sea bed and chorus plants as coral; both are vanilla and
  are left as they are, now surrounded by water. Submerged flora is deliberately not in this release — two thirds
  of the End's water is a 58-block column where floor plants would be invisible; see
  [docs/THE_END_UNDER_WATER.md](docs/THE_END_UNDER_WATER.md) for the numbers and the plan.
- **Shell lurkers.** Shulkers already live in End cities, which FloodedEnd leaves standing above the water, and
  End ships sail at the waterline below them.
- **"End water: super salty / super pure".** Not implemented: it would require a new fluid and block, and the
  video's point is the water itself, not a new one.
- **Endermen displacing blocks / the End draining.** Not implemented: it is lore narration, not world generation.
  The one mechanic the flooding suggests — Endermen and water — is scoped in the underwater notes.

## Licence

MIT.
