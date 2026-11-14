# The End under water

What FloodedEnd's ocean does, measured from generated worlds. Everything below was
read back out of saved region files (`captures-floodedend/tools/region-readback.py` in the capture workspace) or out
of a running dedicated server with a production client connected.

## The waterline

The End's terrain is unchanged, so the sea level only has to be chosen well. These measurements chose it, in the
delivered world (seed 1234567890, sea level 58). They were measured on Minecraft 26.2; the main island's ground and
the ground at the exit portal were measured again on 26.3 (single-precision terrain) with the same result, by the
GameTest `main_island_stays_above_the_water`, which logs them. The other rows are 26.2 measurements.

| landmark | y |
| --- | --- |
| water surface | 57 |
| main island's ground around the exit portal (within 40 blocks) | min 59, median 62, max 63 (26.2 and 26.3) |
| exit portal | a 5x5 ring of `minecraft:end_portal` at y=63, the dragon egg above it at y=67 |
| obsidian pillars | bases at y=32, tops from y=77 to y=104 |
| arrival platform | 5x5 of obsidian at y=48 |
| End cities | bases from y=60 to y=63 |
| End ships, before this change | hull from y=130 to y=153, decks around y=136 |

So a water surface at y=57 sits on the island's rim: the island stays dry by two to six blocks, the exit portal and
the pillars stay dry, and the arrival platform is nine blocks under water. The whole surrounding void floods: 66% of
the water columns in the delivered world are the full 58-block column.

## Ship placement

Before this change the End ship kept vanilla's placement, at the top of the city's tower: hulls sat at y=130-153,
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

The cities themselves use the dry End's terrain-based placement gate, because `floodedend:end_city` asks the ocean
floor heightmap for placement. Vanilla asks the world surface heightmap, so a higher ocean can raise the placement
box above the real terrain and admit cities whose ground would otherwise be too low.

## The dragon fight

