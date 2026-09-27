package com.minecraft_terraria.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = MinecraftTerrariamod.MODID, value = Dist.CLIENT)
public class MinecraftTerrariamodClient {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        // Обязательно проверяем, что игрок в мире, чтобы избежать NullPointerException
        if (minecraft.player == null || minecraft.level == null) return;

        // Вставьте ваше заданное условие (например, игрок вошел в определенную зону или структуру)
        if (minecraft.player.blockPosition().equals(new BlockPos(0, 64, 0))) {
            MusicManager musicManager = minecraft.getMusicManager();

            Music targetMusic = ModSounds.getMusic("music.forest");

            // Проверяем, не играет ли уже эта конкретная музыка, чтобы не перезапускать её каждый тик
            if (!musicManager.isPlayingMusic(targetMusic)) {
                // Останавливаем текущую играющую музыку (если есть), иначе они наложатся
                musicManager.stopPlaying();

                // Форсированно запускаем новую фоновую музыку
                musicManager.startPlaying(targetMusic);
                // Уведомление о смене музыки
                minecraft.getToasts().clear();
                minecraft.getToasts().addToast(new TerrariaMusicToast("music forest"));
            }
        } else {
            // Если условие не выполнено, останавливаем музыку
            MusicManager musicManager = minecraft.getMusicManager();
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
