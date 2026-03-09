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

    private static final int TICKS_BETWEEN_TYPES = 20;
    private static final int TICKS_PER_32_ITEMS = 10;

    public record PendingMove(ServerLevel level, Vec3 from, BlockPos to, ItemStack stack) {}

    private static final Map<UUID, Queue<List<PendingMove>>> typeQueue = new HashMap<>();
    private static final Map<UUID, Queue<PendingMove>> activeGroup = new HashMap<>();
    private static final Map<UUID, Integer> tickCounter = new HashMap<>();
    private static final Map<UUID, Integer> ticksNeeded = new HashMap<>();
    private static final Map<UUID, Boolean> waitingBetweenTypes = new HashMap<>();

    public static void enqueueGroup(ServerPlayer player, List<PendingMove> group) {
        UUID id = player.getUUID();
        typeQueue.computeIfAbsent(id, k -> new LinkedList<>()).add(group);
        tickCounter.putIfAbsent(id, TICKS_BETWEEN_TYPES);
        ticksNeeded.putIfAbsent(id, TICKS_BETWEEN_TYPES);
        waitingBetweenTypes.putIfAbsent(id, false);
    }

    public static PendingMove makePending(ServerPlayer player, Vec3 from, BlockPos to, ItemStack stack) {
        return new PendingMove(player.serverLevel(), from, to, stack);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<UUID, Queue<List<PendingMove>>>> it = typeQueue.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Queue<List<PendingMove>>> entry = it.next();
            UUID id = entry.getKey();
            Queue<List<PendingMove>> types = entry.getValue();

            int ticks = tickCounter.merge(id, 1, Integer::sum);
            int needed = ticksNeeded.getOrDefault(id, TICKS_BETWEEN_TYPES);

            if (ticks < needed) continue;
            tickCounter.put(id, 0);

            Queue<PendingMove> current = activeGroup.get(id);

            if (current == null || current.isEmpty()) {
                List<PendingMove> nextGroup = types.poll();
                if (nextGroup == null) {
                    it.remove();
                    activeGroup.remove(id);
                    tickCounter.remove(id);
                    ticksNeeded.remove(id);
                    waitingBetweenTypes.remove(id);
                    continue;
                }
                current = new LinkedList<>(nextGroup);
                activeGroup.put(id, current);
                ticksNeeded.put(id, TICKS_BETWEEN_TYPES);
            }

            PendingMove move = current.poll();
            if (move == null) continue;

            ConveyorItemEntity entity = new ConveyorItemEntity(move.level(), move.from(), move.to(), move.stack());
            move.level().addFreshEntity(entity);

            int stackSize = move.stack().getCount();
            int delay = Math.max(1, (stackSize / 32) * TICKS_PER_32_ITEMS);
            ticksNeeded.put(id, delay);
        }
    }

    public static void clearPlayer(UUID id) {
        typeQueue.remove(id);
        activeGroup.remove(id);
        tickCounter.remove(id);
        ticksNeeded.remove(id);
        waitingBetweenTypes.remove(id);
    }
}