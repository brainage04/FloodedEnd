package io.github.brainage04.floodedend.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.floodedend.EndOcean;
import io.github.brainage04.floodedend.EndOceanPreset;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class FloodedEndCommand {
    public static final String COMMAND_NAME = "floodedend";
    private static final String DIMENSION = "dimension";

    private FloodedEndCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(COMMAND_NAME)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(statusCommand())
                        .then(presetCommand())
                        .then(probeCommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> statusCommand() {
        return Commands.literal("status")
                .executes(context -> status(context, end(context)))
                .then(
                        Commands.argument(DIMENSION, DimensionArgument.dimension())
                                .executes(context -> status(context, dimension(context))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> presetCommand() {
        return Commands.literal("preset")
                .then(Commands.literal("list").executes(FloodedEndCommand::listPresets))
                .then(
                        Commands.literal("apply")
                                .then(
                                        Commands.argument("preset", StringArgumentType.word())
                                                .suggests(
                                                        (context, builder) -> {
                                                            EndOceanPreset.PRESETS.forEach(
                                                                    preset ->
                                                                            builder.suggest(
                                                                                    preset.id()));
                                                            return builder.buildFuture();
                                                        })
                                                .executes(FloodedEndCommand::applyPreset)))
                .then(Commands.literal("clear").executes(FloodedEndCommand::clearPreset));
    }

    /**
     * Without coordinates, probes where the source stands, in its own dimension. With coordinates,
     * probes the End unless a dimension is given.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> probeCommand() {
        return Commands.literal("probe")
                .executes(
                        context ->
                                probe(
                                        context,
                                        context.getSource().getLevel(),
                                        BlockPos.containing(context.getSource().getPosition())))
                .then(
                        Commands.argument("x", IntegerArgumentType.integer())
                                .then(
                                        Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(
                                                        context ->
                                                                probe(
                                                                        context,
                                                                        end(context),
                                                                        coordinates(context)))
                                                .then(
                                                        Commands.argument(
                                                                        DIMENSION,
                                                                        DimensionArgument
                                                                                .dimension())
                                                                .executes(
                                                                        context ->
                                                                                probe(
                                                                                        context,
                                                                                        dimension(
                                                                                                context),
                                                                                        coordinates(
                                                                                                context))))));
    }

    private static @Nullable ServerLevel end(CommandContext<CommandSourceStack> context) {
        return context.getSource().getServer().getLevel(Level.END);
    }

    private static ServerLevel dimension(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        return DimensionArgument.getDimension(context, DIMENSION);
    }

    private static BlockPos coordinates(CommandContext<CommandSourceStack> context) {
        return new BlockPos(
                IntegerArgumentType.getInteger(context, "x"),
                0,
                IntegerArgumentType.getInteger(context, "z"));
    }

    private static String name(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    private static int status(
            CommandContext<CommandSourceStack> context, @Nullable ServerLevel level) {
        CommandSourceStack source = context.getSource();
        if (level == null) {
            source.sendFailure(Component.literal("The End is not loaded."));
            return 0;
        }

        if (!EndOcean.generatesFromEndSettings(level.getChunkSource().getGenerator())) {
            source.sendFailure(
                    Component.literal(
                            "FloodedEnd: "
                                    + name(level)
                                    + " does not generate from the minecraft:end noise settings, so FloodedEnd leaves it alone."));
            return 0;
        }

        EndOcean ocean = EndOcean.of(level);
        source.sendSuccess(
                () -> Component.literal("FloodedEnd: " + name(level) + ": " + ocean.describe()),
                false);
        if (!ocean.flooded()) {
            source.sendFailure(
                    Component.literal(
                            "FloodedEnd: "
                                    + name(level)
                                    + " is dry; the ocean world generation is not active."));
            return 0;
        }

        return 1;
    }

    private static int listPresets(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel end = end(context);
        for (EndOceanPreset preset : EndOceanPreset.PRESETS) {
            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "FloodedEnd preset "
                                            + preset.id()
                                            + ": End sea level "
                                            + preset.seaLevel()
                                            + " – "
                                            + preset.description()),
                    false);
        }

        String state;
        if (!EndOceanPreset.installed(source.getServer())) {
            state = "not installed";
        } else if (EndOceanPreset.disabled(source.getServer())) {
            state = "installed but disabled";
        } else {
            state = "installed";
        }
        source.sendSuccess(
                () ->
                        Component.literal(
                                "FloodedEnd: this world's preset data pack is "
                                        + state
                                        + (end == null
                                                ? ""
                                                : " (" + EndOcean.of(end).describe() + ")")
                                        + ". Presets change world generation, so they apply to chunks generated after the next server start."),
                false);
        warnIfDisabled(source);
        return EndOceanPreset.PRESETS.size();
    }

    private static void warnIfDisabled(CommandSourceStack source) {
        if (EndOceanPreset.installed(source.getServer())
                && EndOceanPreset.disabled(source.getServer())) {
            source.sendFailure(
                    Component.literal(
                            "FloodedEnd: the preset data pack is disabled in this world and will not load. Run "
                                    + "/datapack enable \""
                                    + EndOceanPreset.PACK_ID
                                    + "\", then restart the server."));
        }
    }

    private static int applyPreset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "preset");
        EndOceanPreset preset = EndOceanPreset.byId(id).orElse(null);
        if (preset == null) {
            source.sendFailure(
                    Component.literal(
                            "Unknown FloodedEnd preset '"
                                    + id
                                    + "'. Available: "
                                    + String.join(
                                            ", ",
                                            EndOceanPreset.PRESETS.stream()
                                                    .map(EndOceanPreset::id)
                                                    .toList())));
            return 0;
        }

        try {
            preset.install(source.getServer());
        } catch (IOException error) {
            source.sendFailure(
                    Component.literal(
                            "Could not install the " + id + " preset: " + error.getMessage()));
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "FloodedEnd: installed the "
                                        + id
                                        + " preset (End sea level "
                                        + preset.seaLevel()
                                        + ") as "
                                        + EndOceanPreset.DIRECTORY
                                        + " in this world's data packs. Restart the server before generating "
                                        + "the chunks you want the new waterline in, then run /"
                                        + COMMAND_NAME
                                        + " status."),
                true);
        warnIfDisabled(source);
        return 1;
    }

    private static int clearPreset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            if (!EndOceanPreset.remove(source.getServer())) {
                source.sendFailure(Component.literal("No FloodedEnd preset is installed."));
                return 0;
            }
        } catch (IOException error) {
            source.sendFailure(
                    Component.literal(
                            "Could not clear the FloodedEnd preset: " + error.getMessage()));
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "FloodedEnd: cleared the preset data pack; this world generates the shipped End ocean again after the "
                                        + "next server start."),
                true);
        return 1;
    }

    private static int probe(
            CommandContext<CommandSourceStack> context,
            @Nullable ServerLevel level,
            BlockPos position) {
        CommandSourceStack source = context.getSource();
        if (level == null) {
            source.sendFailure(Component.literal("The End is not loaded."));
            return 0;
        }

        int x = position.getX();
        int z = position.getZ();
        if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
            source.sendFailure(
                    Component.literal(
                            "No " + name(level) + " chunk loaded at " + x + ", " + z + "."));
            return 0;
        }

        EndOcean.Column column = EndOcean.probe(level, x, z);
        source.sendSuccess(
                () -> Component.literal("FloodedEnd: " + name(level) + " " + column.describe()),
                false);
        return column.isFlooded() ? 1 : 0;
    }
}
