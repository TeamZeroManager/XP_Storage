package com.teamzeromanager.xpstorage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void openCapsule(BlockPos pos, long storedPoints) {
        Minecraft.getInstance().setScreen(new XpCapsuleScreen(pos, storedPoints));
    }
}
