package com.teamzeromanager.xpstorage.registry;

import com.teamzeromanager.xpstorage.XPStorage;
import com.teamzeromanager.xpstorage.block.entity.XpCapsuleBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, XPStorage.MOD_ID);
    public static final RegistryObject<BlockEntityType<XpCapsuleBlockEntity>> XP_CAPSULE = BLOCK_ENTITIES.register("xp_capsule",
            () -> BlockEntityType.Builder.of(XpCapsuleBlockEntity::new, ModBlocks.XP_CAPSULE.get()).build(null));
    public static void register(IEventBus eventBus) { BLOCK_ENTITIES.register(eventBus); }
}
