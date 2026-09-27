package com.minecraft_terraria.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// ЖЕЛЕЗНЫЙ ПОДКЛЮЧАТЕЛЬ: Принудительно заставляем NeoForge слушать этот файл на клиенте
@EventBusSubscriber(modid = MinecraftTerrariamod.MODID, value = Dist.CLIENT)
public class MinecraftTerrariamodClient {

    private static SimpleSoundInstance currentTrack = null;
    private static ResourceKey<Biome> lastBiome = null;
    private static boolean wasInMenu = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        // 1. ВЫЗОВ ДЛЯ ГЛАВНОГО МЕНЮ И ЭКРАНА ЗАГРУЗКИ
        if (mc.level == null || mc.player == null) {
            lastBiome = null;

            if (mc.getMusicManager() != null) {
                mc.getMusicManager().stopPlaying();
            }

            if (!wasInMenu) {
                stopTerrariaMusic();

                SoundEvent menuSound = com.minecraft_terraria.mod.ModSounds.MENU.get();
                currentTrack = new SimpleSoundInstance(
                        menuSound.getLocation(), SoundSource.MUSIC, 1.0F, 1.0F,
                        net.minecraft.util.RandomSource.create(), false, 0,
                        SimpleSoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true
                );
                mc.getSoundManager().play(currentTrack);

                wasInMenu = true;
            }
            return;
        }

        // 2. ВЫЗОВ ПРИ ВХОДЕ В МИР (Выключаем музыку меню)
        if (wasInMenu) {
            stopTerrariaMusic();
            wasInMenu = false;
        }

        // Намертво блокируем стандартный фоновый плеер Майнкрафта
        if (mc.getMusicManager() != null) {
            mc.getMusicManager().stopPlaying();
        }

        // Проверяем смену биома раз в секунду (20 тиков)
        if (mc.level.getGameTime() % 20 == 0) {
            Holder<Biome> biomeHolder = mc.level.getBiome(mc.player.blockPosition());
            ResourceKey<Biome> currentBiome = biomeHolder.unwrapKey().orElse(null);

            if (currentBiome != null && !currentBiome.equals(lastBiome)) {
                lastBiome = currentBiome;
                SoundEvent biomeSound = getSoundEventForBiome(currentBiome);

                if (biomeSound != null) {
                    // Принудительное завершение старого трека, чтобы они не накладывались
                    stopTerrariaMusic();

                    String trackName = getTrackNameForBiome(currentBiome);

                    // Вызов тоста
                    mc.getToasts().clear();
                    mc.getToasts().addToast(new TerrariaMusicToast(trackName));

                    // 2. ПРЯМОЙ ИНТЕГРИРОВАННЫЙ ВЫЗОВ МУЗЫКИ:
                    // Используем стандартный рабочий конструктор Майнкрафта 1.21.1
                    currentTrack = new SimpleSoundInstance(
                            biomeSound.getLocation(),                         // Твоя системная переменная звука
                            SoundSource.RECORDS,                              // Канал пластинок (убирает ванильные паузы)
                            1.0F,                                             // Громкость 100%
                            1.0F,                                             // Тональность (Pitch)
                            net.minecraft.util.RandomSource.create(),         // Рандомайзер игры
                            true,                                             // Бесконечный повтор (Loop)
                            0,                                                // Задержка перед стартом (0 = мгновенно)
                            SimpleSoundInstance.Attenuation.NONE,             // Звук не затухает от расстояния
                            0.0D, 0.0D, 0.0D,                           // Координаты (0,0,0 так как относительный)
                            true                                              // Звук играет прямо в голове (relative)
                    );
                    mc.getSoundManager().play(currentTrack);
                }
            }
        }
    }

        public static SoundEvent getSoundEventForBiome(ResourceKey<Biome> biomeKey) {
            if (biomeKey == Biomes.DESERT || biomeKey == Biomes.BADLANDS) {
                return com.minecraft_terraria.mod.ModSounds.DESERT_MUSIC.get();
            } else if (biomeKey == Biomes.OCEAN || biomeKey == Biomes.DEEP_OCEAN || biomeKey == Biomes.BEACH) {
                return com.minecraft_terraria.mod.ModSounds.OCEAN_MUSIC.get();
            } else {
                return com.minecraft_terraria.mod.ModSounds.FOREST_MUSIC.get();
            }
        }

        private static String getTrackNameForBiome(ResourceKey<Biome> biomeKey) {
            if (biomeKey == Biomes.DESERT || biomeKey == Biomes.BADLANDS) {
                return "Desert Theme";
            } else if (biomeKey == Biomes.OCEAN || biomeKey == Biomes.DEEP_OCEAN || biomeKey == Biomes.BEACH) {
                return "Ocean Theme";
            } else {
                return "Overworld Day";
            }
        }


        // Твой красивый тёмно-серый тост с пластинкой
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
    private static void stopTerrariaMusic() {
        if (currentTrack != null) {
            Minecraft.getInstance().getSoundManager().stop(currentTrack);
            currentTrack = null;
        }
    }
}
