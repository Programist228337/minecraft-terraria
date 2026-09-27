package com.minecraft_terraria.mod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MinecraftTerrariamod.MODID);

    // Указываем только "music/menu", игрушка сама допишет всё остальное!
    public static final DeferredHolder<SoundEvent, SoundEvent> MENU =
            SOUND_EVENTS.register("music_menu",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, "music/menu")));

    public static final DeferredHolder<SoundEvent, SoundEvent> FOREST_MUSIC =
            SOUND_EVENTS.register("music_forest",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, "music/forest")));

    public static final DeferredHolder<SoundEvent, SoundEvent> DESERT_MUSIC =
            SOUND_EVENTS.register("music_desert",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, "music/desert")));

    public static final DeferredHolder<SoundEvent, SoundEvent> OCEAN_MUSIC =
            SOUND_EVENTS.register("music_ocean",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, "music/ocean")));

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}
