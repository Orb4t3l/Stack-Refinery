package com.orbital.stackrefinery.consolidation;

import com.orbital.stackrefinery.config.RefineryConfig;
import com.orbital.stackrefinery.tracking.ChestTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsolidationHandler {

    public static void consolidate(ServerPlayer player) {
        List<BlockPos> positions = ChestTracker.getChestsForPlayer(player.getUUID());

        if (positions.isEmpty()) {
            positions = ChestTracker.scanImmediately(player);
        }

        List<RandomizableContainerBlockEntity> containers = resolveContainers(player.serverLevel(), positions);
        if (containers.isEmpty()) return;

        Map<String, List<StackEntry>> itemGroups = groupItems(containers);

        for (List<StackEntry> stacks : itemGroups.values()) {
            if (stacks.size() <= 1) continue;

            int maxStack = stacks.get(0).stack.getMaxStackSize();
            int total = stacks.stream().mapToInt(s -> s.stack.getCount()).sum();

            clearStacks(stacks);
            redistributeItems(stacks, total, maxStack);
        }

        for (RandomizableContainerBlockEntity container : containers) {
            container.setChanged();
        }

        ChestTracker.invalidatePlayer(player.getUUID());
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

                String key = itemKey(stack);
                groups.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(new StackEntry(container, slot, stack.copy()));
            }
        }

        return groups;
    }

    private static void clearStacks(List<StackEntry> stacks) {
        for (StackEntry entry : stacks) {
            entry.container.setItem(entry.slot, ItemStack.EMPTY);
        }
    }

    private static void redistributeItems(List<StackEntry> stacks, int total, int maxStack) {
        int remaining = total;
        int entryIndex = 0;

        while (remaining > 0 && entryIndex < stacks.size()) {
            StackEntry entry = stacks.get(entryIndex);
            int give = Math.min(remaining, maxStack);
            entry.container.setItem(entry.slot, entry.stack.copyWithCount(give));
            remaining -= give;
            entryIndex++;
        }
    }

    private static String itemKey(ItemStack stack) {
        String base = stack.getItem().toString();
        return stack.hasTag() ? base + stack.getTag() : base;
    }

    private record StackEntry(RandomizableContainerBlockEntity container, int slot, ItemStack stack) {}
}