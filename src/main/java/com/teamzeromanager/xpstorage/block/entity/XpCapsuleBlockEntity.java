package com.teamzeromanager.xpstorage.block.entity;

import com.teamzeromanager.xpstorage.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class XpCapsuleBlockEntity extends BlockEntity {
    private long storedExperience;
    public XpCapsuleBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.XP_CAPSULE.get(), pos, state); }
    public long getStoredExperience() { return storedExperience; }

    public void deposit(long points) {
        if (points <= 0L) return;
        storedExperience = storedExperience > Long.MAX_VALUE - points ? Long.MAX_VALUE : storedExperience + points;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static long getPlayerExperiencePoints(Player player) {
        int currentLevel = Math.max(0, player.experienceLevel);
        long base;
        if (currentLevel <= 16) base = (long) currentLevel * currentLevel + 6L * currentLevel;
        else if (currentLevel <= 31) base = (long) Math.floor(2.5D * currentLevel * currentLevel - 40.5D * currentLevel + 360.0D);
        else base = (long) Math.floor(4.5D * currentLevel * currentLevel - 162.5D * currentLevel + 2220.0D);
        long partial = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        return Math.max(0L, base + partial);
    }

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putLong("StoredExperience", storedExperience); }
    @Override public void load(CompoundTag tag) { super.load(tag); storedExperience = Math.max(0L, tag.getLong("StoredExperience")); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
