package com.orbital.stackrefinery.registries;

import com.orbital.stackrefinery.StackRefinery;
import com.orbital.stackrefinery.entities.ConveyorItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StackRefinery.MODID);

    public static final RegistryObject<EntityType<ConveyorItemEntity>> CONVEYOR_ITEM =
            ENTITIES.register("conveyor_item", () -> EntityType.Builder
                    .<ConveyorItemEntity>of(ConveyorItemEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("conveyor_item"));
}