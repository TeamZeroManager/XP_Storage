package com.teamzeromanager.xpstorage.network;

import com.teamzeromanager.xpstorage.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenCapsuleScreenS2CPacket {
    private final BlockPos pos;
    private final long storedPoints;

    public OpenCapsuleScreenS2CPacket(BlockPos pos, long storedPoints) {
        this.pos = pos;
        this.storedPoints = storedPoints;
    }

    public static void encode(OpenCapsuleScreenS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeLong(packet.storedPoints);
    }

    public static OpenCapsuleScreenS2CPacket decode(FriendlyByteBuf buffer) {
        return new OpenCapsuleScreenS2CPacket(buffer.readBlockPos(), buffer.readLong());
    }

    public static void handle(OpenCapsuleScreenS2CPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientPacketHandler.openCapsule(packet.pos, packet.storedPoints)));
        context.get().setPacketHandled(true);
    }
}
