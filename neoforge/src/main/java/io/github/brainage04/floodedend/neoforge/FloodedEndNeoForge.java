package io.github.brainage04.floodedend.neoforge;

import io.github.brainage04.floodedend.EndCityAtSea;
import io.github.brainage04.floodedend.FloodedEnd;
import io.github.brainage04.floodedend.command.FloodedEndCommand;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FloodedEnd.MOD_ID)
public final class FloodedEndNeoForge {
	private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
			DeferredRegister.create(Registries.STRUCTURE_TYPE, FloodedEnd.MOD_ID);
	private static final DeferredHolder<StructureType<?>, StructureType<EndCityAtSea>> END_CITY =
			STRUCTURE_TYPES.register(EndCityAtSea.NAME, () -> EndCityAtSea.TYPE);

	public FloodedEndNeoForge(IEventBus modBus) {
		FloodedEnd.initialize();
		STRUCTURE_TYPES.register(modBus);
		NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
	}

	private void onRegisterCommands(RegisterCommandsEvent event) {
		FloodedEndCommand.register(event.getDispatcher());
	}
}
