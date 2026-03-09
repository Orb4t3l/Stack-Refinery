package com.orbital.stackrefinery.network;

import com.orbital.stackrefinery.consolidation.ConsolidationHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConsolidatePacket {

    public static void encode(ConsolidatePacket pkt, FriendlyByteBuf buf) {}

    public static ConsolidatePacket decode(FriendlyByteBuf buf) {
        return new ConsolidatePacket();
    }

    public static void handle(ConsolidatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ConsolidationHandler.consolidate(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}