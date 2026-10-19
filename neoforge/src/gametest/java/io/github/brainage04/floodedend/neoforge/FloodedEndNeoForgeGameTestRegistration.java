package io.github.brainage04.floodedend.neoforge;

import io.github.brainage04.floodedend.FloodedEnd;
import io.github.brainage04.floodedend.FloodedEndNeoForgeGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = FloodedEnd.MOD_ID)
public final class FloodedEndNeoForgeGameTestRegistration {
	private FloodedEndNeoForgeGameTestRegistration() {
	}

	@SubscribeEvent
	public static void registerTestFunctions(RegisterEvent event) {
		FloodedEndNeoForgeGameTests tests = new FloodedEndNeoForgeGameTests();
		event.register(
				BuiltInRegistries.TEST_FUNCTION.key(),
				FloodedEnd.of("end_generates_its_ocean"),
				() -> tests::endGeneratesItsOcean
		);
		event.register(
				BuiltInRegistries.TEST_FUNCTION.key(),
				FloodedEnd.of("preset_installs_an_ocean"),
				() -> tests::presetInstallsAnOcean
		);
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), FloodedEnd.of("commands_respect_permissions_and_report_errors"),
				() -> tests::commandsRespectPermissionsAndReportErrors);
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), FloodedEnd.of("generated_ocean_and_probe"),
				() -> tests::generatedOceanAndProbe);
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), FloodedEnd.of("probe_handles_dry_and_capped_columns"),
				() -> tests::probeHandlesDryAndCappedColumns);
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), FloodedEnd.of("ships_moor_at_both_waterlines"),
				() -> tests::shipsMoorAtBothWaterlines);
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), FloodedEnd.of("city_generation_preserves_dry_vanilla"),
				() -> tests::cityGenerationPreservesDryVanilla);
	}
}
