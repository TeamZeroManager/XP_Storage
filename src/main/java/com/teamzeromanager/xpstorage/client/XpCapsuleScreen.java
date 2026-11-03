package com.teamzeromanager.xpstorage.client;

import com.teamzeromanager.xpstorage.block.entity.XpCapsuleBlockEntity;
import com.teamzeromanager.xpstorage.network.ModMessages;
import com.teamzeromanager.xpstorage.network.WithdrawExperienceC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Locale;

public class XpCapsuleScreen extends Screen {
    private final BlockPos capsulePos;
    private final long storedPoints;
    private EditBox levelsInput;
    private Button withdrawButton;

    public XpCapsuleScreen(BlockPos capsulePos, long storedPoints) {
        super(Component.literal("Terminal de contención de XP"));
        this.capsulePos = capsulePos;
        this.storedPoints = Math.max(0L, storedPoints);
    }

    @Override
    protected void init() {
        int panelLeft = (this.width - 270) / 2;
        int panelTop = (this.height - 190) / 2;

        levelsInput = new EditBox(this.font, panelLeft + 25, panelTop + 112, 100, 20,
                Component.literal("Niveles"));
        levelsInput.setValue("1");
        levelsInput.setMaxLength(7);
        levelsInput.setFilter(value -> value.matches("\\d*"));
        addRenderableWidget(levelsInput);

        withdrawButton = Button.builder(Component.literal("Retirar XP"), button -> withdraw())
                .bounds(panelLeft + 140, panelTop + 111, 105, 22)
                .build();
        addRenderableWidget(withdrawButton);

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), button -> onClose())
                .bounds(panelLeft + 85, panelTop + 151, 100, 20)
                .build());
    }

    private void withdraw() {
        if (minecraft == null || minecraft.player == null) return;
        int levels = parseRequestedLevels();
        int maximum = XpCapsuleBlockEntity.getMaxWithdrawableLevels(
                minecraft.player, storedPoints);
        if (levels <= 0 || levels > maximum) return;
        ModMessages.CHANNEL.sendToServer(new WithdrawExperienceC2SPacket(capsulePos, levels));
    }

    private int parseRequestedLevels() {
        try {
            return Integer.parseInt(levelsInput.getValue());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String formatted(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int left = (this.width - 270) / 2;
        int top = (this.height - 190) / 2;
        graphics.fill(left, top, left + 270, top + 190, 0xEE101A22);
        graphics.fill(left + 2, top + 2, left + 268, top + 188, 0xFF172832);
        graphics.fill(left + 8, top + 8, left + 262, top + 34, 0xFF203D43);

        graphics.drawCenteredString(font, title, this.width / 2, top + 17, 0xFF8CFF9A);
        graphics.drawString(font, "XP almacenada: " + formatted(storedPoints),
                left + 18, top + 48, 0xFFFFFFFF);
        graphics.drawString(font, "Nivel equivalente: "
                        + XpCapsuleBlockEntity.getEquivalentLevel(storedPoints),
                left + 18, top + 65, 0xFFB8D9E0);

        int maximum = minecraft != null && minecraft.player != null
                ? XpCapsuleBlockEntity.getMaxWithdrawableLevels(minecraft.player, storedPoints) : 0;
        graphics.drawString(font, "Niveles que deseas retirar:", left + 18, top + 96, 0xFFFFFFFF);
        graphics.drawString(font, "Máximo disponible: " + maximum,
                left + 18, top + 139, 0xFF8CFF9A);

        int requested = parseRequestedLevels();
        withdrawButton.active = requested > 0 && requested <= maximum;

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
