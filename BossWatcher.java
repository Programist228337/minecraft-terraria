package com.minecraft_terraria.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.BossEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Определяет, какие боссы сейчас активны, по их полоскам здоровья.
 *
 * Почему через босс-бары, а не через сущности: рейд существует только на сервере
 * (у Level/ClientLevel нет getRaidAt(), а Raider.hasActiveRaid() на клиенте всегда false),
 * а "настоящего" босса от его же мобов проще всего отличить именно по полоске HP.
 *
 * Опознаём по КЛЮЧУ ПЕРЕВОДА имени, а не по цвету: цвет у визера (PURPLE) и дракона
 * одинаковый, а ключи разные и не зависят от языка игры:
 *   Raid          -> "event.minecraft.raid"
 *   WitherBoss    -> "entity.minecraft.wither"
 *   EnderDragon   -> "entity.minecraft.ender_dragon"
 *
 * Событие отрисовки приходит каждый кадр, пока бар виден, поэтому запоминаем тик
 * последнего показа и считаем босса активным ещё пару секунд после пропадания бара.
 *
 * Ограничение: при скрытом HUD (F1) бары не рисуются и событие не приходит.
 */
@EventBusSubscriber(modid = MinecraftTerrariamod.MODID, value = Dist.CLIENT)
public final class BossWatcher {

    /** Виды боссов, которые нам интересны. */
    public enum Kind {
        RAID,
        WITHER,
        DRAGON,
        /** Бар есть, но это не ванильный босс (модовый или с кастомным именем). */
        OTHER
    }

    /** Сколько тиков после последнего показа бара считаем, что босс ещё активен. */
    private static final int TIMEOUT_TICKS = 40;

    /** Тик игрового времени, когда бар каждого вида видели в последний раз. */
    private static final Map<Kind, Long> LAST_SEEN = new EnumMap<>(Kind.class);

    /**
     * Мир, для которого собраны отметки LAST_SEEN.
     *
     * КРИТИЧНО: gameTime у каждого мира свой и в новом мире начинается с нуля.
     * Если не сбрасывать отметки при смене мира, получается отрицательная разница
     * (0 - 30000), она меньше таймаута — и "рейд" считается активным ещё десятки минут
     * после создания нового мира. Ровно этот баг и ловили.
     */
    private static ClientLevel watchedLevel;

    private BossWatcher() {
    }

    /**
     * Привязать отметки к текущему миру. При смене мира (или выходе в меню) — очистить.
     *
     * @return true, если мир есть и отметки относятся именно к нему
     */
    private static boolean syncLevel(Minecraft minecraft) {
        ClientLevel level = minecraft.level;

        if (level == null) {
            if (watchedLevel != null) {
                LAST_SEEN.clear();
                watchedLevel = null;
            }
            return false;
        }

        if (level != watchedLevel) {
            LAST_SEEN.clear();
            watchedLevel = level;
        }
        return true;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!syncLevel(minecraft)) {
            return;
        }
        LAST_SEEN.put(classify(event.getBossEvent()), minecraft.level.getGameTime());
    }

    /** Что это за бар. Никогда не возвращает null — неопознанное помечается как OTHER. */
    private static Kind classify(BossEvent bar) {
        String key = translationKey(bar.getName());
        if (key != null) {
            return switch (key) {
                case "event.minecraft.raid" -> Kind.RAID;
                case "entity.minecraft.wither" -> Kind.WITHER;
                case "entity.minecraft.ender_dragon" -> Kind.DRAGON;
                default -> Kind.OTHER;
            };
        }

        // Имя — литерал (кастомное или из мода). Рейд ещё можно узнать по внешнему виду:
        // Raid создаёт ServerBossEvent(RED, NOTCHED_10), других таких ванильных баров нет.
        if (bar.getColor() == BossEvent.BossBarColor.RED
                && bar.getOverlay() == BossEvent.BossBarOverlay.NOTCHED_10) {
            return Kind.RAID;
        }
        return Kind.OTHER;
    }

    private static String translationKey(Component name) {
        if (name != null && name.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return null;
    }

    /** Активен ли босс этого вида прямо сейчас. */
    public static boolean isActive(Minecraft minecraft, Kind kind) {
        if (!syncLevel(minecraft)) {
            return false;
        }

        Long seen = LAST_SEEN.get(kind);
        if (seen == null) {
            return false;
        }

        long delta = minecraft.level.getGameTime() - seen;
        // delta < 0 — время ушло назад (сменился мир); такую отметку не считаем активной.
        return delta >= 0L && delta < TIMEOUT_TICKS;
    }
}
