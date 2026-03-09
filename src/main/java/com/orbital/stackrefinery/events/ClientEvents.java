package com.orbital.stackrefinery.events;

import com.orbital.stackrefinery.StackRefinery;
import com.orbital.stackrefinery.network.ConsolidatePacket;
import com.orbital.stackrefinery.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = StackRefinery.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    private static final ResourceLocation BUTTON_TEXTURE =
            new ResourceLocation(StackRefinery.MODID, "textures/gui/consolidate_button.png");

    private static int btnX, btnY;
    private static final int BTN_W = 20;
    private static final int BTN_H = 20;
    private static boolean chestOpen = false;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        chestOpen = false;

        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (!(containerScreen.getMenu() instanceof ChestMenu chestMenu)) return;

        chestOpen = true;
        int rows = chestMenu.getRowCount();
        int imageWidth = 176;
        int imageHeight = 114 + rows * 18;
        int leftPos = (screen.width - imageWidth) / 2;
        int topPos = (screen.height - imageHeight) / 2;

        btnX = leftPos - BTN_W - 4;
        btnY = topPos + (imageHeight / 2) - (BTN_H / 2);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!chestOpen) return;

        GuiGraphics graphics = event.getGuiGraphics();
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int mx = (int) (Minecraft.getInstance().mouseHandler.xpos() / scale);
        int my = (int) (Minecraft.getInstance().mouseHandler.ypos() / scale);

        boolean hovered = mx >= btnX && mx <= btnX + BTN_W && my >= btnY && my <= btnY + BTN_H;

        if (hovered) {
            graphics.fill(btnX - 1, btnY - 1, btnX + BTN_W + 1, btnY + BTN_H + 1, 0xFFFFFFAA);
        }

        graphics.blit(BUTTON_TEXTURE, btnX, btnY, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!chestOpen) return;
        if (event.getButton() != 0) return;

        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();

        if (mx >= btnX && mx <= btnX + BTN_W && my >= btnY && my <= btnY + BTN_H) {
            Minecraft mc = Minecraft.getInstance();
            mc.level.playLocalSound(
                    mc.player.blockPosition(),
                    SoundEvents.UI_BUTTON_CLICK.get(),
                    SoundSource.MASTER,
                    1.0f, 1.0f, false
            );
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new ConsolidatePacket());
            event.setCanceled(true);
        }
    }
}