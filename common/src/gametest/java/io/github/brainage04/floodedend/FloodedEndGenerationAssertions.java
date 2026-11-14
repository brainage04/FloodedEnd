package io.github.brainage04.floodedend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class FloodedEndGenerationAssertions {
    private static final Identifier SHIP = Identifier.withDefaultNamespace("end_city/ship");

    private FloodedEndGenerationAssertions() {}

    /**
     * End biomes over the Overworld's noise settings, as the Single Biome world type set to End
     * Highlands generates them, are not the flooded End: their End cities must be vanilla's, also
     * where vanilla stands a city on the water of the Overworld's sea.
     */
    public static void assertEndCityOutsideTheEndStaysVanilla(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RegistryAccess access = level.registryAccess();
        EndCityAtSea city = endCity(access);
        EndCityStructure vanilla = vanillaCity(city);
        Holder<NoiseGeneratorSettings> overworld =
                access.lookupOrThrow(Registries.NOISE_SETTINGS)
                        .getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var generator =
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(
                                access.lookupOrThrow(Registries.BIOME)
                                        .getOrThrow(Biomes.END_HIGHLANDS)),
                        overworld);
        RandomState randomState =
                RandomState.create(
                        access.lookupOrThrow(Registries.NOISE),
                        FloodedEndBehaviorAssertions.DOCUMENTED_SEED,
                        overworld.value());
        StructureTemplateManager templates = level.getStructureTemplateManager();
        StructureTemplate ship = templates.getOrCreate(SHIP);
        int cities = 0;
        int onWater = 0;
        int ships = 0;
        for (int x = -96; x <= 96; x += 16) {
            for (int z = -96; z <= 96; z += 16) {
                ChunkPos chunk = new ChunkPos(x, z);
                var expected =
                        vanilla.findGenerationPoint(
                                context(access, generator, randomState, templates, level, chunk));
                var actual =
                        city.findGenerationPoint(
                                context(access, generator, randomState, templates, level, chunk));
                helper.assertValueEqual(
                        actual.isPresent(),
                        expected.isPresent(),
                        "a city generates exactly where vanilla's does, chunk " + chunk);
                if (expected.isEmpty()) {
                    continue;
                }

                cities++;
                List<StructurePiece> expectedPieces =
                        expected.get().getPiecesBuilder().build().pieces();
                helper.assertValueEqual(
                        boxes(actual.orElseThrow().getPiecesBuilder().build().pieces()),
                        boxes(expectedPieces),
                        "every piece, ships included, stays where vanilla puts it, chunk " + chunk);
                ships +=
                        (int)
                                expectedPieces.stream()
                                        .filter(
                                                piece ->
                                                        piece instanceof TemplateStructurePiece t
                                                                && t.template() == ship)
                                        .count();
                int blockX = chunk.getBlockX(7);
                int blockZ = chunk.getBlockZ(7);
                if (generator.getFirstOccupiedHeight(
                                blockX, blockZ, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState)
                        < generator.getFirstOccupiedHeight(
                                blockX,
                                blockZ,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                level,
                                randomState)) {
                    onWater++;
                }
            }
        }
        FloodedEnd.LOGGER.info(
                "FloodedEnd End cities over Overworld settings: {} cities, {} on water, {} ships",
                cities,
                onWater,
                ships);
        helper.assertTrue(onWater > 0, "the samples include a city standing on water");
        helper.assertTrue(ships > 0, "the samples include a ship");
        helper.succeed();
    }

    /**
     * FloodedEnd ships a whole copy of vanilla's {@code minecraft:end} noise settings. It must
     * differ from the vanilla data pack's copy in the fluid and the sea level only, so a Minecraft
     * update that changes the End's terrain fails here instead of being silently overridden.
     */
    public static void assertShippedEndSettingsFollowVanilla(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel end = server.getLevel(Level.END);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, end.registryAccess());
        Identifier file = Identifier.withDefaultNamespace("worldgen/noise_settings/end.json");
        Resource vanillaFile =
                server.getResourceManager().getResourceStack(file).stream()
                        .filter(
                                resource ->
                                        resource.sourcePackId()
                                                .equals(BuiltInPackSource.VANILLA_ID))
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError("the vanilla data pack carries " + file));
        NoiseGeneratorSettings vanilla;
        try (Reader reader = vanillaFile.openAsReader()) {
            vanilla =
                    NoiseGeneratorSettings.DIRECT_CODEC
                            .parse(ops, JsonParser.parseReader(reader))
                            .getOrThrow(
                                    error ->
                                            new AssertionError(
                                                    "vanilla's End settings decode: " + error));
        } catch (IOException error) {
            throw new AssertionError("could not read vanilla's End settings", error);
        }
        JsonObject expected = encode(ops, vanilla);
        JsonObject actual =
                encode(
                        ops,
                        ((NoiseBasedChunkGenerator) end.getChunkSource().getGenerator())
                                .generatorSettings()
                                .value());
        Set<String> fields = new TreeSet<>(expected.keySet());
        fields.addAll(actual.keySet());
        List<String> changed = new ArrayList<>();
        for (String field : fields) {
            if (!Objects.equals(expected.get(field), actual.get(field))) {
                changed.add(field);
            }
        }
        helper.assertValueEqual(
                changed,
                List.of("default_fluid", "sea_level"),
                "the live End settings differ from vanilla's in the fluid and the sea level only");
        helper.succeed();
    }

    private static EndCityAtSea endCity(RegistryAccess access) {
        return (EndCityAtSea)
                access.lookupOrThrow(Registries.STRUCTURE)
                        .getOrThrow(
                                ResourceKey.create(
                                        Registries.STRUCTURE,
                                        Identifier.withDefaultNamespace("end_city")))
                        .value();
    }

    private static EndCityStructure vanillaCity(EndCityAtSea city) {
        return new EndCityStructure(
                new Structure.StructureSettings(
                        city.biomes(),
                        city.spawnOverrides(),
                        city.step(),
                        city.terrainAdaptation()));
    }

    private static Structure.GenerationContext context(
            RegistryAccess access,
            ChunkGenerator generator,
            RandomState randomState,
            StructureTemplateManager templates,
            LevelHeightAccessor heightAccessor,
            ChunkPos chunk) {
        return new Structure.GenerationContext(
                access,
                generator,
                generator.getBiomeSource(),
                randomState.createClimateSampler(SamplerContext.builder().enableCaches().build()),
                randomState,
                templates,
                FloodedEndBehaviorAssertions.DOCUMENTED_SEED,
                chunk,
                heightAccessor,
                biome -> true);
    }

    private static List<String> boxes(List<StructurePiece> pieces) {
        return pieces.stream().map(piece -> piece.getBoundingBox().toString()).toList();
    }

    private static JsonObject encode(
            RegistryOps<JsonElement> ops, NoiseGeneratorSettings settings) {
        return NoiseGeneratorSettings.DIRECT_CODEC
                .encodeStart(ops, settings)
                .getOrThrow(error -> new AssertionError("encode noise settings: " + error))
                .getAsJsonObject();
    }
}
