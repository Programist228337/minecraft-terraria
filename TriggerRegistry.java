package com.minecraft_terraria.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Реестр музыкальных триггеров.
 *
 * Всё содержимое живёт в файле config/minecraftterrariamod-music.json, который создаётся
 * автоматически при первом запуске. Чтобы добавить/убрать/переставить музыку — правите
 * JSON, перекомпилировать мод не нужно. Файл перечитывается на лету (следим за временем
 * изменения раз в пару секунд), так что достаточно сохранить его и вернуться в игру.
 *
 * Порядок разрешения: триггеры сортируются по priority (по убыванию), побеждает первый,
 * чьё условие выполнилось. При равном приоритете сохраняется порядок из файла.
 *
 * Схема приоритетов по умолчанию:
 * <pre>
 *   титры       200   экран финальных титров
 *   рейд        150   босс-бар рейда
 *   бой     125-140   warden / wither / trial chambers / mansion
 *   структуры   120   крепость (по блокам портала Края)
 *   под водой    60   голова в воде
 *   биомы-пещеры 55   lush_caves / dripstone_caves / deep_dark
 *   пещеры       50   ниже поверхности на depth блоков (или ниже y)
 *   океан+дождь  35   тег is_ocean и осадки
 *   гроза        30   только под открытым небом
 *   дождь        25   только под открытым небом
 *   деревня   22-24   жители рядом (НИЖЕ погоды)
 *   ночь         20   время суток
 *   прочие биомы  0   фон
 * </pre>
 * Правило простое: бой перебивает всё, обстановка перебивает биом, пещера перебивает
 * погоду и время суток, а погода перебивает деревню — они глобальные для мира,
 * а игрок в этот момент может быть под землёй.
 * Все "обстановочные" триггеры ограничены Верхним миром через onlyInDimension, чтобы
 * не подменять музыку биомов Нижнего мира и Энда.
 *
 * Единственное, что нельзя сделать через JSON, — добавить НОВЫЙ звук: сам трек должен быть
 * зарегистрирован в {@link ModSounds} и лежать в sounds.json + .ogg. Зато навесить на уже
 * зарегистрированный трек сколько угодно триггеров можно в любой момент.
 */
public final class TriggerRegistry {

    private TriggerRegistry() {
    }

    // ==========================================================================
    //  Структура файла конфигурации
    // ==========================================================================

    /** Общие настройки плавного перехода. */
    public static final class Crossfade {
        /** Сколько тиков новый трек нарастает (20 тиков = 1 секунда). */
        public int fadeInTicks = 60;
        /** Сколько тиков старый трек затихает. */
        public int fadeOutTicks = 50;
        /** Как часто проверять условия, в тиках. */
        public int checkIntervalTicks = 10;

        public Crossfade() {
        }
    }

    /** Корень JSON-файла. */
    public static final class Config {
        /** Версия схемы. Нужна, чтобы старый конфиг автоматически обновился до новых приоритетов. */
        public int configVersion = CURRENT_CONFIG_VERSION;

        /** Подсказка прямо в файле — Gson пишет её как обычное поле. */
        public List<String> _help = List.of(
                "Условия (condition): always, day, night, rain, thunder, village, raid, wither, underground, underwater, biome, biome_tag, dimension, entity, block, structure, credits",
                "Несколько условий сразу: \"conditions\": [\"village\", \"night\"] — все должны выполниться.",
                "priority: чем больше число, тем раньше проверяется. Побеждает первый подошедший триггер.",
                "Порядок по умолчанию (сверху — важнее): титры 200, рейд 150, бой 125-140, структуры 120, вода 60, пещеры 50, океан+дождь 35, гроза 30, дождь 25, деревня 22-24, ночь 20, биом 0.",
                "Дождь и гроза срабатывают только под открытым небом (isRainingAt), поэтому в пещере их нет.",
                "condition=wither ловит настоящего Визера по полоске HP, визер-скелет под него не попадает.",
                "fadeInTicks / fadeOutTicks: 0 = взять значение из блока \"crossfade\".",
                "looping: true — трек играет бесконечно, false — один раз.",
                "Для condition=biome добавьте \"biome\": \"minecraft:forest\".",
                "Для condition=biome_tag добавьте \"biomeTag\": \"minecraft:is_ocean\".",
                "Для condition=dimension добавьте \"dimension\": \"minecraft:the_nether\".",
                "Для condition=entity добавьте \"entities\": [\"minecraft:warden\"], работает с \"radius\".",
                "Для condition=block добавьте \"blocks\": [\"minecraft:end_portal_frame\"], радиус максимум 24.",
                "Для condition=structure добавьте \"blocks\": [...] — блоки-маркеры постройки, \"radius\" (14) и \"minCount\" (24) — сколько их должно быть вокруг.",
                "Для condition=village можно задать \"radius\" (по умолчанию 32).",
                "Для condition=underground: \"depth\" — на сколько блоков ниже поверхности (8), \"y\" — абсолютный порог (50).",
                "onlyInDimension — необязательный фильтр для любого условия, например \"minecraft:overworld\".",
                "sound должен быть треком, зарегистрированным в ModSounds.",
                "Файл перечитывается на лету: сохранили — через пару секунд применилось."
        );

