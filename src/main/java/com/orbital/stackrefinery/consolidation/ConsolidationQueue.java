package com.orbital.stackrefinery.consolidation;

import com.orbital.stackrefinery.StackRefinery;
import com.orbital.stackrefinery.entities.ConveyorItemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = StackRefinery.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConsolidationQueue {

    private static final int SPAWN_INTERVAL_TICKS = 20;

    private record PendingMove(ServerLevel level, Vec3 from, BlockPos to, ItemStack stack) {}

    private static final Map<UUID, Queue<PendingMove>> queues = new HashMap<>();
    private static final Map<UUID, Integer> ticksSinceLast = new HashMap<>();

    public static void enqueue(ServerPlayer player, Vec3 from, BlockPos to, ItemStack stack) {
        UUID id = player.getUUID();
        queues.computeIfAbsent(id, k -> new LinkedList<>())
                .add(new PendingMove(player.serverLevel(), from, to, stack));
        ticksSinceLast.putIfAbsent(id, SPAWN_INTERVAL_TICKS);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<UUID, Queue<PendingMove>>> it = queues.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Queue<PendingMove>> entry = it.next();
            UUID id = entry.getKey();
            Queue<PendingMove> queue = entry.getValue();

            int ticks = ticksSinceLast.merge(id, 1, Integer::sum);

            if (ticks < SPAWN_INTERVAL_TICKS) continue;

            ticksSinceLast.put(id, 0);

            PendingMove move = queue.poll();
            if (move == null) {
                it.remove();
                ticksSinceLast.remove(id);
                continue;
            }

            ConveyorItemEntity entity = new ConveyorItemEntity(move.level(), move.from(), move.to(), move.stack());
            move.level().addFreshEntity(entity);
        }
    }

    public static void clearPlayer(UUID id) {
        queues.remove(id);
        ticksSinceLast.remove(id);
    }
}