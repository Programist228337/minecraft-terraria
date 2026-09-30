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
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.sounds.Music;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;

@EventBusSubscriber(modid = MinecraftTerrariamod.MODID, value = Dist.CLIENT)
public class ModSounds {
    // Регистратор для звуков
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MinecraftTerrariamod.MODID);

    // Карта, которая будет хранить готовые объекты Music для каждого зарегистрированного ID
    private static final Map<String, Music> MUSIC_REGISTRY = new HashMap<>();

    // ==========================================
    // СПИСОК МУЗЫКИ
    // ==========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> FOREST_AMBIENT = registerMusic("music.forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> DESERT_AMBIENT = registerMusic("music.desert");
    public static final DeferredHolder<SoundEvent, SoundEvent> OCEAN_AMBIENT = registerMusic("music.ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOWER_FOREST_AMBIENT = registerMusic("music.flower_forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> BADLANDS_AMBIENT = registerMusic("music.badlands");
    public static final DeferredHolder<SoundEvent, SoundEvent> BAMBOO_JUNGLE_AMBIENT = registerMusic("music.bamboo_jungle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEACH_AMBIENT = registerMusic("music.beach");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIRCH_FOREST_AMBIENT = registerMusic("music.birch_forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHERRY_GROVE_AMBIENT = registerMusic("music.cherry_grove");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLD_OCEAN_AMBIENT = registerMusic("music.cold_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRIMSON_FOREST_AMBIENT = registerMusic("music.crimson_forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> DARK_FOREST_AMBIENT = registerMusic("music.dark_forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_COLD_OCEAN_AMBIENT = registerMusic("music.deep_cold_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_DARK_AMBIENT = registerMusic("music.deep_dark");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_FROZEN_OCEAN_AMBIENT = registerMusic("music.deep_frozen_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_LUKEWARM_OCEAN_AMBIENT = registerMusic("music.deep_lukewarm_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_OCEAN_AMBIENT = registerMusic("music.deep_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRIPSTONE_CAVES_AMBIENT = registerMusic("music.dripstone_caves");
    public static final DeferredHolder<SoundEvent, SoundEvent> END_BARRENS_AMBIENT = registerMusic("music.end_barrens");
    public static final DeferredHolder<SoundEvent, SoundEvent> END_HIGHLANDS_AMBIENT = registerMusic("music.end_highlands");
    public static final DeferredHolder<SoundEvent, SoundEvent> END_MIDLANDS_AMBIENT = registerMusic("music.end_midlands");
    public static final DeferredHolder<SoundEvent, SoundEvent> ERODED_BADLANDS_AMBIENT = registerMusic("music.eroded_badlands");
    public static final DeferredHolder<SoundEvent, SoundEvent> FROZEN_OCEAN_AMBIENT = registerMusic("music.frozen_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> FROZEN_PEAKS_AMBIENT = registerMusic("music.frozen_peaks");
    public static final DeferredHolder<SoundEvent, SoundEvent> FROZEN_RIVER_AMBIENT = registerMusic("music.frozen_river");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICE_SPIKES_AMBIENT = registerMusic("music.ice_spikes");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAGGED_PEAKS_AMBIENT = registerMusic("music.jagged_peaks");
    public static final DeferredHolder<SoundEvent, SoundEvent> JUNGLE_AMBIENT = registerMusic("music.jungle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUKEWARM_OCEAN_AMBIENT = registerMusic("music.lukewarm_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUSH_CAVES_AMBIENT = registerMusic("music.lush_caves");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANGROVE_SWAMP_AMBIENT = registerMusic("music.mangrove_swamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOUNTAIN_GROVE_AMBIENT = registerMusic("music.mountain_grove");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOUNTAIN_MEADOW_AMBIENT = registerMusic("music.mountain_meadow");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSHROOM_FIELDS_AMBIENT = registerMusic("music.mushroom_fields");
    public static final DeferredHolder<SoundEvent, SoundEvent> NETHER_WASTES_AMBIENT = registerMusic("music.nether_wastes");
    public static final DeferredHolder<SoundEvent, SoundEvent> OLD_GROWTH_BIRCH_FOREST_AMBIENT = registerMusic("music.old_growth_birch_forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> OLD_GROWTH_PINE_TAIGA_AMBIENT = registerMusic("music.old_growth_pine_taiga");
    public static final DeferredHolder<SoundEvent, SoundEvent> OLD_GROWTH_SPRUCE_TAIGA_AMBIENT = registerMusic("music.old_growth_spruce_taiga");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLAINS_AMBIENT = registerMusic("music.plains");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIVER_AMBIENT = registerMusic("music.river");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAVANNA_AMBIENT = registerMusic("music.savanna");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAVANNA_PLATEAU_AMBIENT = registerMusic("music.savanna_plateau");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_END_ISLANDS_AMBIENT = registerMusic("music.small_end_islands");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNOWY_BEACH_AMBIENT = registerMusic("music.snowy_beach");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNOWY_PLAINS_AMBIENT = registerMusic("music.snowy_plains");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNOWY_SLOPES_AMBIENT = registerMusic("music.snowy_slopes");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNOWY_TAIGA_AMBIENT = registerMusic("music.snowy_taiga");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_SAND_VALLEY_AMBIENT = registerMusic("music.soul_sand_valley");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPARSE_JUNGLE_AMBIENT = registerMusic("music.sparse_jungle");
    public static final DeferredHolder<SoundEvent, SoundEvent> STONY_PEAKS_AMBIENT = registerMusic("music.stony_peaks");
    public static final DeferredHolder<SoundEvent, SoundEvent> STONY_SHORE_AMBIENT = registerMusic("music.stony_shore");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUNFLOWER_PLAINS_AMBIENT = registerMusic("music.sunflower_plains");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWAMP_AMBIENT = registerMusic("music.swamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAIGA_AMBIENT = registerMusic("music.taiga");
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_END_AMBIENT = registerMusic("music.the_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARM_OCEAN_AMBIENT = registerMusic("music.warm_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARPED_FOREST_AMBIENT = registerMusic("music.warped_forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDSWEPT_HILLS_AMBIENT = registerMusic("music.windswept_hills");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDSWEPT_FOREST_AMBIENT = registerMusic("music.windswept_forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDSWEPT_GRAVELLY_HILLS_AMBIENT = registerMusic("music.windswept_gravelly_hills");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDSWEPT_SAVANNA_AMBIENT = registerMusic("music.windswept_savanna");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOODED_BADLANDS_AMBIENT = registerMusic("music.wooded_badlands");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASALT_DELTAS_AMBIENT = registerMusic("music.basalt_deltas");
    public static final DeferredHolder<SoundEvent, SoundEvent> MENU_AMBIENT = registerMusic("music.menu");
    public static final DeferredHolder<SoundEvent, SoundEvent> MENU2_AMBIENT = registerMusic("music.menu2");
    public static final DeferredHolder<SoundEvent, SoundEvent> MENU3_AMBIENT = registerMusic("music.menu3");

    private static DeferredHolder<SoundEvent, SoundEvent> registerMusic(String name) {
        DeferredHolder<SoundEvent, SoundEvent> holder = SOUNDS.register(name, () ->
                SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, name)
                ));
        // Запоминаем имя, чтобы потом динамически собрать объект Music по запросу
        MUSIC_REGISTRY.put(name, null);
        return holder;
    }

    // ОДИН УНИВЕРСАЛЬНЫЙ ГЕТТЕР ДЛЯ ВСЕЙ МУЗЫКИ. Возвращает объект Music по его строковому ID
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

            // Создаем объект Music
            Music musicInstance = new Music(holder, 0, 0, true);
            MUSIC_REGISTRY.put(name, musicInstance);
        }

        return MUSIC_REGISTRY.get(name);
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
    // 1. Поля класса (должны быть прямо внутри класса, вне методов)
    private static final List<Music> MENU_MUSIC_LIST = new ArrayList<>();
    private static final Random RANDOM = new Random();

    // 2. Статический блок инициализации (чтобы заполнить список при старте игры)
    static {
        MENU_MUSIC_LIST.add(getMusic("music.menu"));
        MENU_MUSIC_LIST.add(getMusic("music.menu2"));
        MENU_MUSIC_LIST.add(getMusic("music.menu3"));
    }

    // 3. Метод подмены музыки
    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        if (event.getMusic().equals(net.minecraft.sounds.Musics.MENU)) {
            if (!MENU_MUSIC_LIST.isEmpty()) {
                Music randomMenuMusic = MENU_MUSIC_LIST.get(RANDOM.nextInt(MENU_MUSIC_LIST.size()));
                event.setMusic(randomMenuMusic);
            }
        }
    }
}
