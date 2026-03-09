package com.orbital.stackrefinery;

import com.orbital.stackrefinery.network.ModNetwork;
import com.orbital.stackrefinery.registries.ModMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StackRefinery.MODID)
public class StackRefinery {

    public static final String MODID = "stackrefinery";

    public StackRefinery(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}