package com.teamzeromanager.xpstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.teamzeromanager.xpstorage.block.entity.XpCapsuleBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public class XpCapsuleRenderer implements BlockEntityRenderer<XpCapsuleBlockEntity> {
    // Storage is unlimited; this only controls the visible fill scale.
    private static final double VISUAL_CAPACITY = 25000.0D;
    private static final float MIN_Y = 7.0F / 16.0F;
    private static final float MAX_Y = 25.0F / 16.0F;
    private static final float MIN_X = 4.0F / 16.0F;
    private static final float MAX_X = 12.0F / 16.0F;
    private static final float MIN_Z = 4.0F / 16.0F;
    private static final float MAX_Z = 12.0F / 16.0F;

    // Smooths abrupt server updates so the liquid appears to rise and fall naturally.
    private final Map<XpCapsuleBlockEntity, Float> displayedFill = new WeakHashMap<>();

    public XpCapsuleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(XpCapsuleBlockEntity capsule, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        long stored = Math.max(0L, capsule.getStoredExperience());
        float targetFill = (float) Math.min(1.0D, stored / VISUAL_CAPACITY);
        float fill = displayedFill.compute(capsule, (key, previous) ->
                previous == null ? targetFill : Mth.lerp(0.16F, previous, targetFill));
        if (Math.abs(fill - targetFill) < 0.001F) {
            fill = targetFill;
            displayedFill.put(capsule, fill);
        }

        if (fill > 0.002F) {
            float liquidTop = MIN_Y + (MAX_Y - MIN_Y) * fill;
            float time = (capsule.getLevel() == null ? 0.0F : capsule.getLevel().getGameTime()) + partialTick;
            float wave = Math.min(0.012F, 0.002F + fill * 0.01F);

            poseStack.pushPose();
            VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
            renderLiquidSides(consumer, poseStack, liquidTop, packedLight);
            renderInnerHighlights(consumer, poseStack, liquidTop, packedLight);
            renderBubbles(consumer, poseStack, liquidTop, time);
            renderWavySurface(consumer, poseStack, liquidTop, time, wave, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        // Keep the readout visible even when the capsule is empty.
        renderTerminalAmount(poseStack, buffers, stored);
    }

    private static void renderLiquidSides(VertexConsumer out, PoseStack stack, float top, int light) {
        var pose = stack.last();
        float x0 = MIN_X, x1 = MAX_X, z0 = MIN_Z, z1 = MAX_Z;
        quad(out, pose, x0, MIN_Y, z0, x1, MIN_Y, z0, x1, top, z0, x0, top, z0,
                0.08F, 0.78F, 0.19F, 0.82F, light);
        quad(out, pose, x1, MIN_Y, z1, x0, MIN_Y, z1, x0, top, z1, x1, top, z1,
                0.05F, 0.58F, 0.14F, 0.82F, light);
        quad(out, pose, x0, MIN_Y, z1, x0, MIN_Y, z0, x0, top, z0, x0, top, z1,
                0.07F, 0.68F, 0.17F, 0.82F, light);
        quad(out, pose, x1, MIN_Y, z0, x1, MIN_Y, z1, x1, top, z1, x1, top, z0,
                0.10F, 0.88F, 0.22F, 0.82F, light);
        quad(out, pose, x0, MIN_Y, z1, x1, MIN_Y, z1, x1, MIN_Y, z0, x0, MIN_Y, z0,
                0.03F, 0.30F, 0.08F, 0.9F, light);
    }

    /** Thin luminous streaks make the green fluid read clearly through the glass. */
    private static void renderInnerHighlights(VertexConsumer out, PoseStack stack, float top, int light) {
        var pose = stack.last();
        float low = MIN_Y + 0.025F;
        float width = 0.012F;
        quad(out, pose, MIN_X + 0.025F, low, MIN_Z + 0.025F,
                MIN_X + 0.025F + width, low, MIN_Z + 0.025F,
                MIN_X + 0.025F + width, top, MIN_Z + 0.025F,
                MIN_X + 0.025F, top, MIN_Z + 0.025F,
                0.55F, 1.0F, 0.58F, 0.28F, light);
        quad(out, pose, MAX_X - 0.025F, low, MAX_Z - 0.025F,
                MAX_X - 0.025F - width, low, MAX_Z - 0.025F,
                MAX_X - 0.025F - width, top, MAX_Z - 0.025F,
                MAX_X - 0.025F, top, MAX_Z - 0.025F,
                0.55F, 1.0F, 0.58F, 0.22F, light);
    }

    /** Small rising bubbles are deterministic, so they animate without random flicker. */
    private static void renderBubbles(VertexConsumer out, PoseStack stack, float top, float time) {
        var pose = stack.last();
        float usableHeight = Math.max(0.02F, top - MIN_Y - 0.06F);
        for (int i = 0; i < 5; i++) {
            float speed = 0.018F + (i % 3) * 0.006F;
            float phase = i * 1.73F;
            float travel = (time * speed + phase) % 1.0F;
            float y = MIN_Y + 0.035F + usableHeight * travel;
            float x = 0.39F + (i % 3) * 0.105F + (float) Math.sin(time * 0.035F + phase) * 0.025F;
            float z = 0.39F + ((i * 7) % 4) * 0.065F;
            float radius = 0.009F + (i % 2) * 0.004F;
            float alpha = 0.48F + (float) Math.sin(time * 0.08F + phase) * 0.16F;

            // Two crossed translucent quads give each bubble a soft, rounded glint.
            quad(out, pose, x - radius, y - radius, z, x + radius, y - radius, z,
                    x + radius, y + radius, z, x - radius, y + radius, z,
                    0.70F, 1.0F, 0.76F, alpha, LightTexture.FULL_BRIGHT);
            quad(out, pose, x, y - radius, z - radius, x, y - radius, z + radius,
                    x, y + radius, z + radius, x, y + radius, z - radius,
                    0.42F, 1.0F, 0.55F, alpha * 0.72F, LightTexture.FULL_BRIGHT);
        }
    }

    private static void renderWavySurface(VertexConsumer out, PoseStack stack, float top,
                                           float time, float wave, int light) {
        var pose = stack.last();
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            float z0 = Mth.lerp((float) i / segments, MIN_Z, MAX_Z);
            float z1 = Mth.lerp((float) (i + 1) / segments, MIN_Z, MAX_Z);
            float y00 = top + (float) Math.sin(time * 0.08F + i * 0.7F) * wave;
            float y01 = top + (float) Math.sin(time * 0.08F + (i + 1) * 0.7F) * wave;
            quad(out, pose, MIN_X, y00, z0, MAX_X, y00, z0,
                    MAX_X, y01, z1, MIN_X, y01, z1,
                    0.30F, 1.0F, 0.40F, 0.96F, light);
        }
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float r, float g, float b, float a, int light) {
        float ax = x1 - x0, ay = y1 - y0, az = z1 - z0;
        float bx = x2 - x0, by = y2 - y0, bz = z2 - z0;
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.00001F) {
            nx /= length;
            ny /= length;
            nz /= length;
        } else {
            ny = 1.0F;
        }
        vertex(out, pose, x0, y0, z0, r, g, b, a, light, nx, ny, nz);
        vertex(out, pose, x1, y1, z1, r, g, b, a, light, nx, ny, nz);
        vertex(out, pose, x2, y2, z2, r, g, b, a, light, nx, ny, nz);
        vertex(out, pose, x3, y3, z3, r, g, b, a, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float r, float g, float b, float a, int light,
                               float nx, float ny, float nz) {
        out.vertex(pose.pose(), x, y, z)
                .color(r, g, b, a)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }

    private static void renderTerminalAmount(PoseStack poseStack, MultiBufferSource buffers, long stored) {
        var font = Minecraft.getInstance().font;
        String amount = NumberFormat.getIntegerInstance(Locale.US).format(stored) + " XP";
        Component label = Component.literal(amount);
        Component subLabel = Component.literal("STORED");

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.835D, 0.006D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-0.0065F, -0.0065F, 0.0065F);
        int color = stored > 0L ? 0x8CFF9A : 0xA9B6B0;
        font.drawInBatch(label, -font.width(label) / 2.0F, -font.lineHeight / 2.0F - 1.0F,
                color, false, poseStack.last().pose(), buffers,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        font.drawInBatch(subLabel, -font.width(subLabel) / 2.0F, font.lineHeight / 2.0F - 1.0F,
                0xC5D5C9, false, poseStack.last().pose(), buffers,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
