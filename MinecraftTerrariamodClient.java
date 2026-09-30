package com.minecraft_terraria.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import java.util.List;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = MinecraftTerrariamod.MODID, value = Dist.CLIENT)
public class MinecraftTerrariamodClient {

    private static final List<Music> MENU_MUSIC_LIST = new ArrayList<>();
    private static final java.util.Random RANDOM = new java.util.Random();
    // Вспомогательный класс для хранения музыки вместе с её названием
    private record BiomeMusic(Music music, String title) {}
    // Список соотношений биомов и музыки
    private static final Map<ResourceLocation, BiomeMusic> BIOME_MUSIC_MAP = new HashMap<>();
    static {
        // Регистрируйте здесь любые биомы и соответствующую им музыку
        registerBiomeMusic("minecraft:forest", "music.forest", "Forest theme");
        registerBiomeMusic("minecraft:desert", "music.desert", "Desert theme");
        registerBiomeMusic("minecraft:ocean", "music.ocean", "Ocean theme");
        registerBiomeMusic("minecraft:flower_forest","music.flower_forest","Flower Forest theme");
        registerBiomeMusic("minecraft:badlands","music.badlands","Badlands theme");
        registerBiomeMusic("minecraft:bamboo_jungle","music.bamboo_jungle","Bamboo theme");
        registerBiomeMusic("minecraft:beach","music.beach","Beach theme");
        registerBiomeMusic("minecraft:birch_forest","music.birch_forest","Birch theme");
        registerBiomeMusic("minecraft:cherry_grove","music.cherry_grove","Cherry grove theme");
        registerBiomeMusic("minecraft:cold_ocean","music.cold_ocean","Cold ocean theme");
        registerBiomeMusic("minecraft:crimson_forest","music.crimson_forest", "Crimson theme");
        registerBiomeMusic("minecraft:dark_forest","music.dark_forest","Dark forest theme");
        registerBiomeMusic( "minecraft:deep_cold_ocean",  "music.deep_cold_ocean",  "Deep cold ocean theme");
        registerBiomeMusic( "minecraft:deep_dark",  "music.deep_dark",  "Deep dark theme");
        registerBiomeMusic( "minecraft:deep_frozen_ocean",  "music.deep_frozen_ocean", "Deep frozen ocean theme");
        registerBiomeMusic( "minecraft:deep_lukewarm_ocean",  "music.deep_lukewarm_ocean",  "Deep lukewarm ocean theme");
        registerBiomeMusic( "minecraft:deep_ocean",  "music.deep_ocean",  "Deep ocean theme");
        registerBiomeMusic( "minecraft:dripstone_caves",  "music.dripstone_caves",  "Dripstone caves theme");
        registerBiomeMusic( "minecraft:end_barrens",  "music.end_barrens",  "End barrens theme");
        registerBiomeMusic( "minecraft:end_highlands",  "music.end_highlands",  "End highlands theme");
        registerBiomeMusic( "minecraft:end_midlands",  "music.end_midlands",  "End midlands theme");
        registerBiomeMusic( "minecraft:eroded_badlands",  "music.eroded_badlands",  "Eroded badlands theme");
        registerBiomeMusic( "minecraft:frozen_ocean",  "music.frozen_ocean",  "Frozen ocean theme");
        registerBiomeMusic( "minecraft:frozen_peaks",  "music.frozen_peaks",  "Frozen peaks theme");
        registerBiomeMusic( "minecraft:frozen_river",  "music.frozen_river",  "Frozen river theme");
        registerBiomeMusic( "minecraft:ice_spikes",  "music.ice_spikes",  "Ice spikes theme");
        registerBiomeMusic( "minecraft:jagged_peaks",  "music.jagged_peaks",  "Jagged peaks theme");
        registerBiomeMusic( "minecraft:jungle",  "music.jungle",  "Jungle theme");
        registerBiomeMusic( "minecraft:lukewarm_ocean",  "music.lukewarm_ocean",  "Lukewarm ocean theme");
        registerBiomeMusic( "minecraft:lush_caves",  "music.lush_caves",  "Lush caves theme");
        registerBiomeMusic( "minecraft:mangrove_swamp",  "music.mangrove_swamp",  "Mangrove swamp theme");
        registerBiomeMusic( "minecraft:grove",  "music.mountain_grove",  "Mountain grove theme");
        registerBiomeMusic( "minecraft:meadow",  "music.mountain_meadow",  "Mountain meadow theme");
        registerBiomeMusic( "minecraft:mushroom_fields",  "music.mushroom_fields",  "Mushroom fields theme");
        registerBiomeMusic( "minecraft:nether_wastes",  "music.nether_wastes",  "Nether wastes theme");
        registerBiomeMusic( "minecraft:old_growth_birch_forest",  "music.old_growth_birch_forest",  "Old growth birch forest theme");
        registerBiomeMusic( "minecraft:old_growth_pine_taiga",  "music.old_growth_pine_taiga",  "Old growth pine taiga theme");
        registerBiomeMusic( "minecraft:old_growth_spruce_taiga", "music.old_growth_spruce_taiga",  "Old growth spruce taiga theme");
        registerBiomeMusic( "minecraft:plains",  "music.plains",  "Plains theme");
        registerBiomeMusic( "minecraft:river",  "music.river",  "River theme");
        registerBiomeMusic( "minecraft:savanna", "music.savanna",  "Savanna theme");
        registerBiomeMusic( "minecraft:savanna_plateau",  "music.savanna_plateau",  "Savanna plateau theme");
        registerBiomeMusic( "minecraft:small_end_islands",  "music.small_end_islands",  "Small end islands theme");
        registerBiomeMusic( "minecraft:snowy_beach",  "music.snowy_beach",  "Snowy beach theme");
        registerBiomeMusic( "minecraft:snowy_plains",  "music.snowy_plains",  "Snowy plains theme");
        registerBiomeMusic( "minecraft:snowy_slopes",  "music.snowy_slopes",  "Snowy slopes theme");
        registerBiomeMusic( "minecraft:snowy_taiga",  "music.snowy_taiga",  "Snowy taiga theme");
        registerBiomeMusic( "minecraft:soul_sand_valley",  "music.soul_sand_valley",  "Soul sand valley theme");
        registerBiomeMusic( "minecraft:sparse_jungle",  "music.sparse_jungle",  "Sparse jungle theme");
        registerBiomeMusic( "minecraft:stony_peaks",  "music.stony_peaks",  "Stony peaks theme");
        registerBiomeMusic( "minecraft:stony_shore",  "music.stony_shore",  "Stony shore theme");
        registerBiomeMusic( "minecraft:sunflower_plains",  "music.sunflower_plains",  "Sunflower plains theme");
        registerBiomeMusic( "minecraft:swamp",  "music.swamp",  "Swamp theme");
        registerBiomeMusic( "minecraft:taiga",  "music.taiga",  "Taiga theme");
        registerBiomeMusic( "minecraft:the_end",  "music.the_end",  "The end theme");
        registerBiomeMusic( "minecraft:warm_ocean",  "music.warm_ocean",  "Warm ocean theme");
        registerBiomeMusic( "minecraft:warped_forest",  "music.warped_forest",  "Warped forest theme");
        registerBiomeMusic( "minecraft:windswept_hills",  "music.windswept_hills",  "Windswept hills theme");
        registerBiomeMusic( "minecraft:windswept_forest",  "music.windswept_forest",  "Windswept forest theme");
        registerBiomeMusic( "minecraft:windswept_gravelly_hills",  "music.windswept_gravelly_hills",  "Windswept gravelly hills theme");
        registerBiomeMusic( "minecraft:windswept_savanna",  "music.windswept_savanna",  "Windswept savanna theme");
        registerBiomeMusic( "minecraft:wooded_badlands",  "music.wooded_badlands",  "Wooded badlands theme");
        registerBiomeMusic("minecraft:basalt_deltas", "music.basalt_deltas", "Basalt deltas theme");
        MENU_MUSIC_LIST.add(ModSounds.getMusic("music.menu"));
        MENU_MUSIC_LIST.add(ModSounds.getMusic("music.menu2"));
        MENU_MUSIC_LIST.add(ModSounds.getMusic("music.menu3"));

    }

