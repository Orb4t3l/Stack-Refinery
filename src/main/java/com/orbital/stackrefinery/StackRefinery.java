package com.orbital.stackrefinery;

import com.orbital.stackrefinery.client.ConveyorItemRenderer;
import com.orbital.stackrefinery.network.ModNetwork;
import com.orbital.stackrefinery.registries.ModEntities;
import com.orbital.stackrefinery.registries.ModMenus;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StackRefinery.MODID)
public class StackRefinery {

    public static final String MODID = "stackrefinery";

    public StackRefinery(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModMenus.MENUS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() ->
                    EntityRenderers.register(ModEntities.CONVEYOR_ITEM.get(), ConveyorItemRenderer::new)
            );
        }
    }
}