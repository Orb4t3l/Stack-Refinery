package com.orbital.stackrefinery.network;

import com.orbital.stackrefinery.StackRefinery;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StackRefinery.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, ConsolidatePacket.class,
                ConsolidatePacket::encode,
                ConsolidatePacket::decode,
                ConsolidatePacket::handle
        );
    }
}