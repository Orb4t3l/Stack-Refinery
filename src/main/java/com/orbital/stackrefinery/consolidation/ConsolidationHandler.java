package com.orbital.stackrefinery.consolidation;

import com.orbital.stackrefinery.tracking.ChestTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsolidationHandler {

    public static void consolidate(ServerPlayer player) {
        List<BlockPos> positions = ChestTracker.getChestsForPlayer(player.getUUID());
        List<RandomizableContainerBlockEntity> containers = resolveContainers(player, positions);

        if (containers.isEmpty()) return;

        Map<String, List<StackEntry>> itemGroups = groupItems(containers);

        for (Map.Entry<String, List<StackEntry>> entry : itemGroups.entrySet()) {
            List<StackEntry> stacks = entry.getValue();
            if (stacks.size() <= 1) continue;

            int maxStack = stacks.get(0).stack.getMaxStackSize();
            int total = stacks.stream().mapToInt(s -> s.stack.getCount()).sum();

            clearStacks(stacks);
            redistributeItems(stacks, total, maxStack);
        }

        containers.forEach(c -> c.setChanged());
        ChestTracker.invalidatePlayer(player.getUUID());
    }

    private static List<RandomizableContainerBlockEntity> resolveContainers(ServerPlayer player, List<BlockPos> positions) {
        List<RandomizableContainerBlockEntity> result = new ArrayList<>();
        for (BlockPos pos : positions) {
            BlockEntity be = player.serverLevel().getBlockEntity(pos);
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

        for (StackEntry entry : stacks) {
            if (remaining <= 0) break;
            int give = Math.min(remaining, maxStack);
            ItemStack newStack = entry.stack.copyWithCount(give);
            entry.container.setItem(entry.slot, newStack);
            remaining -= give;
        }
    }

    private static String itemKey(ItemStack stack) {
        StringBuilder key = new StringBuilder();
        key.append(stack.getItem());
        if (stack.hasTag()) {
            key.append(stack.getTag().toString());
        }
        return key.toString();
    }

    private record StackEntry(RandomizableContainerBlockEntity container, int slot, ItemStack stack) {}
}