Run end to end in both presets, on a dedicated server with a production client. Arrival was a real portal: an End
portal block was placed in the overworld and the player walked into it, which runs the same code path as a
stronghold portal (`EndPortalBlock` builds the arrival platform and moves the player to the End's spawn point).

### `moored` (the shipped ocean, sea level 58)

| step | what was observed |
| --- | --- |
| arrival | player landed at (100.5, 49.0, 0.5), nine blocks under the water surface. The column at (100, 0) reads identically before and after the arrival – water 0-57, obsidian at 48 – because the platform rebuild only runs when the block under the spawn point is not already obsidian, and it is |
| dragon | `Found that the dragon has not yet been killed in this world.` Spawned at 200 health and flew at y=67 to y=84, entirely above the water |
| crystals | ten, one per pillar, at y=80 |
| crystal healing | health written to 60, read back 95 after 17 seconds: 2.0 health per second, exactly the vanilla rate. After killing all ten crystals, 60 stayed 60 for 33 seconds |
| pillars | unchanged; bases at y=32, tops y=77-104 |
| portal activation | the dragon was killed, the egg appeared at (0, 67, 0) and the portal blocks at y=63 |
| leaving | the player stood in a portal block at (1.5, 63.5, 0.5); the client showed the End poem and the player returned to the overworld |

The fight itself is vanilla. The only flooded difference a player meets is arriving in nine blocks of water and
swimming up, which takes less than one breath.

### `drowned` (sea level 72, installed with `/floodedend preset apply drowned`)

| step | what was observed |
| --- | --- |
| arrival | player landed at (100.5, 49.0, 0.5), 22 blocks under the water surface. In survival that is more than one breath of swimming; the surface is about 20 seconds away at vanilla swim speed |
| dragon | spawned at 200 health and flew at y=93, above the water |
| crystals | ten, at y=86 – 15 blocks above the water, because the pillars are taller than the drowned sea |
| crystal healing | 60 health read back as 132 after 36 seconds: again 2.0 health per second |
| pillars | tops y=77-104, so they still stand out of the water and still perch |
| island and portal | the island's ground (y=59-63) is 8 to 12 blocks under water. The fight rebuilds the exit portal by scanning down from the sky, and with an ocean the first thing it meets is the water surface: the portal was built at y=71 on top of the sea with the dragon egg above it at y=75. The portal is *not* drowned, which is what keeps the End leaveable |
| leaving | the player was set to survival and stood in a portal block at (1.5, 71.5, 0.5); the client showed the End poem and the player returned to the overworld |

The fight still runs at either waterline: the dragon flies, the crystals heal at the vanilla rate, the pillars still
perch and the portal still activates and works. The drowned preset changes only the arena floor.

## Where a ship may moor

Mooring moves a structure piece, which brings two limits. Both were found by measuring generated worlds, not by
reasoning about them.

**A ship has to stay inside the structure's reference radius.** A chunk only places the pieces of a structure start
that is at most `ChunkStatus.MAX_STRUCTURE_DISTANCE` (8) chunks away from the chunk that created it. The first
version of the mooring searched 128 blocks and produced a ship whose hull reached a ninth chunk away: the far third of
it was never placed, and it stood in the sea half built. The search now rejects any position whose hull box leaves the
placement chunk's eight-chunk neighbourhood.

**A ship has to stay in its city's own neighbourhood.** The reach is 48 blocks, which is far more than the mooring
ever needs – the ships of a watched world moved between 1 and 14 blocks to find water – and it keeps a moored ship
close to the city it belongs to.

## Ship defects

### Small End islands in a hull

The `small_end_islands` biome places `minecraft:end_island_decorated`, small islands of end stone between y=55 and
y=70, in the first generation step. They are features, so the terrain height that the mooring reads does not include
them. Vanilla's island writes every block of its blob without asking, and a chunk's features may write one chunk
around their own, while a chunk places its structures before its own features but after the features of the chunks
generated before it. An island placed from a neighbouring chunk after the ship's own chunk was decorated therefore
replaced hull blocks with end stone, and which of the two chunks was decorated first changed from run to run.

A diagnostic GameTest switched structure generation on in the GameTest world (seed 0), generated the chunks under five
moored ships and compared 898 hull blocks of each with the ship template. Over three runs, three ships were whole every
time, one lost 2, 2 and 0 blocks, and one lost 37, 37 and 50. Four of the five hulls lie over `small_end_islands`,
so keeping ships away from that biome would leave most of them in the sky.

FloodedEnd now keeps the island out of every structure piece within `ChunkStatus.MAX_STRUCTURE_DISTANCE` chunks of its
own chunk: the island reads the pieces of the structure starts in that neighbourhood, the same bound that keeps a moored
ship inside its city's neighbourhood, and skips a block that lies inside one of them. The island keeps growing outside
the boxes, so an island next to a ship stops at the piece's bounding box instead of replacing it. The box is the whole
piece, air included, so an island that overlaps an empty corner of a ship's box is cut straight off there. The GameTest
`small_end_islands_keep_out_of_ships` puts vanilla's ship template in the End, registers it as a structure start of that
chunk the way a generated city's ship is, grows vanilla's island feature over it and checks that no block of the ship
changed.

`island_keeps_out_of_chunk_whose_start_is_beyond_the_scan` puts the ship in the chunk next to the
island's, one chunk inside the island's write radius, and its start the whole structure distance farther, at
nine chunks from the island's own chunk: the island then leaves that chunk alone entirely, because a chunk
holds a reference to every start whose box reaches into it (`ChunkGenerator.java:633-650`) and this one names a
start the scan cannot see. The island also reads the starts of every chunk the level holds, not only of full
chunks: a gateway's exit island is placed through the server level (`TheEndGatewayBlockEntity.java:156-165`),
where the chunks around the exit are held below a full chunk. No GameTest covers that case: a start cannot be
kept in a chunk below a full chunk in the GameTest world, because the level's ticket for such a chunk expires
(`TicketType.java:24`, `TicketStorage.java:279-286`).

### A lone bow in a 26.2 world

One world generated on Minecraft 26.2 has a 174-block bow of an End ship at (-1728, 54, 45) with no matching
structure start in the save, while the same city's complete ship is a hundred blocks away. The fragment was unchanged
by the reach (identical at 48 and 128 blocks) and by a regeneration in which each city's structure start chunk was
generated before its neighbours. Its cause is unknown. On 26.3 the nearest End city of that seed, at chunk (-115, 1),
gave the same pieces in two computations and moors its ship at x=-1729 to -1701, z=27 to 39. The hull's farthest chunk is
8 chunks from the start chunk, inside the reference radius. The End cities the delivered captures were shot around all
carry complete 929-block ships.

## Not in this release

**Submerged flora.** The measurements rule this out for now. Of the 2662222 water columns in the delivered world,
66.07% are the full 58-block open-ocean column, 25.63% are 10-20 blocks deep, 8.3% are deeper than 20 and shallower
than full, and no column is shallower than 10 blocks. Seagrass and kelp are placed on the ocean floor, so two thirds
of the End's water would grow plants 58 blocks below the surface where nobody can see them; the visible quarter is
the shelf under the islands. It would also change the delivered world after its renders were shot, so the
photographed world would stop matching the shipped one. Done properly, it is a separate change: a placed feature over
the End biomes restricted to columns with at most 12 blocks of water, using `minecraft:seagrass` and a kelp variant
tall enough to reach into the light, with its own world regeneration and re-shoot.

**An aquatic mob.** A single passive fish shoal in the End ocean would give the water life without touching the
dragon fight. It needs a new entity, spawn placement and loot, and it belongs behind the same preset
mechanism, because it changes what a player meets while crossing the sea.

**Mechanics.** The flooding suggests one mechanic: Endermen reacting to water, by avoiding it or taking damage in
it, which is the video's "the End drained" idea. It would change mob behaviour in every existing flooded
End world, so it belongs behind a config flag rather than in world generation.