        public Crossfade crossfade = new Crossfade();

        public List<MusicTrigger> triggers = new ArrayList<>();

        public Config() {
        }
    }

    // ==========================================================================
    //  Состояние
    // ==========================================================================

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String FILE_NAME = "minecraftterrariamod-music.json";

    /**
     * Версия схемы конфига.
     *
     * Поднимайте при изменении приоритетов/условий по умолчанию: при запуске старый файл
     * будет слит с новыми умолчаниями (пользовательские триггеры сохраняются), а копия
     * старого файла останется рядом с суффиксом .v{старая версия}.bak.
     *
     * 1 -> 2: триггеры боя подняты наверх, дождь/гроза только под открытым небом,
     *         пещера считается от поверхности, а не от фиксированного Y.
     * 2 -> 3: деревня опущена ниже погоды, визер определяется как босс по полоске HP.
     * 3 -> 4: крепость определяется по рамкам портала, а не по блоку портала.
     * 4 -> 5: крепость и залы испытаний ловятся как структура по плотности блоков —
     *         музыка звучит по всему данжу, а не только рядом с порталом/мобами.
     */
    private static final int CURRENT_CONFIG_VERSION = 5;

    /** Как часто проверять, не изменился ли файл на диске. */
    private static final long RELOAD_CHECK_INTERVAL_MS = 2000L;

    /**
     * Порядок проверки: чем выше priority, тем раньше. Компаратор стабильный,
     * поэтому при равном приоритете сохраняется порядок из файла.
     */
    private static final Comparator<MusicTrigger> BY_PRIORITY_DESC =
            Comparator.comparingInt((MusicTrigger t) -> t.priority).reversed();

    private static Config config;
    private static long configLastModified = -1L;
    private static long lastReloadCheck = 0L;

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    // ==========================================================================
    //  Загрузка / горячая перезагрузка
    // ==========================================================================

    /** Гарантирует, что конфиг загружен, и подхватывает изменения файла на диске. */
    public static synchronized void ensureLoaded() {
        if (config == null) {
            load();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastReloadCheck < RELOAD_CHECK_INTERVAL_MS) {
            return;
        }
        lastReloadCheck = now;

        try {
            Path path = configPath();
            long modified = Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1L;
            if (modified != configLastModified) {
                MinecraftTerrariamod.LOGGER.info("[Музыка] Конфиг триггеров изменился — перечитываю.");
                load();
            }
        } catch (Exception e) {
            MinecraftTerrariamod.LOGGER.warn("[Музыка] Не удалось проверить конфиг триггеров", e);
        }
    }

    /** Принудительно перечитать файл с диска (команда /terrariamusic reload). */
    public static synchronized void forceReload() {
        load();
    }

