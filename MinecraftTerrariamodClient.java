package com.minecraft_terrariamod.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Музыка биомов в стиле Террарии: при переходе в другой биом старый трек НЕ обрывается,
 * а плавно затихает, пока новый плавно нарастает (кроссфейд).
 *
 * КАК ЭТО РАБОТАЕТ (коротко):
 *  1. В {@link SelectMusicEvent} мы всегда отдаём null — ванильный MusicManager вообще
 *     ничего не играет. Иначе он накладывался бы на нашу музыку (и он умеет только
 *     резко останавливать трек — никакого фейда в 1.21.1 у него нет).
 *  2. Всю музыку мы играем сами через {@link SoundManager}, объектами {@link FadeMusicSound},
 *     у которых громкость меняется каждый тик. Отсюда и плавные затухания.
 */
@EventBusSubscriber(modid = MinecraftTerrariamod.MODID, value = Dist.CLIENT)
public class MinecraftTerrariamodClient {

    // ==========================================================================
    //  1. ТАБЛИЦА "БИОМ -> МУЗЫКА"
    // ==========================================================================

    /** Название трека (ключ в sounds.json) + подпись для тоста. */
    private record BiomeMusic(String soundName, String title) {}

    /** Список соотношений биомов и музыки. */
    private static final Map<ResourceLocation, BiomeMusic> BIOME_MUSIC_MAP = new HashMap<>();

