package com.minecraft_terraria.mod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Регистрация звуковых событий мода.
 *
 * Раньше здесь было по отдельному DeferredHolder-полю на каждый трек (66 штук), и чтобы
 * добавить новый трек, нужно было править три места. Теперь достаточно ОДНОЙ строки в
 * списке {@link #TRACKS} — регистрация идёт циклом.
 *
 * Полный рецепт добавления нового трека:
 *   1. положить файл  src/main/resources/assets/minecraftterrariamod/sounds/music/имя.ogg
 *   2. добавить запись "music.имя" в sounds.json
 *   3. добавить строку "music.имя" в список TRACKS ниже
 *   4. навесить на него триггер в config/minecraftterrariamod-music.json (можно без перекомпиляции)
 */
public class ModSounds {

    // Регистратор для звуков
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MinecraftTerrariamod.MODID);

    /**
     * ВСЕ треки мода. Имя здесь = ключ в sounds.json = имя .ogg-файла.
     * Добавили строку — трек зарегистрирован.
     */
    public static final List<String> TRACKS = List.of(
            // --- Биомы Верхнего мира ---
            "music.badlands",
            "music.bamboo_jungle",
            "music.beach",
            "music.birch_forest",
            "music.cherry_grove",
            "music.cold_ocean",
            "music.dark_forest",
            "music.deep_cold_ocean",
            "music.deep_frozen_ocean",
            "music.deep_lukewarm_ocean",
            "music.deep_ocean",
            "music.desert",
            "music.dripstone_caves",
            "music.eroded_badlands",
            "music.flower_forest",
            "music.forest",
            "music.frozen_ocean",
            "music.frozen_peaks",
            "music.frozen_river",
            "music.ice_spikes",
            "music.jagged_peaks",
            "music.jungle",
            "music.lukewarm_ocean",
            "music.lush_caves",
            "music.mangrove_swamp",
            "music.mountain_grove",
            "music.mountain_meadow",
            "music.mushroom_fields",
            "music.ocean",
            "music.old_growth_birch_forest",
            "music.old_growth_pine_taiga",
            "music.old_growth_spruce_taiga",
            "music.plains",
            "music.river",
            "music.savanna",
            "music.savanna_plateau",
            "music.snowy_beach",
            "music.snowy_plains",
            "music.snowy_slopes",
            "music.snowy_taiga",
            "music.sparse_jungle",
            "music.stony_peaks",
            "music.stony_shore",
            "music.sunflower_plains",
            "music.swamp",
            "music.taiga",
            "music.warm_ocean",
            "music.windswept_forest",
            "music.windswept_gravelly_hills",
            "music.windswept_hills",
            "music.windswept_savanna",
            "music.wooded_badlands",
            // --- Нижний мир ---
            "music.basalt_deltas",
            "music.crimson_forest",
            "music.nether_wastes",
            "music.soul_sand_valley",
            "music.warped_forest",
            // --- Энд ---
            "music.end_barrens",
            "music.end_highlands",
            "music.end_midlands",
            "music.small_end_islands",
            "music.the_end",
            // --- Подземелья ---
            "music.deep_dark",
            // --- Триггеры: обстановка ---
            "music.village",
            "music.night_village",
            "music.night",
            "music.rain",
            "music.ocean_rain",
            "music.thunder",
            "music.underwater",
            "music.cave",
            // --- Триггеры: сражения и структуры ---
            "music.warden_battle",
            "music.wither_battle",
            "music.trial_chambers_battle",
            "music.woodland_mansion_battle",
            "music.stronghold",
            "music.void",
            "music.titres",
            // --- Главное меню ---
            "music.menu",
            "music.menu2",
            "music.menu3",
            "music.menu4"
    );

    /** Уникальные имена (на случай опечатки-дубликата в списке выше). */
    private static final Set<String> REGISTERED = new LinkedHashSet<>();

    static {
        for (String name : TRACKS) {
            if (REGISTERED.add(name)) {
                SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, name)
                ));
            }
        }
    }

    /** Зарегистрирован ли такой трек (используется валидацией конфига триггеров). */
    public static boolean isKnownTrack(String name) {
        return name != null && REGISTERED.contains(name);
    }

    /**
     * "Сырой" SoundEvent по строковому ID.
     *
     * Нужен для ручного проигрывания музыки с плавным затуханием (см. FadeMusicSound):
     * мы больше не отдаём трек ванильному MusicManager, а играем его сами,
     * поэтому нам нужен именно SoundEvent, а не объект Music.
     *
     * Возвращает null, если трек не зарегистрирован — вызывающий код должен это пережить,
     * чтобы одна опечатка в конфиге не роняла игру.
     */
    public static SoundEvent getSoundEvent(String name) {
        if (name == null) {
            return null;
        }
        ResourceLocation targetKey = ResourceLocation.fromNamespaceAndPath(MinecraftTerrariamod.MODID, name);
        for (var holder : SOUNDS.getEntries()) {
            if (holder.getId().equals(targetKey)) {
                return holder.get();
            }
        }
        return null;
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
