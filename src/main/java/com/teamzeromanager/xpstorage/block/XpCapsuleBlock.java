package com.teamzeromanager.xpstorage.block;

import com.teamzeromanager.xpstorage.block.entity.XpCapsuleBlockEntity;
import com.teamzeromanager.xpstorage.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

public class XpCapsuleBlock extends BaseEntityBlock {
    public XpCapsuleBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XpCapsuleBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (level.getBlockEntity(pos) instanceof XpCapsuleBlockEntity capsule) {
            // Sneak + right-click opens the integrated terminal. Normal right-click deposits XP.
            if (serverPlayer.isShiftKeyDown()) {
                ModMessages.openCapsuleScreen(serverPlayer, pos, capsule.getStoredExperience());
                return InteractionResult.CONSUME;
            }

            long points = XpCapsuleBlockEntity.getPlayerExperiencePoints(serverPlayer);
            if (points <= 0L) {
                serverPlayer.displayClientMessage(
                        Component.literal("No tienes experiencia para almacenar. Agáchate y haz clic derecho para abrir la computadora."),
                        true
                );
                return InteractionResult.CONSUME;
            }

            capsule.deposit(points);
            serverPlayer.experienceLevel = 0;
            serverPlayer.experienceProgress = 0.0F;
            serverPlayer.totalExperience = 0;
            serverPlayer.connection.send(new ClientboundSetExperiencePacket(0.0F, 0, 0));

            String deposited = NumberFormat.getIntegerInstance(Locale.US).format(points);
            String total = NumberFormat.getIntegerInstance(Locale.US)
                    .format(capsule.getStoredExperience());
            serverPlayer.displayClientMessage(
                    Component.literal("Almacenaste " + deposited + " puntos. Total en cápsula: " + total + " puntos."),
                    true
            );
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
