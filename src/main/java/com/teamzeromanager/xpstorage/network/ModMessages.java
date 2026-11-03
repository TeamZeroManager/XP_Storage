package com.teamzeromanager.xpstorage.network;

import com.teamzeromanager.xpstorage.XPStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModMessages {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(XPStorage.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int packetId = 0;

    private ModMessages() {}

    public static void register() {
        CHANNEL.registerMessage(packetId++, OpenCapsuleScreenS2CPacket.class,
                OpenCapsuleScreenS2CPacket::encode,
                OpenCapsuleScreenS2CPacket::decode,
                OpenCapsuleScreenS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(packetId++, WithdrawExperienceC2SPacket.class,
                WithdrawExperienceC2SPacket::encode,
                WithdrawExperienceC2SPacket::decode,
                WithdrawExperienceC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void openCapsuleScreen(ServerPlayer player, net.minecraft.core.BlockPos pos, long storedPoints) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenCapsuleScreenS2CPacket(pos, storedPoints));
    }
}
