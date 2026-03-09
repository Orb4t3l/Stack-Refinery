package com.orbital.stackrefinery.tracking;

import com.orbital.stackrefinery.StackRefinery;
import com.orbital.stackrefinery.config.RefineryConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = StackRefinery.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChestTracker {

    private static final int SCAN_INTERVAL_TICKS = 40;
    private static int tickCounter = 0;

    private static final Map<UUID, List<BlockPos>> playerChestMap = new ConcurrentHashMap<>();

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
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> found = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {

            if (origin.distSqr(pos) > (double) radius * radius) continue;

            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof ChestBlockEntity) {
                BlockPos canonical = getCanonicalChestPos(level, pos);
                if (seen.add(canonical)) {
                    found.add(canonical);
                }
            } else if (be instanceof BarrelBlockEntity) {
                BlockPos immutable = pos.immutable();
                if (seen.add(immutable)) {
                    found.add(immutable);
                }
            }
        }

        return found;
    }

    private static BlockPos getCanonicalChestPos(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return pos.immutable();

        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE) return pos.immutable();

        BlockPos otherHalf = pos.relative(ChestBlock.getConnectedDirection(state)).immutable();
        return pos.immutable().compareTo(otherHalf) < 0 ? pos.immutable() : otherHalf;
    }

    public static List<BlockPos> getChestsForPlayer(UUID playerId) {
        return playerChestMap.getOrDefault(playerId, Collections.emptyList());
    }

    public static void invalidatePlayer(UUID playerId) {
        playerChestMap.remove(playerId);
    }
}