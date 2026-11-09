package com.minecraft_terraria.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Музыка биомов и событий в стиле Террарии: при смене обстановки старый трек НЕ обрывается,
 * а плавно затихает, пока новый плавно нарастает (кроссфейд).
 *
 * КАК ЭТО РАБОТАЕТ (коротко):
 *  1. В {@link SelectMusicEvent} мы всегда отдаём null — ванильный MusicManager вообще
 *     ничего не играет. Иначе он накладывался бы на нашу музыку (и он умеет только
 *     резко останавливать трек — никакого фейда в 1.21.1 у него нет).
 *  2. Раз в N тиков спрашиваем у {@link TriggerRegistry}, какая музыка сейчас уместна.
 *     Реестр читает config/minecraftterrariamod-music.json, так что набор ситуаций
 *     (деревня, ночь, дождь, пещера...) задаётся конфигом, а не кодом.
 *  3. Всю музыку мы играем сами через {@link SoundManager} объектами {@link FadeMusicSound},
 *     у которых громкость меняется каждый тик. Отсюда и плавные затухания.
 */
@EventBusSubscriber(modid = MinecraftTerrariamod.MODID, value = Dist.CLIENT)
public class MinecraftTerrariamodClient {

    // ==========================================================================
    //  МУЗЫКА ГЛАВНОГО МЕНЮ
    // ==========================================================================

    /** Имена треков меню (ключи в sounds.json / ModSounds). */
    private static final List<String> MENU_TRACKS = List.of("music.menu", "music.menu2", "music.menu3", "music.menu4");
    private static final Random RANDOM = new Random();
    /** Последний игравший трек меню — чтобы не выпадал два раза подряд. */
    private static String lastMenuTrack = null;
    /** Ключ "мы в главном меню" (id триггера так выглядеть не может). */
    private static final String MENU_KEY = "menu";

    private static String pickRandomMenuTrack() {
        String next;
        do {
            next = MENU_TRACKS.get(RANDOM.nextInt(MENU_TRACKS.size()));
        } while (MENU_TRACKS.size() > 1 && next.equals(lastMenuTrack));
        lastMenuTrack = next;
        return next;
    }

    // ==========================================================================
    //  СОСТОЯНИЕ КОНТРОЛЛЕРА МУЗЫКИ
    // ==========================================================================

    /** Трек, который играет (или только что начал нарастать) прямо сейчас. */
    private static FadeMusicSound currentTrack;
    /** Ключ активного трека: id триггера или MENU_KEY. */
    private static String currentKey;
    /** Сколько тиков затухает текущий трек — берём из его же триггера. */
    private static int currentFadeOutTicks = 50;
    /** Зациклен ли текущий трек (нужно, чтобы отличить "движок выбросил" от "доиграл"). */
    private static boolean currentLooping = true;
    /** Треки, которые сейчас дозатухают после смены обстановки. */
    private static final List<FadeMusicSound> fadingOutTracks = new ArrayList<>();
    /** Мир, к которому относится текущее состояние контроллера (для сброса при смене мира). */
    private static ClientLevel lastLevel;

    /**
     * Сбросить состояние при смене мира.
     *
     * Без этого при выходе в меню или создании нового мира остаются ссылки на треки
     * из прошлого мира и старый ключ: движок звука их уже выбросил, а контроллер
     * считает, что "этот трек уже играет", и не запускает нужный.
     */
    private static void resetIfLevelChanged(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == lastLevel) {
            return;
        }
        lastLevel = level;

