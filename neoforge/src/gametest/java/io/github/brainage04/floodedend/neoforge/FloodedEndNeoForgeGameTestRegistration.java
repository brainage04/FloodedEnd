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
				FloodedEnd.of("flood_readback_command_is_registered"),
				() -> tests::floodReadbackCommandIsRegistered
		);
		event.register(
				BuiltInRegistries.TEST_FUNCTION.key(),
				FloodedEnd.of("end_city_is_flooded_ends"),
				() -> tests::endCityIsFloodedEnds
		);
	}
}
