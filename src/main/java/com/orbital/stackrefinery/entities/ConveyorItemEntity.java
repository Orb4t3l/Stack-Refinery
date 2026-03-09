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

    private static final double SPEED = 0.1;
    private static final double ARRIVAL_THRESHOLD = 0.15;

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ConveyorItemEntity.class, EntityDataSerializers.ITEM_STACK);

    private BlockPos targetPos = BlockPos.ZERO;

    public ConveyorItemEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
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

        Vec3 dest = Vec3.atCenterOf(targetPos).add(0, 0.25, 0);
        Vec3 diff = dest.subtract(position());
        double dist = diff.length();

        if (dist < ARRIVAL_THRESHOLD) {
            if (!level().isClientSide()) {
                deliverItem();
                discard();
            }
            return;
        }

        double speed = Math.min(SPEED, dist);
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
                container.setItem(i, stack.copyWithCount(Math.min(stack.getCount(), stack.getMaxStackSize())));
                stack.shrink(stack.getCount());
            }
        }

        container.setChanged();

        if (!stack.isEmpty()) dropAsItem(stack);
    }

    private void dropAsItem(ItemStack stack) {
        level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), stack));
    }

    @Override public void playerTouch(Player player) {}
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 1024; }

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

    public ItemStack getItemStack() { return entityData.get(DATA_ITEM); }
    public BlockPos getTargetPos() { return targetPos; }
}