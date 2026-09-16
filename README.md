# FloodedEnd

A server-side Fabric/NeoForge mod for Minecraft 26.2 that puts the ocean back into the End.

The End normally generates as scattered islands over an endless void. FloodedEnd restores the sea the dimension
is described as having had: `minecraft:end` now generates with **water** as its default fluid and a global **End
sea level of 58**, so every basin, hollow and gap between the islands floods and the main island and the outer
islands rise out of open water. Vanilla structures are untouched and simply end up standing in the sea — the ten
obsidian pillars, the exit portal, End cities, End ships and chorus groves all keep their vanilla generation.

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

Sea level 58 was chosen from the measured terrain: the main island's surface sits between y=48 and y=63, so the
island's plateau stays dry while its slopes, hollows and the whole surrounding void flood, and the outer islands
become archipelagos with End cities standing in the water.

## Commands

`/floodedend status` and `/floodedend probe [x z]` (permission level: gamemasters) read the running server back:
the first reports the End's live sea level and default fluid, the second reports the real contents of a vertical
column in the End — its surface block, how many water blocks it holds and the highest non-water block.
`/floodedend status` reports a failure when the ocean is not active, so a server that did not load the world
generation data says so instead of silently generating a dry End.

## Building

```shell
./gradlew build          # common + fabric + neoforge
./gradlew :fabric:runGameTestServer
./gradlew :neoforge:runGameTestServer
```

The GameTests assert the two contracts the mod ships: that `minecraft:end` resolves to a water default fluid above
a positive sea level, and that the readback command is registered.

## Requirements

- Minecraft 26.2, Java 25
- Fabric Loader 0.19.3+ with Fabric API, or NeoForge 26.2.0.41-beta
- Server side only; installing it on the client changes nothing.

## Concepts

The design follows the concepts in
[*The End Used to Be Full of Water*](https://www.youtube.com/watch?v=uFRwAIN5jAo):

- **The End was an ocean.** Implemented: the End generates with a global sea again.
- **Ocean-like elements in the End.** End stone reads as sea bed and chorus plants as coral; both are vanilla and
  are left as they are, now surrounded by water.
- **Shell lurkers.** Shulkers already live in End cities, which FloodedEnd leaves standing above the water.
- **"End water: super salty / super pure".** Not implemented: it would require a new fluid and block, and the
  video's point is the water itself, not a new one.
- **Endermen displacing blocks / the End draining.** Not implemented: it is lore narration, not world generation.

## Licence

MIT.
