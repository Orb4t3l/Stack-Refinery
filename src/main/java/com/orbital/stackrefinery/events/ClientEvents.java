package com.orbital.stackrefinery.events;

import com.orbital.stackrefinery.StackRefinery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StackRefinery.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

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
        btnX = (screen.width - imageWidth) / 2 + imageWidth - 24;
        btnY = (screen.height - imageHeight) / 2 + 4;
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!chestOpen) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int mouseX = (int) Minecraft.getInstance().mouseHandler.xpos();
        int mouseY = (int) Minecraft.getInstance().mouseHandler.ypos();
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int mx = (int) (mouseX / scale);
        int my = (int) (mouseY / scale);

        boolean hovered = mx >= btnX && mx <= btnX + BTN_W && my >= btnY && my <= btnY + BTN_H;

        graphics.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, hovered ? 0xFFAAAAAA : 0xFF888888);
        graphics.renderOutline(btnX, btnY, BTN_W, BTN_H, 0xFF000000);
        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                Component.literal("C"),
                btnX + BTN_W / 2,
                btnY + (BTN_H - 8) / 2,
                0xFFFFFF
        );
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!chestOpen) return;
        if (event.getButton() != 0) return;

        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();

        if (mx >= btnX && mx <= btnX + BTN_W && my >= btnY && my <= btnY + BTN_H) {
            // consolidation logic goes here
            event.setCanceled(true);
        }
    }
}