        currentTrack = null;
        currentKey = null;
        currentLooping = true;
        currentFadeOutTicks = TriggerRegistry.defaultFadeOutTicks();
        fadingOutTracks.clear();
    }

    // ==========================================================================
    //  ОСНОВНАЯ ЛОГИКА
    // ==========================================================================

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        SoundManager soundManager = minecraft.getSoundManager();

        // --- 0. Сменился мир (или вышли в меню) — сбрасываем прошлое состояние ---
        resetIfLevelChanged(minecraft);

        // --- 1. Подтянуть конфиг (и подхватить правки на лету) ---
        TriggerRegistry.ensureLoaded();

        // --- 2. Убираем треки, которые уже дозатухли или доиграли ---
        fadingOutTracks.removeIf(track -> track.isStopped() || !soundManager.isActive(track));

        // --- 3. Следим за активным треком ---
        if (currentTrack != null) {
            if (soundManager.isActive(currentTrack)) {
                currentTrack.markPlaying();
            } else {
                boolean wasHeard = currentTrack.wasPlaying();
                currentTrack = null;

                if (!wasHeard || currentLooping) {
                    // Либо трек так и не заиграл, либо зацикленный трек выбросило движком —
                    // в обоих случаях сбрасываем ключ, чтобы контроллер запустил его заново.
                    currentKey = null;
                }
                // Не-зацикленный трек (например титры) просто доиграл до конца: ключ
                // оставляем, иначе он перезапускался бы бесконечно.
            }
        }

        boolean inWorld = minecraft.player != null && minecraft.level != null;

        // --- 4. Экран титров: отдельная ветка, он бывает и в мире, и из главного меню ---
        if (minecraft.screen instanceof WinScreen) {
            applyTrigger(minecraft, soundManager, TriggerRegistry.resolve(minecraft));
            return;
        }

        // --- 5. В мире условия проверяем раз в N тиков (в конфиге, по умолчанию 10) ---
        if (inWorld) {
            long interval = TriggerRegistry.checkIntervalTicks();
            if (minecraft.level.getGameTime() % interval != 0L) {
                return;
            }
            applyTrigger(minecraft, soundManager, TriggerRegistry.resolve(minecraft));
            return;
        }

        // --- 6. Не в мире: главное меню, проверяем каждый тик ---
        if (MENU_KEY.equals(currentKey) && currentTrack != null) {
            return;
        }
        if (currentTrack != null) {
            fadeOut(currentTrack, currentFadeOutTicks);
            currentTrack = null;
        }

        // В меню треки должны сменять друг друга -> не зацикливаем.
        startTrack(minecraft, soundManager, MENU_KEY, pickRandomMenuTrack(), null, false,
                TriggerRegistry.defaultFadeInTicks(), TriggerRegistry.defaultFadeOutTicks(), false);
    }

    /**
     * Привести музыку в соответствие с найденным триггером.
     *
     * @param trigger подходящий триггер или null, если сейчас музыки быть не должно
     */
    private static void applyTrigger(Minecraft minecraft, SoundManager soundManager, MusicTrigger trigger) {
        // Ни одно условие не подошло: плавно уводим текущую музыку в тишину
        if (trigger == null) {
            if (currentTrack != null) {
                fadeOut(currentTrack, currentFadeOutTicks);
                currentTrack = null;
            }
            currentKey = null;
            return;
        }

        // Уже играет то, что нужно — ничего не трогаем.
        // Для не-зацикленного трека, который уже доиграл, тоже ничего не делаем.
        if (trigger.id.equals(currentKey) && (currentTrack != null || !trigger.looping)) {
            return;
        }

        // Смена трека: старое затихает, новое нарастает (кроссфейд).
        // Гасим ровно столько, сколько новый трек будет нарастать, — так они
        // меняются синхронно и в сумме не «бубнят» два трека разом.
        if (currentTrack != null) {
            fadeOut(currentTrack, Math.min(currentFadeOutTicks, trigger.fadeInTicks));
            currentTrack = null;
        }

        startTrack(minecraft, soundManager, trigger.id, trigger.sound, trigger.title,
                trigger.looping, trigger.fadeInTicks, trigger.fadeOutTicks, true);
    }

    /**
     * Запустить трек и сделать его текущим.
     *
     * @param key         ключ для сравнения "что уже играет"
     * @param soundName   ключ трека в ModSounds/sounds.json
     * @param title       подпись для тоста (может быть null)
     * @param looping     true — играть бесконечно
     * @param fadeInTicks сколько тиков нарастать
     * @param fadeOutTicks сколько тиков затухать, когда придёт время уходить
     * @param showToast   показывать ли тост "сейчас играет"
     */
    private static void startTrack(Minecraft minecraft, SoundManager soundManager, String key, String soundName,
                                   String title, boolean looping, int fadeInTicks, int fadeOutTicks,
                                   boolean showToast) {
        SoundEvent soundEvent = ModSounds.getSoundEvent(soundName);
        if (soundEvent == null) {
            // Такое возможно только если трек убрали из ModSounds, а триггер в конфиге остался.
            // Не роняем игру, просто пропускаем.
            MinecraftTerrariamod.LOGGER.warn("[Музыка] Трек '{}' не зарегистрирован — пропускаю", soundName);
            currentTrack = null;
            currentKey = null;
            return;
        }

        FadeMusicSound track = new FadeMusicSound(soundEvent, 0.0F, looping);
        track.fadeTo(1.0F, fadeInTicks);
        soundManager.play(track);

        currentTrack = track;
        currentKey = key;
        currentFadeOutTicks = fadeOutTicks;
        currentLooping = looping;

        // Тост с названием трека показываем только для триггеров, не для меню
        if (showToast) {
            minecraft.getToasts().clear();
            minecraft.getToasts().addToast(new TerrariaMusicToast(title));
        }
    }

    /** Отправить трек в плавное затухание. */
    private static void fadeOut(FadeMusicSound track, int ticks) {
        track.fadeTo(0.0F, ticks);
        fadingOutTracks.add(track);
    }

    // ==========================================================================
    //  ЗАБИРАЕМ МУЗЫКУ У ВАНИЛЬНОГО MUSICMANAGER
    // ==========================================================================

    /**
     * Возвращаем null — это официально поддерживаемый способ сказать игре
     * "ничего не играй, музыку контролирую я".
     *
     * Проверено по исходникам 1.21.1: MusicManager.tick() при music == null вызывает
     * stopPlaying() и выходит, поэтому ванильная музыка действительно замолкает.
     */
    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        event.setMusic(null);
    }

    // ==========================================================================
    //  ТОСТ "СЕЙЧАС ИГРАЕТ"
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
