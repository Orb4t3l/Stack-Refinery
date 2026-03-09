package com.orbital.stackrefinery.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.orbital.stackrefinery.config.RefineryConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.orbital.stackrefinery.StackRefinery;

@Mod.EventBusSubscriber(modid = StackRefinery.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StackRefineryCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("stackrefinery")
                        .then(Commands.literal("config")
                                .then(Commands.literal("radius")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> {
                                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                                    RefineryConfig.setRadius(value);
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("Stack Refinery consolidation radius set to " + value),
                                                            true
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }
}