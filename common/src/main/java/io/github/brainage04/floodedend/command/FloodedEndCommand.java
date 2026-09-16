package io.github.brainage04.floodedend.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.brainage04.floodedend.EndOcean;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Server-side readback for the flooded End: what the End actually generates, and what a column actually holds. */
public final class FloodedEndCommand {
	public static final String COMMAND_NAME = "floodedend";

	private FloodedEndCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(COMMAND_NAME)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("status").executes(FloodedEndCommand::status))
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
