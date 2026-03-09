package com.orbital.stackrefinery.tracking;

import com.orbital.stackrefinery.StackRefinery;
import com.orbital.stackrefinery.config.RefineryConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = StackRefinery.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChestTracker {

    private static final int SCAN_INTERVAL_TICKS = 40;
    private static int tickCounter = 0;

    private static final Map<UUID, List<BlockPos>> playerChestMap = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                playerChestMap.put(player.getUUID(), doScan(level, player));
            }
        }
    }

    public static List<BlockPos> scanImmediately(ServerPlayer player) {
        List<BlockPos> result = doScan(player.serverLevel(), player);
        playerChestMap.put(player.getUUID(), result);
        return result;
    }

    private static List<BlockPos> doScan(ServerLevel level, ServerPlayer player) {
        int radius = RefineryConfig.getRadius();
        BlockPos origin = player.blockPosition();
        List<BlockPos> found = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {

            if (origin.distSqr(pos) > (double) radius * radius) continue;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) {
                found.add(pos.immutable());
            }
        }

        return found;
    }

    public static List<BlockPos> getChestsForPlayer(UUID playerId) {
        return playerChestMap.getOrDefault(playerId, Collections.emptyList());
    }

    public static void invalidatePlayer(UUID playerId) {
        playerChestMap.remove(playerId);
    }
}