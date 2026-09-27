package com.minecraft_terraria.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class TerrariaMusicManager {

    private static TerrariaSoundInstance currentTrack = null;
    private static ResourceKey<Biome> lastBiomeKey = null;
    private static boolean firstLoad = true;
    private static boolean inMenu = false;

//    public static void init() {
//        MinecraftTerrariamod.LOGGER.info("[TerrariaMusic] Музыкальный менеджер Террарии успешно инициализирован!");
//    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();

        // ЕСЛИ ИГРОК В ГЛАВНОМ МЕНЮ
        if (mc.level == null || mc.player == null) {
            lastBiomeKey = null;
            firstLoad = true;

            if (mc.getMusicManager() != null) {
                mc.getMusicManager().stopPlaying();
            }

            if (!inMenu) {
                stopCurrentTrack();
                MinecraftTerrariamod.LOGGER.info("[TerrariaMusic] Запуск музыки в главном меню!");
                currentTrack = new TerrariaSoundInstance(com.minecraft_terraria.mod.ModSounds.MENU.get());
                mc.getSoundManager().play(currentTrack);
                inMenu = true;
            }
            return;
        }

        // ЕСЛИ ИГРОК ЗАШЕЛ В МИР
        if (inMenu) {
            stopCurrentTrack();
            inMenu = false;
        }

        if (mc.getMusicManager() != null) {
            mc.getMusicManager().stopPlaying();
        }

        if (firstLoad || mc.level.getGameTime() % 20 == 0) {
            updateBiomeMusic(mc.player, mc.level);
            firstLoad = false;
        }
    }

    private static void updateBiomeMusic(Player player, Level level) {
        Holder<Biome> biomeHolder = level.getBiome(player.blockPosition());
        ResourceKey<Biome> currentBiomeKey = biomeHolder.unwrapKey().orElse(null);

        if (currentBiomeKey == null || currentBiomeKey.equals(lastBiomeKey)) {
            return;
        }

        lastBiomeKey = currentBiomeKey;
        SoundEvent selectedSound = getSoundEventForBiome(currentBiomeKey);

        if (selectedSound != null) {
            Minecraft mc = Minecraft.getInstance();
            stopCurrentTrack();

            MinecraftTerrariamod.LOGGER.info("=================================================");
            MinecraftTerrariamod.LOGGER.info("[TerrariaMusic] СМЕНА БИОМА: " + currentBiomeKey.location());
            MinecraftTerrariamod.LOGGER.info("[TerrariaMusic] ИГРАЕМ ТРЕК: " + selectedSound.getLocation());
            MinecraftTerrariamod.LOGGER.info("=================================================");

            currentTrack = new TerrariaSoundInstance(selectedSound);
            mc.getSoundManager().play(currentTrack);
        }
    }

    // МЕТОД ПЕРЕИМЕНОВАН: теперь он называется точно так, как его ищет твой клиентский класс!
    public static SoundEvent getSoundEventForBiome(ResourceKey<Biome> biomeKey) {
        if (biomeKey.equals(Biomes.DESERT) || biomeKey.equals(Biomes.BADLANDS)) {
            return com.minecraft_terraria.mod.ModSounds.DESERT_MUSIC.get();
        } else if (biomeKey.equals(Biomes.OCEAN) || biomeKey.equals(Biomes.DEEP_OCEAN) || biomeKey.equals(Biomes.BEACH)) {
            return com.minecraft_terraria.mod.ModSounds.OCEAN_MUSIC.get();
        } else {
            return com.minecraft_terraria.mod.ModSounds.FOREST_MUSIC.get();
        }
    }

    private static void stopCurrentTrack() {
        if (currentTrack != null) {
            Minecraft.getInstance().getSoundManager().stop(currentTrack);
            currentTrack = null;
        }
    }

    private static class TerrariaSoundInstance extends AbstractSoundInstance {
        protected TerrariaSoundInstance(SoundEvent soundEvent) {
            super(soundEvent, SoundSource.MUSIC, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.relative = true;
        }
    }
}
