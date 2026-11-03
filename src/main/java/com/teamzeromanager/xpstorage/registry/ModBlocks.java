package com.teamzeromanager.xpstorage.registry;

import com.teamzeromanager.xpstorage.XPStorage;
import com.teamzeromanager.xpstorage.block.XpCapsuleBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, XPStorage.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XPStorage.MOD_ID);

    public static final RegistryObject<Block> XP_CAPSULE = BLOCKS.register("xp_capsule", () ->
            new XpCapsuleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F)
                    .sound(SoundType.GLASS).lightLevel(state -> 4).noOcclusion()));
    public static final RegistryObject<net.minecraft.world.item.Item> XP_CAPSULE_ITEM = ITEMS.register("xp_capsule",
            () -> new BlockItem(XP_CAPSULE.get(), new net.minecraft.world.item.Item.Properties()));

    public static void register(IEventBus eventBus) { BLOCKS.register(eventBus); ITEMS.register(eventBus); }
}
