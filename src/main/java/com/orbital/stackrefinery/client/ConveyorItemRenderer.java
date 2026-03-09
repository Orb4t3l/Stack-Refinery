package com.orbital.stackrefinery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.orbital.stackrefinery.entities.ConveyorItemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ConveyorItemRenderer extends EntityRenderer<ConveyorItemEntity> {

    private final ItemRenderer itemRenderer;

    public ConveyorItemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ConveyorItemEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        ItemStack stack = entity.getItemStack();
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.0, 0.2, 0.0);

        float spin = (entity.tickCount + partialTick) * 3.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(0.4f, 0.4f, 0.4f);

        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ConveyorItemEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
}