    static {
        // Регистрируйте здесь любые биомы и соответствующую им музыку
        registerBiomeMusic("minecraft:forest", "music.forest", "Forest theme");
        registerBiomeMusic("minecraft:desert", "music.desert", "Desert theme");
        registerBiomeMusic("minecraft:ocean", "music.ocean", "Ocean theme");
        registerBiomeMusic("minecraft:flower_forest", "music.flower_forest", "Flower Forest theme");
        registerBiomeMusic("minecraft:badlands", "music.badlands", "Badlands theme");
        registerBiomeMusic("minecraft:bamboo_jungle", "music.bamboo_jungle", "Bamboo theme");
        registerBiomeMusic("minecraft:beach", "music.beach", "Beach theme");
        registerBiomeMusic("minecraft:birch_forest", "music.birch_forest", "Birch theme");
        registerBiomeMusic("minecraft:cherry_grove", "music.cherry_grove", "Cherry grove theme");
        registerBiomeMusic("minecraft:cold_ocean", "music.cold_ocean", "Cold ocean theme");
        registerBiomeMusic("minecraft:crimson_forest", "music.crimson_forest", "Crimson theme");
        registerBiomeMusic("minecraft:dark_forest", "music.dark_forest", "Dark forest theme");
        registerBiomeMusic("minecraft:deep_cold_ocean", "music.deep_cold_ocean", "Deep cold ocean theme");
        registerBiomeMusic("minecraft:deep_dark", "music.deep_dark", "Deep dark theme");
        registerBiomeMusic("minecraft:deep_frozen_ocean", "music.deep_frozen_ocean", "Deep frozen ocean theme");
        registerBiomeMusic("minecraft:deep_lukewarm_ocean", "music.deep_lukewarm_ocean", "Deep lukewarm ocean theme");
        registerBiomeMusic("minecraft:deep_ocean", "music.deep_ocean", "Deep ocean theme");
        registerBiomeMusic("minecraft:dripstone_caves", "music.dripstone_caves", "Dripstone caves theme");
        registerBiomeMusic("minecraft:end_barrens", "music.end_barrens", "End barrens theme");
        registerBiomeMusic("minecraft:end_highlands", "music.end_highlands", "End highlands theme");
        registerBiomeMusic("minecraft:end_midlands", "music.end_midlands", "End midlands theme");
        registerBiomeMusic("minecraft:eroded_badlands", "music.eroded_badlands", "Eroded badlands theme");
        registerBiomeMusic("minecraft:frozen_ocean", "music.frozen_ocean", "Frozen ocean theme");
        registerBiomeMusic("minecraft:frozen_peaks", "music.frozen_peaks", "Frozen peaks theme");
        registerBiomeMusic("minecraft:frozen_river", "music.frozen_river", "Frozen river theme");
        registerBiomeMusic("minecraft:ice_spikes", "music.ice_spikes", "Ice spikes theme");
        registerBiomeMusic("minecraft:jagged_peaks", "music.jagged_peaks", "Jagged peaks theme");
        registerBiomeMusic("minecraft:jungle", "music.jungle", "Jungle theme");
        registerBiomeMusic("minecraft:lukewarm_ocean", "music.lukewarm_ocean", "Lukewarm ocean theme");
        registerBiomeMusic("minecraft:lush_caves", "music.lush_caves", "Lush caves theme");
        registerBiomeMusic("minecraft:mangrove_swamp", "music.mangrove_swamp", "Mangrove swamp theme");
        registerBiomeMusic("minecraft:grove", "music.mountain_grove", "Mountain grove theme");
        registerBiomeMusic("minecraft:meadow", "music.mountain_meadow", "Mountain meadow theme");
        registerBiomeMusic("minecraft:mushroom_fields", "music.mushroom_fields", "Mushroom fields theme");
        registerBiomeMusic("minecraft:nether_wastes", "music.nether_wastes", "Nether wastes theme");
        registerBiomeMusic("minecraft:old_growth_birch_forest", "music.old_growth_birch_forest", "Old growth birch forest theme");
        registerBiomeMusic("minecraft:old_growth_pine_taiga", "music.old_growth_pine_taiga", "Old growth pine taiga theme");
        registerBiomeMusic("minecraft:old_growth_spruce_taiga", "music.old_growth_spruce_taiga", "Old growth spruce taiga theme");
        registerBiomeMusic("minecraft:plains", "music.plains", "Plains theme");
        registerBiomeMusic("minecraft:river", "music.river", "River theme");
        registerBiomeMusic("minecraft:savanna", "music.savanna", "Savanna theme");
        registerBiomeMusic("minecraft:savanna_plateau", "music.savanna_plateau", "Savanna plateau theme");
        registerBiomeMusic("minecraft:small_end_islands", "music.small_end_islands", "Small end islands theme");
        registerBiomeMusic("minecraft:snowy_beach", "music.snowy_beach", "Snowy beach theme");
        registerBiomeMusic("minecraft:snowy_plains", "music.snowy_plains", "Snowy plains theme");
        registerBiomeMusic("minecraft:snowy_slopes", "music.snowy_slopes", "Snowy slopes theme");
        registerBiomeMusic("minecraft:snowy_taiga", "music.snowy_taiga", "Snowy taiga theme");
        registerBiomeMusic("minecraft:soul_sand_valley", "music.soul_sand_valley", "Soul sand valley theme");
        registerBiomeMusic("minecraft:sparse_jungle", "music.sparse_jungle", "Sparse jungle theme");
        registerBiomeMusic("minecraft:stony_peaks", "music.stony_peaks", "Stony peaks theme");
        registerBiomeMusic("minecraft:stony_shore", "music.stony_shore", "Stony shore theme");
        registerBiomeMusic("minecraft:sunflower_plains", "music.sunflower_plains", "Sunflower plains theme");
        registerBiomeMusic("minecraft:swamp", "music.swamp", "Swamp theme");
        registerBiomeMusic("minecraft:taiga", "music.taiga", "Taiga theme");
        registerBiomeMusic("minecraft:the_end", "music.the_end", "The end theme");
        registerBiomeMusic("minecraft:warm_ocean", "music.warm_ocean", "Warm ocean theme");
        registerBiomeMusic("minecraft:warped_forest", "music.warped_forest", "Warped forest theme");
        registerBiomeMusic("minecraft:windswept_hills", "music.windswept_hills", "Windswept hills theme");
        registerBiomeMusic("minecraft:windswept_forest", "music.windswept_forest", "Windswept forest theme");
        registerBiomeMusic("minecraft:windswept_gravelly_hills", "music.windswept_gravelly_hills", "Windswept gravelly hills theme");
        registerBiomeMusic("minecraft:windswept_savanna", "music.windswept_savanna", "Windswept savanna theme");
        registerBiomeMusic("minecraft:wooded_badlands", "music.wooded_badlands", "Wooded badlands theme");
        registerBiomeMusic("minecraft:basalt_deltas", "music.basalt_deltas", "Basalt deltas theme");
    }

