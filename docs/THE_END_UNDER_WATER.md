# The End under water

What FloodedEnd's ocean actually does, measured from generated worlds rather than assumed. Everything below was
read back out of saved region files (`captures-floodedend/tools/region-readback.py` in the capture workspace) or out
of a running dedicated server with a production client connected.

## Where the waterline sits

The End's terrain is unchanged, so the sea level only has to be chosen well. The numbers that chose it, from the
delivered world (seed 1234567890, sea level 58):

| landmark | y |
| --- | --- |
| water surface | 57 |
| main island's ground around the exit portal (within 40 blocks) | min 59, median 62, max 63 |
| exit portal | a 5x5 ring of `minecraft:end_portal` at y=63, the dragon egg above it at y=67 |
| obsidian pillars | bases at y=32, tops from y=77 to y=104 |
| arrival platform | 5x5 of obsidian at y=48 |
| End cities | bases from y=60 to y=63 |
| End ships, before this change | hull from y=130 to y=153, decks around y=136 |

So a water surface at y=57 sits on the island's rim: the island stays dry by two to six blocks, the exit portal and
the pillars stay dry, and the arrival platform is nine blocks under water. The whole surrounding void — 66% of the
water columns in the delivered world are the full 58-block column — floods.

## What happened to the ships

Before this change the End ship kept vanilla's placement, at the top of the city's tower: hulls sat at y=130–153,
73 to 96 blocks above the water surface. Two ships in the sampled area, one city, city base y=63.

After the change, every End city's ship is moored at the waterline. Eleven ships across twelve cities:

| | before | after |
| --- | --- | --- |
| ships measured | 2 | 11 |
| hull bottom (min y) | 130 and 130 | 54 for all eleven |
| hull top (max y) | 153 | 77 |
| city base y | 63 | 60 to 63 |
| distance from the city | 66 and 91 | 50 to 180 |

Every hull footprint column is water (`siteWaterFraction` 1.0) and the seabed under each hull stays 7 or more blocks
below the keel, so no ship is buried in an island and none is left in the sky. The keel sits three blocks under the
water surface, which puts the hull's lower four layers in the water and its deck one block above it.

The cities themselves are unchanged: the same seed produces the same cities at the same bases, because
`floodedend:end_city` asks the ocean floor heightmap for placement. Vanilla asks the world surface heightmap, which
under an ocean answers with the water surface and would reject any city whose five-by-five placement box touches
flooded ground.

## The dragon fight

Run end to end in both presets, on a dedicated server with a production client. Arrival was a real portal: an End
portal block was placed in the overworld and the player walked into it, which runs the same code path as a
stronghold portal (`EndPortalBlock` builds the arrival platform and moves the player to the End's spawn point).

### `moored` (the shipped ocean, sea level 58)

| step | what was observed |
| --- | --- |
| arrival | player landed at (100.5, 49.0, 0.5), nine blocks under the water surface. The column at (100, 0) reads identically before and after the arrival — water 0–57, obsidian at 48 — because the platform rebuild only runs when the block under the spawn point is not already obsidian, and it is |
| dragon | `Found that the dragon has not yet been killed in this world.` Spawned at 200 health and flew at y=67 to y=84, entirely above the water |
| crystals | ten, one per pillar, at y=80 |
| crystal healing | health written to 60, read back 95 after 17 seconds: 2.0 health per second, exactly the vanilla rate. After killing all ten crystals, 60 stayed 60 for 33 seconds |
| pillars | unchanged; bases at y=32, tops y=77–104 |
| portal activation | the dragon was killed, the egg appeared at (0, 67, 0) and the portal blocks at y=63 |
| leaving | the player stood in a portal block at (1.5, 63.5, 0.5); the client showed the End poem and the player returned to the overworld |

The fight is vanilla. The only flooded difference a player meets is arriving into nine blocks of water and swimming
up, which is comfortably inside one breath.

### `drowned` (sea level 72, installed with `/floodedend preset apply drowned`)

| step | what was observed |
| --- | --- |
| arrival | player landed at (100.5, 49.0, 0.5), 22 blocks under the water surface. In survival that is more than one breath of swimming; the surface is about 20 seconds away at vanilla swim speed |
| dragon | spawned at 200 health and flew at y=93, above the water |
| crystals | ten, at y=86 — 15 blocks above the water, because the pillars are taller than the drowned sea |
| crystal healing | 60 health read back as 132 after 36 seconds: again 2.0 health per second |
| pillars | tops y=77–104, so they still stand out of the water and still perch |
| island and portal | the island's ground (y=59–63) is 8 to 12 blocks under water. The fight rebuilds the exit portal by scanning down from the sky, and with an ocean the first thing it meets is the water surface: the portal was built at y=71 on top of the sea with the dragon egg above it at y=75. The portal is *not* drowned, which is what keeps the End leaveable |
| leaving | the player was set to survival and stood in a portal block at (1.5, 71.5, 0.5); the client showed the End poem and the player returned to the overworld |

The fight still runs at either waterline: the dragon flies, the crystals heal at the vanilla rate, the pillars still
perch and the portal still activates and works. What the drowned preset changes is the arena floor.

## Where a ship may moor

Mooring moves a structure piece, and two limits come with that. Both were found by measuring generated worlds rather
than by reasoning about them.

**A ship has to stay inside the structure's reference radius.** A chunk only places the pieces of a structure start
that is at most `ChunkStatus.MAX_STRUCTURE_DISTANCE` (8) chunks away from the chunk that created it. The first
version of the mooring searched 128 blocks and produced a ship whose hull reached a ninth chunk away: the far third of
it was never placed, and it stood in the sea half built. The search now rejects any position whose hull box leaves the
placement chunk's eight-chunk neighbourhood.

**A ship has to stay in its city's own neighbourhood.** The reach is 48 blocks, which is far more than the mooring
ever needs — the ships of a watched world moved between 1 and 14 blocks to find water — and it keeps a moored ship
close to the city it belongs to.

One generated world still shows a residue of the earlier, wider search: a 174-block bow of an End ship stands at
(-1728, 54, 45) with no matching structure start in the save, while the same city's complete ship is a hundred blocks
away. The fragment is unchanged by the reach (identical at 48 and 128 blocks) and by a regeneration in which each
city's structure start chunk was generated before its neighbours, so it is not the mooring alone; the End cities the
delivered captures were shot around all carry complete 929-block ships.


## Not in this release

**Submerged flora.** The measurement is against it for now. Of the 2662222 water columns in the delivered world,
66.07% are the full 58-block open-ocean column, 25.63% are 10–20 blocks deep, 8.3% are deeper than 20 and shallower
than full, and no column is shallower than 10 blocks. Seagrass and kelp are placed on the ocean floor, so two thirds
of the End's water would grow plants 58 blocks below the surface where nobody can see them; the visible quarter is
the shelf under the islands. It would also change the delivered world after its renders were shot, so the
photographed world would stop matching the shipped one. Done properly it is its own change: a placed feature over
the End biomes restricted to columns with at most 12 blocks of water, using `minecraft:seagrass` and a kelp variant
tall enough to reach into the light, with its own world regeneration and re-shoot.

**An aquatic mob.** One mob, not a family: a passive fish shoal in the End ocean would give the water life without
touching the dragon fight. It needs a new entity, spawn placement and loot, and it belongs behind the same preset
mechanism, because it changes what a player meets while crossing the sea.

**Mechanics.** The only mechanic the flooding itself suggests is Endermen reacting to water — avoiding it or taking
damage in it, which is the video's "the End drained" idea. It would change mob behaviour in every existing flooded
End world, so it belongs behind a config flag rather than in world generation.