    private static void load() {
        try {
            Path path = configPath();
            boolean needsSave = false;

            if (Files.exists(path)) {
                Config parsed;
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    parsed = GSON.fromJson(reader, Config.class);
                }

                if (parsed == null) {
                    config = defaultConfig();
                    needsSave = true;
                } else if (parsed.configVersion < CURRENT_CONFIG_VERSION) {
                    config = migrate(parsed, path);
                    needsSave = true;
                } else {
                    config = parsed;
                }
            } else {
                config = defaultConfig();
                needsSave = true;
            }

            if (needsSave) {
                save();
            }

            configLastModified = Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1L;
            lastReloadCheck = System.currentTimeMillis();
            normalize();
        } catch (Exception e) {
            MinecraftTerrariamod.LOGGER.error("[Музыка] Ошибка чтения конфига триггеров — беру настройки по умолчанию", e);
            config = defaultConfig();
            normalize();
        }
    }

    /**
     * Слить старый конфиг с новыми умолчаниями.
     *
     * Триггеры с известными id (то есть все, что поставляются модом) заменяются свежими —
     * именно так обновляются приоритеты и условия. Триггеры с незнакомыми id считаются
     * добавленными пользователем и сохраняются как есть.
     */
    private static Config migrate(Config old, Path path) {
        Config fresh = defaultConfig();

        Map<String, MusicTrigger> merged = new LinkedHashMap<>();
        for (MusicTrigger trigger : fresh.triggers) {
            merged.put(trigger.id, trigger);
        }

        int custom = 0;
        if (old.triggers != null) {
            for (MusicTrigger trigger : old.triggers) {
                if (trigger == null || trigger.id == null || trigger.id.isBlank()) {
                    continue;
                }
                if (!merged.containsKey(trigger.id)) {
                    merged.put(trigger.id, trigger);
                    custom++;
                }
            }
        }

        fresh.triggers = new ArrayList<>(merged.values());
        fresh.triggers.sort(BY_PRIORITY_DESC);

        backup(path, old.configVersion);

        MinecraftTerrariamod.LOGGER.warn(
                "[Музыка] Конфиг обновлён с версии {} до {}: приоритеты и условия пересобраны, "
                        + "пользовательских триггеров сохранено: {}. Копия старого файла — с суффиксом .v{}.bak",
                old.configVersion, CURRENT_CONFIG_VERSION, custom, old.configVersion);
        return fresh;
    }

    private static void backup(Path path, int version) {
        try {
            Path backup = path.resolveSibling(path.getFileName() + ".v" + version + ".bak");
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            MinecraftTerrariamod.LOGGER.info("[Музыка] Копия старого конфига: {}", backup);
        } catch (IOException e) {
            MinecraftTerrariamod.LOGGER.warn("[Музыка] Не удалось сохранить копию старого конфига", e);
        }
    }

    private static void save() {
        try {
            Path path = configPath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            MinecraftTerrariamod.LOGGER.info("[Музыка] Создан конфиг триггеров: {}", path);
        } catch (IOException e) {
            MinecraftTerrariamod.LOGGER.error("[Музыка] Не удалось записать конфиг триггеров", e);
        }
    }

    /** Чистит и валидирует загруженный конфиг: битые записи отбрасываются с понятным логом. */
    private static void normalize() {
        if (config.crossfade == null) {
            config.crossfade = new Crossfade();
        }
        config.crossfade.fadeInTicks = Math.max(1, config.crossfade.fadeInTicks);
        config.crossfade.fadeOutTicks = Math.max(1, config.crossfade.fadeOutTicks);
        config.crossfade.checkIntervalTicks = Math.max(1, config.crossfade.checkIntervalTicks);

        if (config.triggers == null) {
            config.triggers = new ArrayList<>();
        }

        List<MusicTrigger> valid = new ArrayList<>();
        for (MusicTrigger trigger : config.triggers) {
            if (trigger == null) {
                continue;
            }

            if (trigger.conditionTypes() == null) {
                MinecraftTerrariamod.LOGGER.warn("[Музыка] Триггер '{}': неизвестное условие (condition={}, conditions={}) — пропускаю. Доступны: {}",
                        trigger.id, trigger.condition, trigger.conditions, TriggerCondition.names());
                continue;
            }

            String paramError = validateParams(trigger);
            if (paramError != null) {
                MinecraftTerrariamod.LOGGER.warn("[Музыка] Триггер '{}': {} — пропускаю.", trigger.id, paramError);
                continue;
            }

            if (!ModSounds.isKnownTrack(trigger.sound)) {
                MinecraftTerrariamod.LOGGER.warn("[Музыка] Триггер '{}': трек '{}' не зарегистрирован — пропускаю. Известные треки: {}",
                        trigger.id, trigger.sound, ModSounds.TRACKS);
                continue;
            }

            if (trigger.id == null || trigger.id.isBlank()) {
                trigger.id = trigger.condition + ":" + trigger.sound;
            }
            if (trigger.title == null || trigger.title.isBlank()) {
                trigger.title = trigger.sound;
            }
            if (trigger.fadeInTicks <= 0) {
                trigger.fadeInTicks = config.crossfade.fadeInTicks;
            }
            if (trigger.fadeOutTicks <= 0) {
                trigger.fadeOutTicks = config.crossfade.fadeOutTicks;
            }
            if (trigger.radius <= 0) {
                trigger.radius = 32;
            }
            if (trigger.depth <= 0) {
                trigger.depth = 8;
            }

            valid.add(trigger);
        }

        // Стабильная сортировка: при равном priority порядок из файла сохраняется.
        valid.sort(BY_PRIORITY_DESC);
        config.triggers = valid;

        MinecraftTerrariamod.LOGGER.info("[Музыка] Загружено триггеров: {} (проверка каждые {} тиков)",
                valid.size(), config.crossfade.checkIntervalTicks);
    }

    /**
     * Проверяет, что у условий заданы все нужные параметры.
     *
     * @return текст ошибки или null, если всё в порядке
     */
    private static String validateParams(MusicTrigger trigger) {
        for (TriggerCondition type : trigger.conditionTypes()) {
            switch (type) {
                case BIOME:
                    if (trigger.biome == null || trigger.biome.isBlank()) {
                        return "для condition=biome не задан \"biome\"";
                    }
                    break;
                case BIOME_TAG:
                    if (trigger.biomeTagKey() == null) {
                        return "для condition=biome_tag не задан или неверен \"biomeTag\"";
                    }
                    break;
                case DIMENSION:
                    if (trigger.dimension == null || trigger.dimension.isBlank()) {
                        return "для condition=dimension не задан \"dimension\"";
                    }
                    break;
                case ENTITY:
                    if (trigger.entityIds().isEmpty()) {
                        return "для condition=entity пустой или неверный список \"entities\"";
                    }
                    break;
                case BLOCK:
                    if (trigger.blockSet().isEmpty()) {
                        return "для condition=block пустой или неверный список \"blocks\"";
                    }
                    break;
                case STRUCTURE:
                    if (trigger.blockSet().isEmpty()) {
                        return "для condition=structure пустой или неверный список \"blocks\"";
                    }
                    if (trigger.minCount < 1) {
                        return "для condition=structure \"minCount\" должен быть >= 1";
                    }
                    break;
                default:
                    break;
            }
        }
        return null;
    }

    // ==========================================================================
    //  Публичный API
    // ==========================================================================

    /**
     * Первый (самый приоритетный) триггер, чьё условие выполнилось прямо сейчас.
     * Возвращает null, если ни один не подошёл — значит, музыки в этом месте нет.
     */
    public static MusicTrigger resolve(Minecraft minecraft) {
        ensureLoaded();
        for (MusicTrigger trigger : config.triggers) {
            if (trigger.test(minecraft)) {
                return trigger;
            }
        }
        return null;
    }

    public static int defaultFadeInTicks() {
        ensureLoaded();
        return config.crossfade.fadeInTicks;
    }

    public static int defaultFadeOutTicks() {
        ensureLoaded();
        return config.crossfade.fadeOutTicks;
    }

    public static int checkIntervalTicks() {
        ensureLoaded();
        return config.crossfade.checkIntervalTicks;
    }

    /** Все триггеры в порядке проверки (после сортировки по приоритету). Только для отладки. */
    public static List<MusicTrigger> triggers() {
        ensureLoaded();
        return config.triggers;
    }

    // ==========================================================================
    //  Конфигурация по умолчанию
    // ==========================================================================

    private static Config defaultConfig() {
        Config cfg = new Config();
        cfg.configVersion = CURRENT_CONFIG_VERSION;

        // ================= Титры =================
        // Единственный триггер без привязки к миру: экран титров бывает и из главного меню,
        // поэтому onlyInDimension здесь НЕ ставим.
        MusicTrigger credits = new MusicTrigger();
        credits.id = "credits";
        credits.priority = 200;
        credits.condition = "credits";
        credits.sound = "music.titres";
        credits.title = "Credits";
        credits.looping = false;
        cfg.triggers.add(credits);

        // ================= Сражения =================
        // Бой всегда перебивает всё остальное: и деревню, и погоду, и биом.
        // Рейд определяется по босс-бару (см. RaidWatcher). Отдельного "рейдового" трека
        // в наборе нет, поэтому стоит самый "иллагерский" бой — поменяйте строку ниже,
        // если положите свой raid.ogg.
        MusicTrigger raid = special("raid", 150, "raid", "music.woodland_mansion_battle", "Raid theme");
        raid.fadeInTicks = 25;
        cfg.triggers.add(raid);

        cfg.triggers.add(entityTrigger("warden_battle", 140, "music.warden_battle", "Warden battle",
                48, true, "minecraft:warden"));
        // Визер ловится как БОСС — по полоске здоровья, а не как обычная сущность.
        // Так он не путается с визер-скелетом (у того другой id и нет босс-бара).
        // В Нижнем мире его тоже можно призвать, поэтому фильтра по измерению нет.
        MusicTrigger wither = new MusicTrigger();
        wither.id = "wither_battle";
        wither.priority = 135;
        wither.condition = "wither";
        wither.radius = 64;
        wither.sound = "music.wither_battle";
        wither.title = "Wither battle";
        wither.fadeInTicks = 25;
        cfg.triggers.add(wither);
        // Залы испытаний ловятся как СТРУКТУРА (см. блок "Структуры" ниже): музыка должна
        // звучать по всему данжу, а не только пока рядом бриз или боггед.
        cfg.triggers.add(entityTrigger("woodland_mansion_battle", 125, "music.woodland_mansion_battle",
                "Woodland mansion battle", 32, true,
                "minecraft:evoker", "minecraft:vindicator", "minecraft:vex"));

        // ================= Структуры =================
        // Клиенту данные о структурах не передаются вообще (в ClientboundLevelChunkPacketData
        // их нет), поэтому структуру опознаём по её "подписи" — блокам, которые встречаются
        // только у неё. Условие "structure" считает ПЛОТНОСТЬ таких блоков вокруг игрока,
        // поэтому музыка играет по всему данжу, а не только в зале с порталом.
        //
        // Крепость целиком сложена каменным кирпичом (включая заражённый и резной).
        cfg.triggers.add(structureTrigger("stronghold", 120, "music.stronghold", "Stronghold",
                STRONGHOLD_BLOCKS, 14, 24));

        // Залы испытаний: туфовый кирпич, резной и полированный туф, спавнеры и хранилища.
        // Раньше этот триггер срабатывал только когда рядом бриз или боггед, поэтому
        // в пустом данже (и в коридорах между залами) музыки не было вовсе.
        cfg.triggers.add(structureTrigger("trial_chambers_battle", 130, "music.trial_chambers_battle",
                "Trial chambers", TRIAL_CHAMBERS_BLOCKS, 14, 24));

        // ================= Обстановка =================
        // Деревня стоит НИЖЕ погоды: если пошёл дождь или началась гроза, звучит погода,
        // а не музыка жителей. Ночь в деревне — отдельный трек (два условия через И).
        cfg.triggers.add(conditionsTrigger("village_night", 24, "music.night_village",
                "Village at night", true, "village", "night"));
        cfg.triggers.add(special("village", 22, "village", "music.village", "Village theme"));

        // Дождь над любым океаном: тег биома (все океаны сразу) + дождь.
        // Это уточнение дождя, поэтому стоит чуть выше обычной грозы.
        MusicTrigger oceanRain = conditionsTrigger("ocean_rain", 35, "music.ocean_rain",
                "Ocean rain", true, "biome_tag", "rain");
        oceanRain.biomeTag = "minecraft:is_ocean";
        cfg.triggers.add(oceanRain);

        cfg.triggers.add(special("underwater", 60, "underwater", "music.underwater", "Underwater theme"));
        // Пещеры стоят ВЫШЕ погоды и ночи: дождь и время суток — глобальные для мира,
        // а игрок в этот момент может сидеть под землёй, где ни дождя, ни неба нет.
        // Биомы-пещеры (lush_caves / dripstone_caves / deep_dark) идут ещё выше (55).
        cfg.triggers.add(special("underground", 50, "underground", "music.cave", "Cave theme"));
        cfg.triggers.add(special("thunder", 30, "thunder", "music.thunder", "Thunderstorm theme"));
        cfg.triggers.add(special("rain", 25, "rain", "music.rain", "Rain theme"));
        cfg.triggers.add(special("night", 20, "night", "music.night", "Night theme"));

        // ================= Биомы: самый низкий приоритет, работают как "фон" =================
        for (String[] biome : DEFAULT_BIOMES) {
            MusicTrigger trigger = new MusicTrigger();
            trigger.id = "biome:" + biome[0];
            // Биомы-пещеры получают приоритет выше, чем триггер "underground" (50),
            // иначе их собственная музыка никогда бы не зазвучала.
            trigger.priority = CAVE_BIOMES.contains(biome[0]) ? 55 : 0;
            trigger.condition = "biome";
            trigger.biome = biome[0];
            trigger.sound = biome[1];
            trigger.title = biome[2];
            cfg.triggers.add(trigger);
        }

        // Единственный биом с отдельным треком, которого нет в общем списке выше.
        MusicTrigger voidBiome = new MusicTrigger();
        voidBiome.id = "void";
        voidBiome.priority = 0;
        voidBiome.condition = "biome";
        voidBiome.biome = "minecraft:the_void";
        voidBiome.sound = "music.void";
        voidBiome.title = "Void theme";
        cfg.triggers.add(voidBiome);

        // Сортируем сразу, чтобы файл читался в порядке проверки (а не в порядке добавления).
        cfg.triggers.sort(BY_PRIORITY_DESC);
        return cfg;
    }

    private static final String OVERWORLD = "minecraft:overworld";

    private static MusicTrigger special(String id, int priority, String condition, String sound, String title) {
        MusicTrigger trigger = new MusicTrigger();
        trigger.id = id;
        trigger.priority = priority;
        trigger.condition = condition;
        trigger.sound = sound;
        trigger.title = title;
        // Все "обстановочные" триггеры — только для Верхнего мира.
        trigger.onlyInDimension = OVERWORLD;
        return trigger;
    }

    /** Триггер с несколькими условиями: все должны выполниться одновременно. */
    private static MusicTrigger conditionsTrigger(String id, int priority, String sound, String title,
                                                  boolean overworldOnly, String... conditions) {
        MusicTrigger trigger = new MusicTrigger();
        trigger.id = id;
        trigger.priority = priority;
        trigger.conditions = List.of(conditions);
        trigger.sound = sound;
        trigger.title = title;
        if (overworldOnly) {
            trigger.onlyInDimension = OVERWORLD;
        }
        return trigger;
    }

    /** Триггер "рядом есть сущность такого-то типа". */
    private static MusicTrigger entityTrigger(String id, int priority, String sound, String title,
                                              int radius, boolean overworldOnly, String... entityIds) {
        MusicTrigger trigger = new MusicTrigger();
        trigger.id = id;
        trigger.priority = priority;
        trigger.condition = "entity";
        trigger.entities = List.of(entityIds);
        trigger.radius = radius;
        trigger.sound = sound;
        trigger.title = title;
        // Бой вступает быстрее общего кроссфейда (60 тиков = 3 с): пока моб уже бьёт,
        // три секунды тишины выглядят как "музыка не работает".
        trigger.fadeInTicks = 25;
        if (overworldOnly) {
            trigger.onlyInDimension = OVERWORLD;
        }
        return trigger;
    }

    /** Триггер "игрок внутри постройки из таких-то блоков". */
    private static MusicTrigger structureTrigger(String id, int priority, String sound, String title,
                                                 List<String> signature, int radius, int minCount) {
        MusicTrigger trigger = new MusicTrigger();
        trigger.id = id;
        trigger.priority = priority;
        trigger.condition = "structure";
        trigger.blocks = signature;
        trigger.radius = radius;
        trigger.minCount = minCount;
        trigger.sound = sound;
        trigger.title = title;
        // Обе структуры ванильные и находятся в Верхнем мире.
        trigger.onlyInDimension = OVERWORLD;
        return trigger;
    }

    /** Блоки, из которых целиком сложена крепость (Stronghold). */
    private static final List<String> STRONGHOLD_BLOCKS = List.of(
            "minecraft:stone_bricks",
            "minecraft:mossy_stone_bricks",
            "minecraft:cracked_stone_bricks",
            "minecraft:chiseled_stone_bricks",
            "minecraft:infested_stone_bricks",
            "minecraft:infested_mossy_stone_bricks",
            "minecraft:infested_cracked_stone_bricks",
            "minecraft:infested_chiseled_stone_bricks",
            "minecraft:stone_brick_stairs",
            "minecraft:stone_brick_slab",
            "minecraft:stone_brick_wall"
    );

    /**
     * Блоки-подписи залов испытаний (Trial Chambers).
     * В природе таких сочетаний не бывает: обычный туф генерируется, а кирпич из него,
     * резной и полированный — только в этой структуре.
     */
    private static final List<String> TRIAL_CHAMBERS_BLOCKS = List.of(
            "minecraft:tuff_bricks",
            "minecraft:chiseled_tuff",
            "minecraft:chiseled_tuff_bricks",
            "minecraft:polished_tuff",
            "minecraft:tuff_brick_stairs",
            "minecraft:tuff_brick_slab",
            "minecraft:tuff_brick_wall",
            "minecraft:trial_spawner",
            "minecraft:vault"
    );

    /** Биомы, у которых есть своя музыка и которые при этом находятся под землёй. */
    private static final Set<String> CAVE_BIOMES = Set.of(
            "minecraft:lush_caves",
            "minecraft:dripstone_caves",
            "minecraft:deep_dark"
    );

    /** {id биома, ключ трека, подпись} — отсюда генерируются биомные триггеры по умолчанию. */
    private static final String[][] DEFAULT_BIOMES = {
            {"minecraft:forest", "music.forest", "Forest theme"},
            {"minecraft:desert", "music.desert", "Desert theme"},
            {"minecraft:ocean", "music.ocean", "Ocean theme"},
            {"minecraft:flower_forest", "music.flower_forest", "Flower Forest theme"},
            {"minecraft:badlands", "music.badlands", "Badlands theme"},
            {"minecraft:bamboo_jungle", "music.bamboo_jungle", "Bamboo theme"},
            {"minecraft:beach", "music.beach", "Beach theme"},
            {"minecraft:birch_forest", "music.birch_forest", "Birch theme"},
            {"minecraft:cherry_grove", "music.cherry_grove", "Cherry grove theme"},
            {"minecraft:cold_ocean", "music.cold_ocean", "Cold ocean theme"},
            {"minecraft:crimson_forest", "music.crimson_forest", "Crimson theme"},
            {"minecraft:dark_forest", "music.dark_forest", "Dark forest theme"},
            {"minecraft:deep_cold_ocean", "music.deep_cold_ocean", "Deep cold ocean theme"},
            {"minecraft:deep_dark", "music.deep_dark", "Deep dark theme"},
            {"minecraft:deep_frozen_ocean", "music.deep_frozen_ocean", "Deep frozen ocean theme"},
            {"minecraft:deep_lukewarm_ocean", "music.deep_lukewarm_ocean", "Deep lukewarm ocean theme"},
            {"minecraft:deep_ocean", "music.deep_ocean", "Deep ocean theme"},
            {"minecraft:dripstone_caves", "music.dripstone_caves", "Dripstone caves theme"},
            {"minecraft:end_barrens", "music.end_barrens", "End barrens theme"},
            {"minecraft:end_highlands", "music.end_highlands", "End highlands theme"},
            {"minecraft:end_midlands", "music.end_midlands", "End midlands theme"},
            {"minecraft:eroded_badlands", "music.eroded_badlands", "Eroded badlands theme"},
            {"minecraft:frozen_ocean", "music.frozen_ocean", "Frozen ocean theme"},
            {"minecraft:frozen_peaks", "music.frozen_peaks", "Frozen peaks theme"},
            {"minecraft:frozen_river", "music.frozen_river", "Frozen river theme"},
            {"minecraft:ice_spikes", "music.ice_spikes", "Ice spikes theme"},
            {"minecraft:jagged_peaks", "music.jagged_peaks", "Jagged peaks theme"},
            {"minecraft:jungle", "music.jungle", "Jungle theme"},
            {"minecraft:lukewarm_ocean", "music.lukewarm_ocean", "Lukewarm ocean theme"},
            {"minecraft:lush_caves", "music.lush_caves", "Lush caves theme"},
            {"minecraft:mangrove_swamp", "music.mangrove_swamp", "Mangrove swamp theme"},
            {"minecraft:grove", "music.mountain_grove", "Mountain grove theme"},
            {"minecraft:meadow", "music.mountain_meadow", "Mountain meadow theme"},
            {"minecraft:mushroom_fields", "music.mushroom_fields", "Mushroom fields theme"},
            {"minecraft:nether_wastes", "music.nether_wastes", "Nether wastes theme"},
            {"minecraft:old_growth_birch_forest", "music.old_growth_birch_forest", "Old growth birch forest theme"},
            {"minecraft:old_growth_pine_taiga", "music.old_growth_pine_taiga", "Old growth pine taiga theme"},
            {"minecraft:old_growth_spruce_taiga", "music.old_growth_spruce_taiga", "Old growth spruce taiga theme"},
            {"minecraft:plains", "music.plains", "Plains theme"},
            {"minecraft:river", "music.river", "River theme"},
            {"minecraft:savanna", "music.savanna", "Savanna theme"},
            {"minecraft:savanna_plateau", "music.savanna_plateau", "Savanna plateau theme"},
            {"minecraft:small_end_islands", "music.small_end_islands", "Small end islands theme"},
            {"minecraft:snowy_beach", "music.snowy_beach", "Snowy beach theme"},
            {"minecraft:snowy_plains", "music.snowy_plains", "Snowy plains theme"},
            {"minecraft:snowy_slopes", "music.snowy_slopes", "Snowy slopes theme"},
            {"minecraft:snowy_taiga", "music.snowy_taiga", "Snowy taiga theme"},
            {"minecraft:soul_sand_valley", "music.soul_sand_valley", "Soul sand valley theme"},
            {"minecraft:sparse_jungle", "music.sparse_jungle", "Sparse jungle theme"},
            {"minecraft:stony_peaks", "music.stony_peaks", "Stony peaks theme"},
            {"minecraft:stony_shore", "music.stony_shore", "Stony shore theme"},
            {"minecraft:sunflower_plains", "music.sunflower_plains", "Sunflower plains theme"},
            {"minecraft:swamp", "music.swamp", "Swamp theme"},
            {"minecraft:taiga", "music.taiga", "Taiga theme"},
            {"minecraft:the_end", "music.the_end", "The end theme"},
            {"minecraft:warm_ocean", "music.warm_ocean", "Warm ocean theme"},
            {"minecraft:warped_forest", "music.warped_forest", "Warped forest theme"},
            {"minecraft:windswept_hills", "music.windswept_hills", "Windswept hills theme"},
            {"minecraft:windswept_forest", "music.windswept_forest", "Windswept forest theme"},
            {"minecraft:windswept_gravelly_hills", "music.windswept_gravelly_hills", "Windswept gravelly hills theme"},
            {"minecraft:windswept_savanna", "music.windswept_savanna", "Windswept savanna theme"},
            {"minecraft:wooded_badlands", "music.wooded_badlands", "Wooded badlands theme"},
            {"minecraft:basalt_deltas", "music.basalt_deltas", "Basalt deltas theme"},
    };
}
