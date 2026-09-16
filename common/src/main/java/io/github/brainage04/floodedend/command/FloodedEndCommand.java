package io.github.brainage04.floodedend.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.brainage04.floodedend.EndOcean;
import io.github.brainage04.floodedend.EndOceanPreset;
import java.io.IOException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Server-side readback and sea-level presets for the flooded End. */
public final class FloodedEndCommand {
	public static final String COMMAND_NAME = "floodedend";

	private FloodedEndCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(COMMAND_NAME)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("status").executes(FloodedEndCommand::status))
				.then(Commands.literal("preset")
						.then(Commands.literal("list").executes(FloodedEndCommand::listPresets))
						.then(Commands.literal("apply")
								.then(Commands.argument("preset", StringArgumentType.word())
										.suggests((context, builder) -> {
											EndOceanPreset.PRESETS.forEach(preset -> builder.suggest(preset.id()));
											return builder.buildFuture();
										})
										.executes(FloodedEndCommand::applyPreset)))
						.then(Commands.literal("clear").executes(FloodedEndCommand::clearPreset)))
				.then(Commands.literal("probe")
						.executes(context -> probe(context, BlockPos.containing(context.getSource().getPosition())))
						.then(Commands.argument("x", IntegerArgumentType.integer())
								.then(Commands.argument("z", IntegerArgumentType.integer())
										.executes(context -> probe(context, new BlockPos(
												IntegerArgumentType.getInteger(context, "x"),
												0,
												IntegerArgumentType.getInteger(context, "z")
										)))))));
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel end = source.getServer().getLevel(Level.END);
		if (end == null) {
			source.sendFailure(Component.literal("The End is not loaded."));
			return 0;
		}

		EndOcean ocean = EndOcean.of(end);
		source.sendSuccess(() -> Component.literal("FloodedEnd: " + ocean.describe()), false);
		if (!ocean.flooded()) {
			source.sendFailure(Component.literal("FloodedEnd: the End is dry; the ocean world generation is not active."));
			return 0;
		}

		return 1;
	}

	private static int listPresets(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel end = source.getServer().getLevel(Level.END);
		for (EndOceanPreset preset : EndOceanPreset.PRESETS) {
			source.sendSuccess(() -> Component.literal(
					"FloodedEnd preset " + preset.id() + ": End sea level " + preset.seaLevel() + " — " + preset.description()
			), false);
		}

		source.sendSuccess(() -> Component.literal(
				"FloodedEnd: this world is running " + (EndOceanPreset.installed(source.getServer())
						? "an installed preset data pack"
						: "the shipped End ocean")
						+ (end == null ? "" : " (" + EndOcean.of(end).describe() + ")")
						+ ". Presets change world generation, so they apply to chunks generated after the next server start."
		), false);
		return EndOceanPreset.PRESETS.size();
	}

	private static int applyPreset(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String id = StringArgumentType.getString(context, "preset");
		EndOceanPreset preset = EndOceanPreset.byId(id).orElse(null);
		if (preset == null) {
			source.sendFailure(Component.literal("Unknown FloodedEnd preset '" + id + "'. Available: "
					+ String.join(", ", EndOceanPreset.PRESETS.stream().map(EndOceanPreset::id).toList())));
			return 0;
		}

		try {
			preset.install(source.getServer());
		} catch (IOException error) {
			source.sendFailure(Component.literal("Could not install the " + id + " preset: " + error.getMessage()));
			return 0;
		}

		source.sendSuccess(() -> Component.literal(
				"FloodedEnd: installed the " + id + " preset (End sea level " + preset.seaLevel() + ") as "
						+ EndOceanPreset.DIRECTORY + " in this world's data packs. Restart the server before generating "
						+ "the chunks you want the new waterline in, then run /" + COMMAND_NAME + " status."
		), true);
		return 1;
	}

	private static int clearPreset(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		try {
			EndOceanPreset.remove(source.getServer());
		} catch (IOException error) {
			source.sendFailure(Component.literal("Could not clear the FloodedEnd preset: " + error.getMessage()));
			return 0;
		}

		source.sendSuccess(() -> Component.literal(
				"FloodedEnd: cleared the preset data pack; this world generates the shipped End ocean again after the "
						+ "next server start."
		), true);
		return 1;
	}

	private static int probe(CommandContext<CommandSourceStack> context, BlockPos position) {
		CommandSourceStack source = context.getSource();
		ServerLevel end = source.getServer().getLevel(Level.END);
		if (end == null) {
			source.sendFailure(Component.literal("The End is not loaded."));
			return 0;
		}

		int x = position.getX();
		int z = position.getZ();
		if (!end.hasChunkAt(new BlockPos(x, end.getMinY(), z))) {
			source.sendFailure(Component.literal("No End chunk loaded at " + x + ", " + z + "."));
			return 0;
		}

		EndOcean.Column column = EndOcean.probe(end, x, z);
		source.sendSuccess(() -> Component.literal("FloodedEnd: " + column.describe()), false);
		return column.isFlooded() ? 1 : 0;
	}
}
