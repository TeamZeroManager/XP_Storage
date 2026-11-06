package com.teamzeromanager.xpstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.teamzeromanager.xpstorage.block.entity.XpCapsuleBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.text.NumberFormat;
import java.util.Locale;

public class XpCapsuleRenderer implements BlockEntityRenderer<XpCapsuleBlockEntity> {
    // The storage itself is unlimited. This value only defines the visual scale of the tank.
    private static final double VISUAL_CAPACITY = 25000.0D;
    private static final float MIN_Y = 7.0F / 16.0F;
    private static final float MAX_Y = 25.0F / 16.0F;
    private static final float MIN_X = 4.0F / 16.0F;
    private static final float MAX_X = 12.0F / 16.0F;
    private static final float MIN_Z = 4.0F / 16.0F;
    private static final float MAX_Z = 12.0F / 16.0F;

    public XpCapsuleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(XpCapsuleBlockEntity capsule) {
        return new AABB(capsule.getBlockPos(), capsule.getBlockPos().offset(1, 2, 1));
    }

    @Override
    public void render(XpCapsuleBlockEntity capsule, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        long stored = Math.max(0L, capsule.getStoredExperience());
        if (stored <= 0L) return;

        float amount = (float) Math.min(1.0D, stored / VISUAL_CAPACITY);
        float liquidTop = MIN_Y + (MAX_Y - MIN_Y) * amount;
        float time = (capsule.getLevel() == null ? 0.0F : capsule.getLevel().getGameTime()) + partialTick;
        float wave = Math.min(0.012F, 0.002F + amount * 0.01F);

        poseStack.pushPose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        renderLiquidSides(consumer, poseStack, liquidTop, packedLight);
        renderWavySurface(consumer, poseStack, liquidTop, time, wave, packedLight);
        poseStack.popPose();

        renderTerminalAmount(capsule, poseStack, buffers, packedLight, stored);
    }

    private static void renderLiquidSides(VertexConsumer out, PoseStack stack, float top, int light) {
        var pose = stack.last();
        float x0 = MIN_X, x1 = MAX_X, z0 = MIN_Z, z1 = MAX_Z;
        quad(out, pose, x0, MIN_Y, z0, x1, MIN_Y, z0, x1, top, z0, x0, top, z0, 0.12F, 0.95F, 0.28F, 0.88F, light);
        quad(out, pose, x1, MIN_Y, z1, x0, MIN_Y, z1, x0, top, z1, x1, top, z1, 0.08F, 0.72F, 0.20F, 0.88F, light);
        quad(out, pose, x0, MIN_Y, z1, x0, MIN_Y, z0, x0, top, z0, x0, top, z1, 0.10F, 0.82F, 0.22F, 0.88F, light);
        quad(out, pose, x1, MIN_Y, z0, x1, MIN_Y, z1, x1, top, z1, x1, top, z0, 0.10F, 0.82F, 0.22F, 0.88F, light);
        quad(out, pose, x0, MIN_Y, z1, x1, MIN_Y, z1, x1, MIN_Y, z0, x0, MIN_Y, z0, 0.04F, 0.38F, 0.12F, 0.88F, light);
    }

    private static void renderWavySurface(VertexConsumer out, PoseStack stack, float top,
                                           float time, float wave, int light) {
        var pose = stack.last();
        int segments = 10;
        for (int i = 0; i < segments; i++) {
            float z0 = Mth.lerp((float) i / segments, MIN_Z, MAX_Z);
            float z1 = Mth.lerp((float) (i + 1) / segments, MIN_Z, MAX_Z);
            float y00 = top + (float) Math.sin(time * 0.08F + i * 0.7F) * wave;
            float y01 = top + (float) Math.sin(time * 0.08F + (i + 1) * 0.7F) * wave;
            quad(out, pose, MIN_X, y00, z0, MAX_X, y00, z0,
                    MAX_X, y01, z1, MIN_X, y01, z1, 0.24F, 1.0F, 0.38F, 0.94F, light);
        }
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float r, float g, float b, float a, int light) {
        vertex(out, pose, x0, y0, z0, r, g, b, a, light);
        vertex(out, pose, x1, y1, z1, r, g, b, a, light);
        vertex(out, pose, x2, y2, z2, r, g, b, a, light);
        vertex(out, pose, x3, y3, z3, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float r, float g, float b, float a, int light) {
        out.vertex(pose.pose(), x, y, z)
                .color(r, g, b, a)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static void renderTerminalAmount(XpCapsuleBlockEntity capsule, PoseStack poseStack,
                                             MultiBufferSource buffers, int packedLight, long stored) {
        var font = Minecraft.getInstance().font;
        String amount = NumberFormat.getIntegerInstance(Locale.US).format(stored) + " XP";
        Component label = Component.literal(amount);

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.835D, 0.006D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-0.0065F, -0.0065F, 0.0065F);
        font.drawInBatch(label, -font.width(label) / 2.0F, -font.lineHeight / 2.0F,
                0x8CFF9A, false, poseStack.last().pose(), buffers,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }
}
