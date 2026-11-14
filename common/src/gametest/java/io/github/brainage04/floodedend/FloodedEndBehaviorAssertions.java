package io.github.brainage04.floodedend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class FloodedEndBehaviorAssertions {
    /** The seed of the world {@code docs/THE_END_UNDER_WATER.md} was measured in. */
    static final long DOCUMENTED_SEED = 1234567890L;

    private FloodedEndBehaviorAssertions() {}

    public static void assertCommandsRespectPermissionsAndReportErrors(GameTestHelper helper) {
        Feedback feedback = new Feedback();
        CommandSourceStack source =
                helper.getLevel().getServer().createCommandSourceStack().withSource(feedback);
        try {
            command(
                    source.withPermission(LevelBasedPermissionSet.forLevel(PermissionLevel.ALL)),
                    "status");
            helper.fail(
                    "players without gamemaster permission must not execute FloodedEnd commands");
        } catch (CommandSyntaxException expected) {
            // Brigadier must reject the command, not merely hide its suggestions.
        }
        try {
            helper.assertValueEqual(command(source, "status"), 1, "the live End ocean is active");
            helper.assertTrue(
                    feedback.contains(
                            "minecraft:the_end: End sea level 58, default fluid minecraft:water"),
                    "status reads the live generator");
            helper.assertValueEqual(
                    EndOceanPreset.MOORED.seaLevel(),
                    EndOcean.of(helper.getLevel().getServer().getLevel(Level.END)).seaLevel(),
                    "the moored preset is the shipped ocean");
            helper.assertValueEqual(
                    command(source, "status minecraft:the_end"),
                    1,
                    "status takes the dimension to report");
            helper.assertValueEqual(
                    command(source, "status minecraft:overworld"),
                    0,
                    "a dimension that does not generate from the End's settings has no End ocean");
            helper.assertTrue(
                    feedback.contains(
                            "minecraft:overworld does not generate from the minecraft:end noise"
                                    + " settings"),
                    "status names the dimension it rejects");
            helper.assertValueEqual(command(source, "preset list"), 2, "both presets are listed");
            helper.assertTrue(
                    feedback.contains("moored") && feedback.contains("drowned"),
                    "list names both presets");
            helper.assertValueEqual(
                    command(source, "preset apply unknown"), 0, "unknown presets fail");
            helper.assertTrue(
                    feedback.contains("Unknown FloodedEnd preset 'unknown'"),
                    "unknown preset feedback is useful");
            helper.assertValueEqual(
                    command(source, "probe 29999984 29999984"),
                    0,
                    "probing an unloaded chunk fails");
            helper.assertTrue(
                    feedback.contains("No minecraft:the_end chunk loaded"),
                    "probe explains unloaded chunks");
        } catch (CommandSyntaxException error) {
            throw new AssertionError("the registered commands must execute", error);
        }
        helper.succeed();
    }

    public static void assertGeneratedOceanAndProbe(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "the End is loaded");
        // Inside the central void ring, away from the island and the arrival platform.
        end.getChunk(32, 32);
        EndOcean.Column column = EndOcean.probe(end, 512, 512);
        helper.assertValueEqual(
                column.waterSurfaceY(), OptionalInt.of(57), "generated ocean surface");
        helper.assertValueEqual(column.deepestWaterY(), OptionalInt.of(0), "generated ocean floor");
        helper.assertValueEqual(column.waterBlocks(), 58, "the entire void column floods");
        helper.assertTrue(
                end.getBlockState(new BlockPos(512, 58, 512)).isAir(),
                "blocks above the sea remain air");
        Feedback feedback = new Feedback();
        CommandSourceStack source =
                end.getServer()
                        .createCommandSourceStack()
                        .withSource(feedback)
                        .withLevel(end)
                        .withPosition(new net.minecraft.world.phys.Vec3(512, 64, 512));
        BlockPos here = helper.absolutePos(BlockPos.ZERO);
        CommandSourceStack elsewhere =
                source.withLevel(helper.getLevel())
                        .withPosition(net.minecraft.world.phys.Vec3.atCenterOf(here));
        try {
            helper.assertValueEqual(
                    command(source, "probe"), 1, "probe uses the source's x/z in the End");
            helper.assertValueEqual(
                    command(source, "probe 512 512"), 1, "explicit probe coordinates work");
            helper.assertTrue(
                    feedback.contains(
                            "minecraft:the_end column (512, 512): surface minecraft:water at y=57;"
                                    + " water 58 blocks from y=0 to y=57"),
                    "probe describes real blocks");
            helper.assertValueEqual(
                    command(elsewhere, "probe"),
                    0,
                    "without coordinates, probe reads the source's own dimension");
            helper.assertTrue(
                    feedback.contains(
                            helper.getLevel().dimension().identifier()
                                    + " column ("
                                    + here.getX()
                                    + ", "
                                    + here.getZ()
                                    + ")"),
                    "probe names the dimension it read");
            helper.assertValueEqual(
                    command(elsewhere, "probe 512 512 minecraft:the_end"),
                    1,
                    "probe takes the dimension to read");
        } catch (CommandSyntaxException error) {
            throw new AssertionError(error);
        }
        helper.succeed();
    }

    /**
     * The shipped sea level must leave the main island dry. Measures the generated ground (the top
     * non-fluid block) within 40 blocks of the exit portal for this test world's seed and for the
     * seed {@code THE_END_UNDER_WATER.md} was measured with, straight from the End's noise, and
     * logs the numbers the documentation quotes.
     */
    public static void assertMainIslandStaysAboveTheWater(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "the End is loaded");
        var generator = (NoiseBasedChunkGenerator) end.getChunkSource().getGenerator();
        EndOcean ocean = EndOcean.of(end);
        helper.assertTrue(ocean.flooded(), "the End generates its ocean");
        for (long seed : new long[] {end.getSeed(), DOCUMENTED_SEED}) {
            RandomState randomState =
                    RandomState.create(
                            end.registryAccess().lookupOrThrow(Registries.NOISE),
                            seed,
                            generator.generatorSettings().value());
            List<Integer> ground = new ArrayList<>();
            for (int x = -40; x <= 40; x++) {
                for (int z = -40; z <= 40; z++) {
                    if (x * x + z * z <= 40 * 40) {
                        ground.add(groundY(generator, end, randomState, x, z));
                    }
                }
            }
            ground.sort(null);
            int min = ground.getFirst();
            int median = ground.get(ground.size() / 2);
            int max = ground.getLast();
            int portal = groundY(generator, end, randomState, 0, 0);
            FloodedEnd.LOGGER.info(
                    "FloodedEnd main island ground within 40 blocks of the exit portal, seed {}:"
                            + " min {}, median {}, max {}; ground at the portal {}; water surface {}",
                    seed,
                    min,
                    median,
                    max,
                    portal,
                    ocean.waterSurfaceY());
            helper.assertTrue(
                    min > ocean.waterSurfaceY(),
                    "the main island stays above the water surface for seed "
                            + seed
                            + " (lowest ground y="
                            + min
                            + ")");
        }
        helper.succeed();
    }

    private static int groundY(
            NoiseBasedChunkGenerator generator,
            ServerLevel end,
            RandomState randomState,
            int x,
            int z) {
        // A heightmap height is the first y above the column's top block.
        return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, end, randomState) - 1;
    }

    public static void assertProbeHandlesDryAndCappedColumns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        List<BlockState> original = new ArrayList<>();
        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos(origin.getX(), 0, origin.getZ());
        try {
            for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
                cursor.setY(y);
                original.add(level.getBlockState(cursor));
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
            }
            helper.assertTrue(level.getMinY() < -1, "the test level builds below y=-1");
            int top = level.getMaxY();
            int[] waterYs = {-1, 0, top};
            for (int y : waterYs) {
                level.setBlock(cursor.setY(y), Blocks.WATER.defaultBlockState(), 2);
            }
            String waterOnly = EndOcean.probe(level, origin.getX(), origin.getZ()).describe();
            for (int y : waterYs) {
                level.setBlock(cursor.setY(y), Blocks.AIR.defaultBlockState(), 2);
            }
            String airOnly = EndOcean.probe(level, origin.getX(), origin.getZ()).describe();
            String column = "column (" + origin.getX() + ", " + origin.getZ() + "): ";
            helper.assertValueEqual(
                    List.of(waterOnly, airOnly),
                    List.of(
                            column
                                    + "surface minecraft:water at y="
                                    + top
                                    + "; water 3 blocks from y=-1 to y="
                                    + top
                                    + "; no non-water block",
                            column + "only air; no water"),
                    "the probe of water at y=-1, y=0 and the top build layer, then of air only");
            level.setBlock(origin, Blocks.END_STONE.defaultBlockState(), 2);
            EndOcean.Column dry = EndOcean.probe(level, origin.getX(), origin.getZ());
            helper.assertTrue(
                    !dry.isFlooded() && dry.describe().endsWith("; no water"),
                    "dry columns report no water");
            level.setBlock(origin.above(), Blocks.WATER.defaultBlockState(), 2);
            level.setBlock(origin.above(3), Blocks.WATER.defaultBlockState(), 2);
            level.setBlock(origin.above(4), Blocks.END_STONE.defaultBlockState(), 2);
            EndOcean.Column capped = EndOcean.probe(level, origin.getX(), origin.getZ());
            helper.assertValueEqual(capped.waterBlocks(), 2, "air gaps are not counted as water");
            helper.assertValueEqual(
                    capped.deepestWaterY(),
                    OptionalInt.of(origin.getY() + 1),
                    "lowest water block");
            helper.assertValueEqual(
                    capped.waterSurfaceY(),
                    OptionalInt.of(origin.getY() + 3),
                    "highest water block");
            helper.assertValueEqual(
                    capped.landY(),
                    OptionalInt.of(origin.getY() + 4),
                    "solid cap is the highest non-water block");
            helper.assertValueEqual(
                    capped.surface(), "minecraft:end_stone", "solid cap is the surface");
        } finally {
            for (int i = 0; i < original.size(); i++) {
                level.setBlock(
                        cursor.set(origin.getX(), level.getMinY() + i, origin.getZ()),
                        original.get(i),
                        2);
            }
        }
        helper.succeed();
    }

    public static void assertShipsMoorAtBothWaterlines(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        var generator = end.getChunkSource().getGenerator();
        RandomState randomState = end.getChunkSource().randomState();
        Structure.GenerationContext context =
                new Structure.GenerationContext(
                        end.registryAccess(),
                        generator,
                        generator.getBiomeSource(),
                        randomState.createClimateSampler(
                                SamplerContext.builder().enableCaches().build()),
                        randomState,
                        end.getStructureTemplateManager(),
                        end.getSeed(),
                        new ChunkPos(32, 32),
                        end,
                        biome -> true);
        for (Rotation rotation : Rotation.values()) {
            for (EndOceanPreset preset : EndOceanPreset.PRESETS) {
                var ship =
                        new EndCityPieces.EndCityPiece(
                                end.getStructureTemplateManager(),
                                "ship",
                                new BlockPos(512, 120, 512),
                                rotation,
                                true);
                var tower =
                        new EndCityPieces.EndCityPiece(
                                end.getStructureTemplateManager(),
                                "base_floor",
                                new BlockPos(512, 64, 512),
                                rotation,
                                true);
                BlockPos towerPosition = tower.templatePosition();
                EndShipMooring.moor(
                        context,
                        List.of(ship, tower),
                        new EndOcean(preset.seaLevel(), Blocks.WATER.defaultBlockState(), false));
                helper.assertValueEqual(
                        ship.getBoundingBox().minY(),
                        preset.seaLevel() - 4,
                        "ship keel follows the preset waterline");
                helper.assertValueEqual(
                        tower.templatePosition(),
                        towerPosition,
                        "mooring does not move city towers");
            }
        }
        var outside =
                new EndCityPieces.EndCityPiece(
                        end.getStructureTemplateManager(),
                        "ship",
                        new BlockPos(1024, 120, 1024),
                        Rotation.NONE,
                        true);
        BlockPos position = outside.templatePosition();
        EndShipMooring.moor(context, List.<StructurePiece>of(outside), EndOcean.of(end));
        helper.assertValueEqual(
                outside.templatePosition(),
                position,
                "a ship with no placeable mooring stays at its original position");
        var aboveBuildHeight =
                new EndCityPieces.EndCityPiece(
                        end.getStructureTemplateManager(),
                        "ship",
                        new BlockPos(512, 120, 512),
                        Rotation.NONE,
                        true);
        BlockPos skyPosition = aboveBuildHeight.templatePosition();
        EndShipMooring.moor(
                context,
                List.<StructurePiece>of(aboveBuildHeight),
                new EndOcean(250, Blocks.WATER.defaultBlockState(), false));
        helper.assertValueEqual(
                aboveBuildHeight.templatePosition(),
                skyPosition,
                "a ship whose moored hull would reach above the build height stays where it is");
        var coast =
                new Structure.GenerationContext(
                        end.registryAccess(),
                        generator,
                        generator.getBiomeSource(),
                        randomState.createClimateSampler(
                                SamplerContext.builder().enableCaches().build()),
                        randomState,
                        end.getStructureTemplateManager(),
                        end.getSeed(),
                        new ChunkPos(0, 0),
                        end,
                        biome -> true);
        int moored = 0;
        for (Rotation rotation : Rotation.values()) {
            for (int x : new int[] {64, 96}) {
                var ship =
                        new EndCityPieces.EndCityPiece(
                                end.getStructureTemplateManager(),
                                "ship",
                                new BlockPos(x, 120, 0),
                                rotation,
                                true);
                EndShipMooring.moor(coast, List.of(ship), EndOcean.of(end));
                var hull = ship.getBoundingBox();
                if (hull.minY() != 54) {
                    continue;
                }
                moored++;
                for (int hullX = hull.minX(); hullX <= hull.maxX(); hullX++) {
                    for (int hullZ = hull.minZ(); hullZ <= hull.maxZ(); hullZ++) {
                        int ground =
                                generator.getFirstOccupiedHeight(
                                        hullX,
                                        hullZ,
                                        net.minecraft.world.level.levelgen.Heightmap.Types
                                                .OCEAN_FLOOR_WG,
                                        end,
                                        end.getChunkSource().randomState());
                        helper.assertTrue(
                                ground <= 52,
                                "every hull column clears the real seabed, including between old"
                                        + " grid samples");
                    }
                }
            }
        }
        helper.assertTrue(moored > 0, "the shoreline samples include a moored ship");
        helper.succeed();
    }

    public static void assertCityGenerationPreservesDryVanilla(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        var city =
                (EndCityAtSea)
                        end.registryAccess()
                                .lookupOrThrow(Registries.STRUCTURE)
                                .getOrThrow(
                                        ResourceKey.create(
                                                Registries.STRUCTURE,
                                                Identifier.withDefaultNamespace("end_city")))
                                .value();
        var vanilla =
                new EndCityStructure(
                        new Structure.StructureSettings(
                                city.biomes(),
                                city.spawnOverrides(),
                                city.step(),
                                city.terrainAdaptation()));
        var flooded = (NoiseBasedChunkGenerator) end.getChunkSource().getGenerator();
        var settings = flooded.generatorSettings().value();
        // The record's disableMobGeneration accessor is deprecated with no replacement
        // (NoiseGeneratorSettings.java:32 in 26.3), so the dry copy goes through the codec, which
        // still carries that field.
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, end.registryAccess());
        JsonObject dryJson =
                NoiseGeneratorSettings.DIRECT_CODEC
                        .encodeStart(ops, settings)
                        .getOrThrow(error -> new AssertionError("encode End settings: " + error))
                        .getAsJsonObject();
        dryJson.add(
                "default_fluid",
                BlockState.CODEC
                        .encodeStart(ops, Blocks.AIR.defaultBlockState())
                        .getOrThrow(error -> new AssertionError("encode air: " + error)));
        dryJson.addProperty("sea_level", 0);
        var drySettings =
                NoiseGeneratorSettings.DIRECT_CODEC
                        .parse(ops, dryJson)
                        .getOrThrow(error -> new AssertionError("decode dry settings: " + error));
        var dry =
                new NoiseBasedChunkGenerator(flooded.getBiomeSource(), Holder.direct(drySettings));
        int ships = 0;
        for (int seed = 0; seed < 16; seed++) {
            var expected =
                    vanilla.findGenerationPoint(cityContext(end, dry, seed))
                            .orElseThrow()
                            .getPiecesBuilder()
                            .build()
                            .pieces();
            var actual =
                    city.findGenerationPoint(cityContext(end, dry, seed))
                            .orElseThrow()
                            .getPiecesBuilder()
                            .build()
                            .pieces();
            helper.assertValueEqual(
                    actual.size(), expected.size(), "dry End has vanilla's piece count");
            for (int i = 0; i < expected.size(); i++) {
                helper.assertValueEqual(
                        actual.get(i).getBoundingBox().toString(),
                        expected.get(i).getBoundingBox().toString(),
                        "dry End keeps every vanilla piece in place");
            }
            var wet =
                    city.findGenerationPoint(cityContext(end, flooded, seed))
                            .orElseThrow()
                            .getPiecesBuilder()
                            .build()
                            .pieces();
            helper.assertValueEqual(
                    wet.size(), expected.size(), "flooding preserves the city's pieces");
            for (int i = 0; i < wet.size(); i++) {
                var piece =
                        (net.minecraft.world.level.levelgen.structure.TemplateStructurePiece)
                                wet.get(i);
                if (piece.template()
                        == end.getStructureTemplateManager()
                                .getOrCreate(Identifier.withDefaultNamespace("end_city/ship"))) {
                    ships++;
                } else {
                    helper.assertValueEqual(
                            piece.getBoundingBox().toString(),
                            expected.get(i).getBoundingBox().toString(),
                            "flooding does not move city buildings");
                }
            }
        }
        helper.assertTrue(ships > 0, "the deterministic city samples include ships");
        helper.succeed();
    }

    /**
     * Vanilla's small End island writes every block of its blob without asking ({@code
     * EndIslandFeature.java:27}), so one that grows across a chunk border after that neighbour
     * placed its structures replaces parts of a moored ship's hull with end stone. FloodedEnd keeps
     * the island out of every structure piece around its chunk. This test puts vanilla's own ship
     * template in the End, registers it as a structure start of that chunk the way a generated
     * city's ship is, grows vanilla's island feature over it and compares every block of the hull's
     * box with the state it had before the island.
     */
    public static void assertSmallEndIslandsKeepOutOfShips(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        RegistryAccess access = end.registryAccess();
        Structure city = cityStructure(access);
        Feature island = islandFeature(access);
        StructureTemplateManager templates = end.getStructureTemplateManager();
        StructureTemplate ship =
                templates.getOrCreate(Identifier.withDefaultNamespace("end_city/ship"));
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        // Far above the End's terrain and clear of the GameTest structures.
        BlockPos position = new BlockPos(20000, 150, 20000);
        ChunkPos chunkPos =
                new ChunkPos(
                        SectionPos.blockToSectionCoord(position.getX()),
                        SectionPos.blockToSectionCoord(position.getZ()));
        ChunkAccess chunk = end.getChunk(chunkPos.x(), chunkPos.z());
        Map<Structure, StructureStart> previousStarts = new HashMap<>(chunk.getAllStarts());
        var piece =
                new EndCityPieces.EndCityPiece(templates, "ship", position, Rotation.NONE, true);
        BoundingBox hull = piece.getBoundingBox();
        // At the hull's edge, so part of the island lands outside the ship and part inside it.
        BlockPos islandOrigin =
                new BlockPos(hull.minX() + 2, hull.minY() + 4, hull.getCenter().getZ());
        try {
            ship.placeInWorld(end, position, position, settings, RandomSource.create(1), 2);
            chunk.setStartForStructure(
                    city,
                    new StructureStart(city, chunkPos, 0, new PiecesContainer(List.of(piece))));
            List<BlockState> hullBefore = hullStates(end, hull);
            int hullBlocks = 0;
            for (BlockState state : hullBefore) {
                if (!state.isAir()) {
                    hullBlocks++;
                }
            }
            helper.assertTrue(
                    hullBlocks > 500, "the ship template is in the world before the island runs");
            int endStoneBefore = endStoneOutside(end, islandOrigin, hull);

            island.place(
                    end, end.getChunkSource().getGenerator(), RandomSource.create(2), islandOrigin);

            List<BlockState> hullAfter = hullStates(end, hull);
            List<String> changed = new ArrayList<>();
            for (int i = 0; i < hullBefore.size(); i++) {
                if (!hullBefore.get(i).equals(hullAfter.get(i))) {
                    changed.add(hullBefore.get(i) + " became " + hullAfter.get(i));
                }
            }
            helper.assertTrue(
                    changed.isEmpty(),
                    "the island places no block inside the ship, but "
                            + changed.size()
                            + " did change: "
                            + changed.stream().limit(5).toList());
            helper.assertTrue(
                    endStoneOutside(end, islandOrigin, hull) > endStoneBefore,
                    "the island still places its end stone outside the ship");
        } finally {
            clear(end, hull.inflatedBy(14));
            chunk.setAllStarts(previousStarts);
        }
        helper.succeed();
    }

    /**
     * An island writes one chunk past its own chunk, and a chunk holds a reference to every
     * structure start whose box reaches into it ({@code ChunkGenerator.java:633-650}). A ship
     * moored at the far end of its reach stands in the chunk the island writes into while its
     * city's start is one chunk farther from the island than the scan reaches ({@link
     * EndShipMooring}), so the island must leave that chunk to the structures it holds.
     */
    public static void assertIslandKeepsOutOfChunkWhoseStartIsBeyondTheScan(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        RegistryAccess access = end.registryAccess();
        Structure city = cityStructure(access);
        Feature island = islandFeature(access);
        StructureTemplateManager templates = end.getStructureTemplateManager();
        StructureTemplate ship =
                templates.getOrCreate(Identifier.withDefaultNamespace("end_city/ship"));
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        // The chunk one east of the island's own; the island's origin sits on its west border.
        ChunkPos written = new ChunkPos(1270, 1270);
        BlockPos position = new BlockPos(written.getMinBlockX(), 150, written.getMinBlockZ());
        ChunkAccess writtenChunk = end.getChunk(written.x(), written.z());
        Map<Structure, StructureStart> previousWrittenStarts =
                new HashMap<>(writtenChunk.getAllStarts());
        // addReferenceForStructure adds to the chunk's own set, so the copy needs sets of its own.
        Map<Structure, LongSet> previousWrittenReferences = new HashMap<>();
        writtenChunk
                .getAllReferences()
                .forEach(
                        (structure, starts) ->
                                previousWrittenReferences.put(
                                        structure, new LongOpenHashSet(starts)));
        var piece =
                new EndCityPieces.EndCityPiece(templates, "ship", position, Rotation.NONE, true);
        BoundingBox hull = piece.getBoundingBox();
        // A mooring shift of the whole structure distance puts the start nine chunks from the
        // island's own chunk, one past the reach of every scan.
        ChunkPos startPos =
                new ChunkPos(written.x() + ChunkStatus.MAX_STRUCTURE_DISTANCE, written.z());
        ChunkAccess startChunk = end.getChunk(startPos.x(), startPos.z());
        Map<Structure, StructureStart> previousStartChunkStarts =
                new HashMap<>(startChunk.getAllStarts());
        BlockPos islandOrigin =
                new BlockPos(
                        written.getMinBlockX() - 1, hull.minY() + 4, written.getMinBlockZ() + 8);
        try {
            ship.placeInWorld(end, position, position, settings, RandomSource.create(1), 2);
            startChunk.setStartForStructure(
                    city,
                    new StructureStart(city, startPos, 0, new PiecesContainer(List.of(piece))));
            // What the level's own generation writes for a start whose box reaches the chunk.
            writtenChunk.addReferenceForStructure(city, startPos.pack());
            List<BlockState> hullBefore = hullStates(end, hull);
            int hullBlocks = 0;
            for (BlockState state : hullBefore) {
                if (!state.isAir()) {
                    hullBlocks++;
                }
            }
            helper.assertTrue(
                    hullBlocks > 500, "the ship template is in the world before the island runs");
            int endStoneBefore = endStoneOutside(end, islandOrigin, hull);

            island.place(
                    end, end.getChunkSource().getGenerator(), RandomSource.create(2), islandOrigin);

            List<BlockState> hullAfter = hullStates(end, hull);
            List<String> changed = new ArrayList<>();
            for (int i = 0; i < hullBefore.size(); i++) {
                if (!hullBefore.get(i).equals(hullAfter.get(i))) {
                    changed.add(hullBefore.get(i) + " became " + hullAfter.get(i));
                }
            }
            helper.assertTrue(
                    changed.isEmpty(),
                    "the island leaves the chunk that holds a start beyond the scan alone, but "
                            + changed.size()
                            + " blocks of the ship changed: "
                            + changed.stream().limit(5).toList());
            helper.assertTrue(
                    endStoneOutside(end, islandOrigin, hull) > endStoneBefore,
                    "the island still places its end stone outside the ship");
        } finally {
            clear(end, hull.inflatedBy(14));
            writtenChunk.setAllStarts(previousWrittenStarts);
            writtenChunk.setAllReferences(previousWrittenReferences);
            startChunk.setAllStarts(previousStartChunkStarts);
        }
        helper.succeed();
    }

    private static Structure cityStructure(RegistryAccess access) {
        return access.lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(
                        ResourceKey.create(
                                Registries.STRUCTURE, Identifier.withDefaultNamespace("end_city")))
                .value();
    }

    private static Feature islandFeature(RegistryAccess access) {
        return access.lookupOrThrow(Registries.FEATURE)
                .getOrThrow(
                        ResourceKey.create(
                                Registries.FEATURE, Identifier.withDefaultNamespace("end_island")))
                .value();
    }

    private static List<BlockState> hullStates(ServerLevel level, BoundingBox box) {
        List<BlockState> states = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    states.add(level.getBlockState(cursor.set(x, y, z)));
                }
            }
        }
        return states;
    }

    /**
     * End stone in the island's blob outside the ship, so the test fails if the island stops
     * placing blocks at all instead of only inside the ship.
     */
    private static int endStoneOutside(ServerLevel level, BlockPos islandOrigin, BoundingBox hull) {
        int endStone = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = islandOrigin.getY() - 12; y <= islandOrigin.getY(); y++) {
            for (int z = islandOrigin.getZ() - 8; z <= islandOrigin.getZ() + 8; z++) {
                for (int x = islandOrigin.getX() - 8; x <= islandOrigin.getX() + 8; x++) {
                    BlockPos pos = cursor.set(x, y, z);
                    if (!hull.isInside(pos) && level.getBlockState(pos).is(Blocks.END_STONE)) {
                        endStone++;
                    }
                }
            }
        }
        return endStone;
    }

    private static void clear(ServerLevel level, BoundingBox box) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    level.setBlock(cursor.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static Structure.GenerationContext cityContext(
            ServerLevel end, ChunkGenerator generator, long seed) {
        RandomState randomState = end.getChunkSource().randomState();
        return new Structure.GenerationContext(
                end.registryAccess(),
                generator,
                generator.getBiomeSource(),
                randomState.createClimateSampler(SamplerContext.builder().enableCaches().build()),
                randomState,
                end.getStructureTemplateManager(),
                seed,
                new ChunkPos(0, 0),
                end,
                biome -> true);
    }

    static int command(CommandSourceStack source, String arguments) throws CommandSyntaxException {
        return source.getServer()
                .getCommands()
                .getDispatcher()
                .execute("floodedend " + arguments, source);
    }

    static final class Feedback implements CommandSource {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component message) {
            messages.add(message.getString());
        }

        boolean contains(String text) {
            return messages.stream().anyMatch(message -> message.contains(text));
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    }
}
