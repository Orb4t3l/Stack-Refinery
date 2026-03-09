package com.orbital.stackrefinery.consolidation;

import com.orbital.stackrefinery.tracking.ChestTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

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

            List<RandomizableContainerBlockEntity> containers = resolveContainers(player.serverLevel(), positions);
            if (containers.isEmpty()) return;

            Map<String, List<StackEntry>> groups = groupItems(containers);

            for (List<StackEntry> stacks : groups.values()) {
                if (stacks.size() <= 1) continue;

                int maxStack = stacks.get(0).stack.getMaxStackSize();
                int total = stacks.stream().mapToInt(s -> s.stack.getCount()).sum();

                stacks.sort((a, b) -> Integer.compare(b.stack.getCount(), a.stack.getCount()));

                clearStacks(stacks);
                redistributeItems(stacks, total, maxStack);
            }

            for (RandomizableContainerBlockEntity c : containers) {
                c.setChanged();
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

    private static void clearStacks(List<StackEntry> stacks) {
        for (StackEntry e : stacks) {
            e.container.setItem(e.slot, ItemStack.EMPTY);
        }
    }

    private static void redistributeItems(List<StackEntry> stacks, int total, int maxStack) {
        int remaining = total;
        for (StackEntry e : stacks) {
            if (remaining <= 0) break;
            int give = Math.min(remaining, maxStack);
            e.container.setItem(e.slot, e.stack.copyWithCount(give));
            remaining -= give;
        }
    }

    private static String itemKey(ItemStack stack) {
        String base = stack.getItem().toString();
        return stack.hasTag() ? base + stack.getTag() : base;
    }

    private record StackEntry(RandomizableContainerBlockEntity container, int slot, ItemStack stack) {}
}