package com.teamzeromanager.xpstorage.network;

import com.teamzeromanager.xpstorage.block.entity.XpCapsuleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WithdrawExperienceC2SPacket {
    private final BlockPos pos;
    private final int levels;

    public WithdrawExperienceC2SPacket(BlockPos pos, int levels) {
        this.pos = pos;
        this.levels = levels;
    }

    public static void encode(WithdrawExperienceC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.levels);
    }

    public static WithdrawExperienceC2SPacket decode(FriendlyByteBuf buffer) {
        return new WithdrawExperienceC2SPacket(buffer.readBlockPos(), buffer.readVarInt());
    }

    public static void handle(WithdrawExperienceC2SPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;

            if (packet.levels <= 0 || packet.levels > 100000
                    || player.distanceToSqr(packet.pos.getX() + 0.5D,
                    packet.pos.getY() + 0.5D, packet.pos.getZ() + 0.5D) > 64.0D) {
                return;
            }

            if (!(player.level().getBlockEntity(packet.pos) instanceof XpCapsuleBlockEntity capsule)) {
                return;
            }

            int maximum = XpCapsuleBlockEntity.getMaxWithdrawableLevels(
                    player, capsule.getStoredExperience());
            if (packet.levels > maximum) {
                player.displayClientMessage(
                        Component.literal("No hay suficiente XP. Máximo disponible: " + maximum + " niveles."),
                        true);
                ModMessages.openCapsuleScreen(player, packet.pos, capsule.getStoredExperience());
                return;
            }

            long cost = XpCapsuleBlockEntity.getCostForLevels(player, packet.levels);
            if (cost <= 0L || cost > Integer.MAX_VALUE || !capsule.withdraw(cost)) {
                player.displayClientMessage(Component.literal("No se pudo completar la extracción."), true);
                ModMessages.openCapsuleScreen(player, packet.pos, capsule.getStoredExperience());
                return;
            }

            XpCapsuleBlockEntity.grantExperiencePointsExactly(player, (int) cost);
            player.displayClientMessage(
                    Component.literal("Retiraste " + packet.levels + " niveles (" + cost + " puntos de XP)."),
                    true);
            ModMessages.openCapsuleScreen(player, packet.pos, capsule.getStoredExperience());
        });
        context.get().setPacketHandled(true);
    }
}
