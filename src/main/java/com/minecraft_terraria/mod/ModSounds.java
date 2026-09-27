package com.minecraft_terraria.mod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public class ModSounds {
    // Регистратор для звуков
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MinecraftTerrariamod.MODID);

    // Карта, которая будет хранить готовые объекты Music для каждого зарегистрированного ID
    private static final Map<String, Music> MUSIC_REGISTRY = new HashMap<>();

    // ==========================================
    // СПИСОК МУЗЫКИ
    // ==========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> FOREST_EVENT = registerMusic("music.forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORRUPTION_EVENT = registerMusic("music.corruption");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNDERGROUND_EVENT = registerMusic("music.underground");
    public static final DeferredHolder<SoundEvent, SoundEvent> DESERT_EVENT = registerMusic("music.desert");

//    public static final DeferredHolder<SoundEvent, SoundEvent> FOREST_MUSIC =
//            SOUNDS.register("music.forest",
//            () -> SoundEvent.createVariableRangeEvent(
//                    ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, "music.forest")
//            )
//    );

    private static DeferredHolder<SoundEvent, SoundEvent> registerMusic(String name) {
        DeferredHolder<SoundEvent, SoundEvent> holder = SOUNDS.register(name, () ->
                SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, name)
                ));
        // Запоминаем имя, чтобы потом динамически собрать объект Music по запросу
        MUSIC_REGISTRY.put(name, null);
        return holder;
    }

//    public static Music FOREST = new Music(
//            ModSounds.FOREST_MUSIC, // SoundEvent вашего трека
//            0,                                    // Минимальная пауза перед следующим треком
//            0,                                    // Максимальная пауза
//            true                                  // Заменять ли текущую играющую музыку?
//    );

    /**
     * ОДИН УНИВЕРСАЛЬНЫЙ ГЕТТЕР ДЛЯ ВСЕЙ МУЗЫКИ.
     * Возвращает объект Music по его строковому ID (например, "music.forest")
     */
    public static Music getMusic(String name) {
        if (!MUSIC_REGISTRY.containsKey(name)) {
            throw new IllegalArgumentException("Музыка с именем " + name + " не зарегистрирована в ModSounds!");
        }

        // Если объект Music для этого трека еще не создан — создаем его (ленивая инициализация)
        if (MUSIC_REGISTRY.get(name) == null) {
            ResourceLocation targetKey = ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, name);
            // Ищем зарегистрированный SoundEvent в DeferredRegister по его имени
            var holder = SOUNDS.getEntries().stream()
                    .filter(h -> h.getId().equals(targetKey))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("SoundEvent реестра не найден для ключа: " + targetKey));

            // Создаем ванильный объект Music (0, 0, true — мгновенный запуск с заменой ванильной музыки)
            Music musicInstance = new Music(holder, 0, 0, true);
            MUSIC_REGISTRY.put(name, musicInstance);
        }

        return MUSIC_REGISTRY.get(name);
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
