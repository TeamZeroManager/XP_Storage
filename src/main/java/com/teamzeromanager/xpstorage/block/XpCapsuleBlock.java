package com.teamzeromanager.xpstorage.block;

import com.teamzeromanager.xpstorage.block.entity.XpCapsuleBlockEntity;
import com.teamzeromanager.xpstorage.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

public class XpCapsuleBlock extends BaseEntityBlock {
    public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, 1);

    public XpCapsuleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SECTION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(SECTION);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SECTION) == 0 ? new XpCapsuleBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos basePos = context.getClickedPos();
        Level level = context.getLevel();

        // Reserve both blocks before allowing placement.
        for (int section = 0; section < 2; section++) {
            if (!level.getBlockState(basePos.above(section)).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(SECTION, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        for (int section = 1; section < 3; section++) {
            level.setBlock(pos.above(section), defaultBlockState().setValue(SECTION, section), 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            int currentSection = state.getValue(SECTION);
            BlockPos basePos = pos.below(currentSection);

            // Removing either section dismantles the complete two-block machine.
            for (int section = 0; section < 3; section++) {
                BlockPos partPos = basePos.above(section);
                if (!partPos.equals(pos) && level.getBlockState(partPos).is(this)) {
                    level.removeBlock(partPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        BlockPos basePos = pos.below(state.getValue(SECTION));
        if (level.getBlockEntity(basePos) instanceof XpCapsuleBlockEntity capsule) {
            // Sneak + right-click opens the integrated terminal. Normal right-click deposits XP.
            if (serverPlayer.isShiftKeyDown()) {
                ModMessages.openCapsuleScreen(serverPlayer, basePos, capsule.getStoredExperience());
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