    private static void registerBiomeMusic(String biomeId, String soundName, String title) {
        BIOME_MUSIC_MAP.put(ResourceLocation.parse(biomeId), new BiomeMusic(soundName, title));
    }

    // ==========================================================================
    //  2. МУЗЫКА ГЛАВНОГО МЕНЮ
    // ==========================================================================

    /** Имена треков меню (ключи в sounds.json / ModSounds). */
    private static final List<String> MENU_TRACKS = List.of("music.menu", "music.menu2", "music.menu3");
    private static final Random RANDOM = new Random();
    /** Последний игравший трек меню — чтобы не выпадал два раза подряд. */
    private static String lastMenuTrack = null;

    private static String pickRandomMenuTrack() {
        String next;
        do {
            next = MENU_TRACKS.get(RANDOM.nextInt(MENU_TRACKS.size()));
        } while (MENU_TRACKS.size() > 1 && next.equals(lastMenuTrack));
        lastMenuTrack = next;
        return next;
    }

    // ==========================================================================
    //  3. НАСТРОЙКИ ПЛАВНОГО ПЕРЕХОДА (в тиках; 20 тиков = 1 секунда)
    // ==========================================================================

    /** Сколько тиков старое трек затихает. 50 = 2.5 сек. */
    private static final int FADE_OUT_TICKS = 50;
    /** Сколько тиков новый трек нарастает. 60 = 3 сек. */
    private static final int FADE_IN_TICKS = 60;
    /** Ключ "мы в главном меню" (id биома никогда так не выглядит). */
    private static final String MENU_KEY = "menu";

    // ==========================================================================
    //  4. СОСТОЯНИЕ КОНТРОЛЛЕРА МУЗЫКИ
    // ==========================================================================

    /** Трек, который играет (или только что начал нарастать) прямо сейчас. */
    private static FadeMusicSound currentTrack;
    /** Ключ активного трека: id биома или MENU_KEY. */
    private static String currentKey;
    /** Треки, которые сейчас дозатухают после смены биома. */
    private static final List<FadeMusicSound> fadingOutTracks = new ArrayList<>();

    // ==========================================================================
    //  5. ОСНОВНАЯ ЛОГИКА
    // ==========================================================================

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        SoundManager soundManager = minecraft.getSoundManager();

        // --- 5.1 Убираем треки, которые уже дозатухли или доиграли ---
        fadingOutTracks.removeIf(track -> track.isStopped() || !soundManager.isActive(track));

        // --- 5.2 Следим за активным треком ---
        if (currentTrack != null) {
            if (soundManager.isActive(currentTrack)) {
                currentTrack.markPlaying();
            } else if (currentTrack.isStopped() || currentTrack.wasPlaying()) {
                // Трек оборвался (например, зацикливание не сработало) — освобождаем слот,
                // ниже контроллер запустит его заново.
                currentTrack = null;
                currentKey = null;
            }
        }

        boolean inWorld = minecraft.player != null && minecraft.level != null;

        // --- 5.3 В мире биом проверяем раз в секунду (как и раньше), в меню — каждый тик ---
        if (inWorld && minecraft.level.getGameTime() % 20L != 0L) {
            return;
        }

        String desiredKey = inWorld ? resolveBiomeKey(minecraft) : MENU_KEY;

