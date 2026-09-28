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

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = MinecraftTerrariamod.MODID, value = Dist.CLIENT)
public class MinecraftTerrariamodClient {

    // Вспомогательный класс для хранения музыки вместе с её названием
    private record BiomeMusic(Music music, String title) {}
    // Список соотношений биомов и музыки
    private static final Map<ResourceLocation, BiomeMusic> BIOME_MUSIC_MAP = new HashMap<>();
    static {
        // Регистрируйте здесь любые биомы и соответствующую им музыку
        registerBiomeMusic("minecraft:forest", "music.forest", "Forest theme");
        registerBiomeMusic("minecraft:desert", "music.desert", "Desert theme");
        registerBiomeMusic("minecraft:ocean", "music.ocean", "Ocean theme");
    }

    private static void registerBiomeMusic(String biomeId, String musicRegistryName, String title) {
        BIOME_MUSIC_MAP.put(
                ResourceLocation.parse(biomeId),
                new BiomeMusic(ModSounds.getMusic(musicRegistryName), title)
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        // Обязательно проверяем, что игрок в мире, чтобы избежать NullPointerException
        if (minecraft.player == null || minecraft.level == null) return;

        // Проверка раз в секунду
        if (minecraft.level.getGameTime() % 20 != 0) return;
        // Получение биома игрока
        ResourceLocation biomeKey = minecraft.level.getBiome(minecraft.player.blockPosition())
                .unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);
        // Проверка ,что биом существует
        if (biomeKey == null) return;
        MusicManager musicManager = minecraft.getMusicManager();

        if (BIOME_MUSIC_MAP.containsKey(biomeKey)) {
            BiomeMusic entry = BIOME_MUSIC_MAP.get(biomeKey);
            Music targetMusic = entry.music();

            // Проверяем, не играет ли уже эта конкретная музыка, чтобы не перезапускать её каждый тик
            if (!musicManager.isPlayingMusic(targetMusic)) {
                // Останавливаем текущую играющую музыку (если есть), иначе они наложатся
                musicManager.stopPlaying();

                // Форсированно запускаем новую фоновую музыку
                musicManager.startPlaying(targetMusic);
                // Уведомление о смене музыки

                minecraft.getToasts().clear();
                minecraft.getToasts().addToast(new TerrariaMusicToast(entry.title()));
            }
        } else {
            // Если условие не выполнено, останавливаем музыку
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
}
