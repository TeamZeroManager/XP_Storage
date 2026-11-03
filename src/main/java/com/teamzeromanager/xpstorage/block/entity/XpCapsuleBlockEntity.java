package com.teamzeromanager.xpstorage.block.entity;

import com.teamzeromanager.xpstorage.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class XpCapsuleBlockEntity extends BlockEntity {
    private long storedExperience;

    public XpCapsuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.XP_CAPSULE.get(), pos, state);
    }

    public long getStoredExperience() {
        return storedExperience;
    }

    public void deposit(long points) {
        if (points <= 0L) return;
        storedExperience = storedExperience > Long.MAX_VALUE - points
                ? Long.MAX_VALUE : storedExperience + points;
        markStorageChanged();
    }

    public boolean withdraw(long points) {
        if (points <= 0L || points > storedExperience) return false;
        storedExperience -= points;
        markStorageChanged();
        return true;
    }

    private void markStorageChanged() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static long getXpForLevel(int level) {
        int safeLevel = Math.max(0, level);
        if (safeLevel <= 16) {
            return (long) safeLevel * safeLevel + 6L * safeLevel;
        }
        if (safeLevel <= 31) {
            return (long) Math.floor(2.5D * safeLevel * safeLevel
                    - 40.5D * safeLevel + 360.0D);
        }
        return (long) Math.floor(4.5D * safeLevel * safeLevel
                - 162.5D * safeLevel + 2220.0D);
    }

    public static long getPlayerExperiencePoints(Player player) {
        int level = Math.max(0, player.experienceLevel);
        long base = getXpForLevel(level);
        long partial = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        return Math.max(0L, base + partial);
    }

    /**
     * XP points required to advance the player by the requested number of whole levels.
     * Existing progress in the current XP bar is deducted from the cost.
     */
    public static long getCostForLevels(Player player, int levels) {
        if (levels <= 0) return 0L;
        int currentLevel = Math.max(0, player.experienceLevel);
        long targetLevelLong = (long) currentLevel + levels;
        if (targetLevelLong > 100000L) return Long.MAX_VALUE;

        long currentBase = getXpForLevel(currentLevel);
        long targetBase = getXpForLevel((int) targetLevelLong);
        long partial = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        return Math.max(0L, targetBase - currentBase - partial);
    }

    public static int getMaxWithdrawableLevels(Player player, long availablePoints) {
        if (availablePoints <= 0L) return 0;
        int currentLevel = Math.max(0, player.experienceLevel);
        int upperBound = Math.max(0, 100000 - currentLevel);
        upperBound = Math.min(upperBound, 100000);
        int low = 0;
        int high = upperBound;

        // Vanilla's player XP API accepts an int, so one withdrawal is capped safely.
        long budget = Math.min(availablePoints, (long) Integer.MAX_VALUE);
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            if (getCostForLevels(player, mid) <= budget) low = mid;
            else high = mid - 1;
        }
        return low;
    }

    public static int getEquivalentLevel(long points) {
        if (points <= 0L) return 0;
        int low = 0;
        int high = 100000;
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            if (getXpForLevel(mid) <= points) low = mid;
            else high = mid - 1;
        }
        return low;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("StoredExperience", storedExperience);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedExperience = Math.max(0L, tag.getLong("StoredExperience"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}
