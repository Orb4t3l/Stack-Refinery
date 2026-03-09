package com.orbital.stackrefinery.entities;

import com.orbital.stackrefinery.registries.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.Vec3;

public class ConveyorItemEntity extends Entity {

    private static final float SPEED_BLOCKS_PER_TICK = 0.15f;

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ConveyorItemEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_TOTAL_TICKS =
            SynchedEntityData.defineId(ConveyorItemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_START_X =
            SynchedEntityData.defineId(ConveyorItemEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Y =
            SynchedEntityData.defineId(ConveyorItemEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Z =
            SynchedEntityData.defineId(ConveyorItemEntity.class, EntityDataSerializers.FLOAT);

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
        entityData.set(DATA_START_X, (float) spawnPos.x);
        entityData.set(DATA_START_Y, (float) spawnPos.y);
        entityData.set(DATA_START_Z, (float) spawnPos.z);

        Vec3 dest = Vec3.atCenterOf(target).add(0, 0.25, 0);
        float dist = (float) spawnPos.distanceTo(dest);
        int ticks = Math.max(1, (int) (dist / SPEED_BLOCKS_PER_TICK));
        entityData.set(DATA_TOTAL_TICKS, ticks);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
        entityData.define(DATA_TOTAL_TICKS, 20);
        entityData.define(DATA_START_X, 0f);
        entityData.define(DATA_START_Y, 0f);
        entityData.define(DATA_START_Z, 0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        if (tickCount >= entityData.get(DATA_TOTAL_TICKS)) {
            deliverItem();
            discard();
        }
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

    public float getLerpedX(float partialTick) {
        float t = getProgress(partialTick);
        return Mth.lerp(t, entityData.get(DATA_START_X), (float) Vec3.atCenterOf(targetPos).x);
    }

    public float getLerpedY(float partialTick) {
        float t = getProgress(partialTick);
        return Mth.lerp(t, entityData.get(DATA_START_Y), (float) Vec3.atCenterOf(targetPos).add(0, 0.25, 0).y);
    }

    public float getLerpedZ(float partialTick) {
        float t = getProgress(partialTick);
        return Mth.lerp(t, entityData.get(DATA_START_Z), (float) Vec3.atCenterOf(targetPos).z);
    }

    private float getProgress(float partialTick) {
        int total = entityData.get(DATA_TOTAL_TICKS);
        if (total <= 0) return 1f;
        return Mth.clamp((tickCount + partialTick) / (float) total, 0f, 1f);
    }

    @Override public void playerTouch(Player player) {}
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 4096; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", entityData.get(DATA_ITEM).save(new CompoundTag()));
        tag.putInt("TargetX", targetPos.getX());
        tag.putInt("TargetY", targetPos.getY());
        tag.putInt("TargetZ", targetPos.getZ());
        tag.putInt("TotalTicks", entityData.get(DATA_TOTAL_TICKS));
        tag.putFloat("StartX", entityData.get(DATA_START_X));
        tag.putFloat("StartY", entityData.get(DATA_START_Y));
        tag.putFloat("StartZ", entityData.get(DATA_START_Z));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_ITEM, ItemStack.of(tag.getCompound("Item")));
        targetPos = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        entityData.set(DATA_TOTAL_TICKS, tag.getInt("TotalTicks"));
        entityData.set(DATA_START_X, tag.getFloat("StartX"));
        entityData.set(DATA_START_Y, tag.getFloat("StartY"));
        entityData.set(DATA_START_Z, tag.getFloat("StartZ"));
    }

    public ItemStack getItemStack() { return entityData.get(DATA_ITEM); }
}