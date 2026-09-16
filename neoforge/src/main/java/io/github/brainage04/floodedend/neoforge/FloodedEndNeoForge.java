package io.github.brainage04.floodedend.neoforge;

import io.github.brainage04.floodedend.FloodedEnd;
import io.github.brainage04.floodedend.command.FloodedEndCommand;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(FloodedEnd.MOD_ID)
public final class FloodedEndNeoForge {
	public FloodedEndNeoForge() {
		FloodedEnd.initialize();
		NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
	}

	private void onRegisterCommands(RegisterCommandsEvent event) {
		FloodedEndCommand.register(event.getDispatcher());
	}
}
