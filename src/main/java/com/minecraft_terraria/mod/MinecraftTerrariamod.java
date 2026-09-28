package com.minecraft_terraria.mod;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(MinecraftTerrariamod.MODID)
public class MinecraftTerrariamod {
    public static final String MODID = "minecraftterrariamod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinecraftTerrariamod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModSounds.register(modEventBus); // Регистратор музыки
    }
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("TERRARIA MUSIC MOD ЗАПУЩЕН!");
    }
}