    private static void registerBiomeMusic(String biomeId, String musicRegistryName, String title) {
        BIOME_MUSIC_MAP.put(
                ResourceLocation.parse(biomeId),
                new BiomeMusic(ModSounds.getMusic(musicRegistryName), title)
        );
    }
    private static ResourceLocation lastBiome = null;
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            lastBiome = null; // сброс при выходе из мира
            return;
        }

        ResourceLocation biomeKey = minecraft.level.getBiome(minecraft.player.blockPosition())
                .unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);
        if (biomeKey == null) return;

        BiomeMusic entry = BIOME_MUSIC_MAP.get(biomeKey);

        // Тост показываем каждый тик-проверкой смены биома (не зависит от %20)
        if (!biomeKey.equals(lastBiome)) {
            lastBiome = biomeKey;
            if (entry != null) {
                minecraft.getToasts().clear();
                minecraft.getToasts().addToast(new TerrariaMusicToast(entry.title()));
            }
        }

        // Дальше твоя прежняя логика, раз в секунду
        if (minecraft.level.getGameTime() % 20 != 0) return;

        MusicManager musicManager = minecraft.getMusicManager();

        if (entry != null) {
            Music targetMusic = entry.music();
            if (!musicManager.isPlayingMusic(targetMusic)) {
                musicManager.stopPlaying();
                musicManager.startPlaying(targetMusic);
            }
        } else {
            musicManager.stopPlaying();
        }
    }

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
    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        // 1. ЛОГИКА ДЛЯ ИЗМЕРЕНИЯ КРАЯ (срабатывает, только если мы в игре)
        if (minecraft.level != null && minecraft.player != null) {
            if (minecraft.level.dimension() == net.minecraft.world.level.Level.END) {
                ResourceLocation biomeKey = minecraft.level.getBiome(minecraft.player.blockPosition())
                        .unwrapKey()
                        .map(ResourceKey::location)
                        .orElse(null);

                if (biomeKey != null && BIOME_MUSIC_MAP.containsKey(biomeKey)) {
                    event.setMusic(BIOME_MUSIC_MAP.get(biomeKey).music());
                    return; // Если это Энд, выходим, чтобы код меню ниже не тратил ресурсы
                }
            }
        }
    }
    private static Music activeMenuTrack = null;

    @SubscribeEvent
    public static void onSelectMenuMusic(SelectMusicEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        // Проверяем, что Майнкрафт пытается включить или обновить музыку главного меню
        if (event.getMusic().equals(net.minecraft.sounds.Musics.MENU)) {

            // Если у нас еще нет выбранного трека, ИЛИ текущий трек почему-то перестал играть
            if (activeMenuTrack == null || !minecraft.getMusicManager().isPlayingMusic(activeMenuTrack)) {
                if (!MENU_MUSIC_LIST.isEmpty()) {
                    // Выбираем ОДИН случайный трек и запоминаем его
                    activeMenuTrack = MENU_MUSIC_LIST.get(RANDOM.nextInt(MENU_MUSIC_LIST.size()));
                    System.out.println("[TerrariaMod] Выбран новый трек меню: " + activeMenuTrack.toString());
                }
            }

            // Принудительно отдаем игре наш выбранный трек, пока он не доиграет до конца
            if (activeMenuTrack != null) {
                event.setMusic(activeMenuTrack);
            }
        } else {
            // Если игра вышла из меню в мир, сбрасываем активный трек меню
            activeMenuTrack = null;
        }
    }
}