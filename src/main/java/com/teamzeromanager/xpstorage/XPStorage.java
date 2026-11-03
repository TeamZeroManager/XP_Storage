package com.teamzeromanager.xpstorage;

import com.teamzeromanager.xpstorage.registry.ModBlockEntities;
import com.teamzeromanager.xpstorage.registry.ModBlocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(XPStorage.MOD_ID)
public class XPStorage {
    public static final String MOD_ID = "xpstorage";
    public XPStorage() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
    }
}
