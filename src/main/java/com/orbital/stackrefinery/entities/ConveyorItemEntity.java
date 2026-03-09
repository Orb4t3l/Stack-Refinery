package com.orbital.stackrefinery.entities;

import com.orbital.stackrefinery.registries.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.Vec3;

public class ConveyorItemEntity extends Entity {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ConveyorItemEntity.class, EntityDataSerializers.ITEM_STACK);

    private BlockPos targetPos = BlockPos.ZERO;

    public ConveyorItemEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ConveyorItemEntity(Level level, Vec3 spawnPos, BlockPos target, ItemStack stack) {
        this(ModEntities.CONVEYOR_ITEM.get(), level);
        setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.targetPos = target;
        entityData.set(DATA_ITEM, stack.copy());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 current = position();
        Vec3 dest = Vec3.atCenterOf(targetPos).add(0, 0.5, 0);
        Vec3 diff = dest.subtract(current);
        double dist = diff.length();

        if (dist < 0.3) {
            if (!level().isClientSide()) {
                deliverItem();
                discard();
            }
            return;
        }

        double speed = Math.min(0.5, dist);
        move(MoverType.SELF, diff.normalize().scale(speed));
    }

    private void deliverItem() {
        ItemStack stack = entityData.get(DATA_ITEM).copy();
        if (stack.isEmpty()) return;

        if (!(level().getBlockEntity(targetPos) instanceof RandomizableContainerBlockEntity container)) {
            dropAsItem(stack);
            return;
        }

        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int transfer = Math.min(space, stack.getCount());
                    existing.grow(transfer);
                    container.setItem(i, existing);
                    stack.shrink(transfer);
                }
            }
        }

        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                int give = Math.min(stack.getCount(), stack.getMaxStackSize());
                container.setItem(i, stack.copyWithCount(give));
                stack.shrink(give);
            }
        }

        container.setChanged();

        if (!stack.isEmpty()) {
            dropAsItem(stack);
        }
    }

    private void dropAsItem(ItemStack stack) {
        ItemEntity ie = new ItemEntity(level(), getX(), getY(), getZ(), stack);
        level().addFreshEntity(ie);
    }

    @Override
    public void playerTouch(Player player) {}

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushedByFluid() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) { return dist < 1024; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", entityData.get(DATA_ITEM).save(new CompoundTag()));
        tag.putInt("TargetX", targetPos.getX());
        tag.putInt("TargetY", targetPos.getY());
        tag.putInt("TargetZ", targetPos.getZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_ITEM, ItemStack.of(tag.getCompound("Item")));
        targetPos = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
    }

    public ItemStack getItemStack() {
        return entityData.get(DATA_ITEM);
    }
}