        // --- 5.4 В этом биоме своей музыки нет: плавно уводим текущую в тишину ---
        if (desiredKey == null) {
            if (currentTrack != null) {
                fadeOut(currentTrack);
                currentTrack = null;
            }
            currentKey = null;
            return;
        }

        // --- 5.5 Уже играет то, что нужно — ничего не трогаем ---
        if (desiredKey.equals(currentKey) && currentTrack != null) {
            return;
        }

        // --- 5.6 Смена трека: старое затихает, новое нарастает (кроссфейд) ---
        if (currentTrack != null) {
            fadeOut(currentTrack);
            currentTrack = null;
        }

        boolean menu = MENU_KEY.equals(desiredKey);

        String soundName;
        String title = null;

        if (menu) {
            soundName = pickRandomMenuTrack();
        } else {
            BiomeMusic biomeMusic = BIOME_MUSIC_MAP.get(ResourceLocation.parse(desiredKey));
            soundName = biomeMusic.soundName();
            title = biomeMusic.title();
        }

        SoundEvent soundEvent = ModSounds.getSoundEvent(soundName);

        // В меню треки должны сменять друг друга -> не зацикливаем.
        // В биоме наоборот: тема биома играет бесконечно, как в Террарии -> looping = true.
        FadeMusicSound track = new FadeMusicSound(soundEvent, 0.0F, !menu);
        track.fadeTo(1.0F, FADE_IN_TICKS);
        soundManager.play(track);

        currentTrack = track;
        currentKey = desiredKey;

        // Тост с названием трека показываем только для биомов (как было раньше)
        if (!menu) {
            minecraft.getToasts().clear();
            minecraft.getToasts().addToast(new TerrariaMusicToast(title));
        }
    }

    /** Отправить трек в плавное затухание. */
    private static void fadeOut(FadeMusicSound track) {
        track.fadeTo(0.0F, FADE_OUT_TICKS);
        fadingOutTracks.add(track);
    }

    /** Ключ текущего биома, если для него есть музыка. Иначе null. */
    private static String resolveBiomeKey(Minecraft minecraft) {
        ResourceLocation biomeKey = minecraft.level.getBiome(minecraft.player.blockPosition())
                .unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);

        if (biomeKey == null) {
            return null;
        }
        return BIOME_MUSIC_MAP.containsKey(biomeKey) ? biomeKey.toString() : null;
    }

    // ==========================================================================
    //  6. ЗАБИРАЕМ МУЗЫКУ У ВАНИЛЬНОГО MUSICMANAGER
    // ==========================================================================

    /**
     * Возвращаем null — это официально поддерживаемый способ сказать игре
     * "ничего не играй, музыку контролирую я".
     *
     * Зачем: в 1.21.1 у MusicManager нет никакого фейда, он умеет только резко
     * остановить трек (stopPlaying). Именно из-за этого музыка и обрывалась.
     */
    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        event.setMusic(null);
    }

    // ==========================================================================
    //  7. ТОСТ "СЕЙЧАС ИГРАЕТ"
    // ==========================================================================

    private static class TerrariaMusicToast implements Toast {
        private final String trackName;

        public TerrariaMusicToast(String trackName) {
            this.trackName = trackName;
        }

        @Override
        public Toast.Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long l) {
            guiGraphics.fill(0, 0, 160, 32, 0xCC2B2B2B);
            guiGraphics.fill(0, 31, 160, 32, 0xFFD4AF37);

            guiGraphics.drawString(toastComponent.getMinecraft().font, Component.translatable("minecraftterrariamod.toast"), 34, 5, 0xFFFFFFFF, false);
            guiGraphics.drawString(toastComponent.getMinecraft().font, Component.literal("§f" + trackName), 34, 17, 0xFFFFFFFF, false);

            ItemStack recordStack = new ItemStack(Items.MUSIC_DISC_STAL);
            guiGraphics.renderFakeItem(recordStack, 9, 8);

            return l >= 5000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
        }
    }
}
