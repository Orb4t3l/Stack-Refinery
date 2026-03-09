package com.orbital.stackrefinery.consolidation;

import com.orbital.stackrefinery.entities.ConveyorItemEntity;
import com.orbital.stackrefinery.tracking.ChestTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ConsolidationHandler {

    private static final Set<UUID> inProgress = Collections.synchronizedSet(new HashSet<>());

    public static void consolidate(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!inProgress.add(id)) return;

        try {
            List<BlockPos> positions = ChestTracker.getChestsForPlayer(id);
            if (positions.isEmpty()) positions = ChestTracker.scanImmediately(player);
            if (positions.isEmpty()) return;

            ServerLevel level = player.serverLevel();
            List<RandomizableContainerBlockEntity> containers = resolveContainers(level, positions);
            if (containers.isEmpty()) return;

            Map<String, List<StackEntry>> groups = groupItems(containers);

            for (List<StackEntry> stacks : groups.values()) {
                if (stacks.size() <= 1) continue;

                stacks.sort((a, b) -> Integer.compare(b.stack.getCount(), a.stack.getCount()));

                BlockPos destPos = stacks.get(0).container.getBlockPos();

                for (int i = 1; i < stacks.size(); i++) {
                    StackEntry src = stacks.get(i);
                    ItemStack toSend = src.stack.copy();

                    src.container.setItem(src.slot, ItemStack.EMPTY);
                    src.container.setChanged();

                    Vec3 spawnPos = Vec3.atCenterOf(src.container.getBlockPos()).add(0, 0.5, 0);
                    ConveyorItemEntity entity = new ConveyorItemEntity(level, spawnPos, destPos, toSend);
                    level.addFreshEntity(entity);
                }
            }

            ChestTracker.invalidatePlayer(id);

        } finally {
            inProgress.remove(id);
        }
    }

    private static List<RandomizableContainerBlockEntity> resolveContainers(ServerLevel level, List<BlockPos> positions) {
        List<RandomizableContainerBlockEntity> result = new ArrayList<>();
        for (BlockPos pos : positions) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) {
                result.add((RandomizableContainerBlockEntity) be);
            }
        }
        return result;
    }

    private static Map<String, List<StackEntry>> groupItems(List<RandomizableContainerBlockEntity> containers) {
        Map<String, List<StackEntry>> groups = new HashMap<>();
        for (RandomizableContainerBlockEntity container : containers) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) continue;
                groups.computeIfAbsent(itemKey(stack), k -> new ArrayList<>())
                        .add(new StackEntry(container, slot, stack.copy()));
            }
        }
        return groups;
    }

    private static String itemKey(ItemStack stack) {
        String base = stack.getItem().toString();
        return stack.hasTag() ? base + stack.getTag() : base;
    }

    private record StackEntry(RandomizableContainerBlockEntity container, int slot, ItemStack stack) {}
}