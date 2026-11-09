package com.minecraft_terraria.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Один триггер: "при таких-то условиях играть такой-то трек".
 *
 * Это чистый DTO для Gson — он читается из config/minecraftterrariamod-music.json,
 * поэтому все поля публичные, а имена полей в JSON совпадают с именами в классе.
 *
 * Простое условие (одно):
 * <pre>
 * { "id": "night", "priority": 15, "condition": "night", "sound": "music.night" }
 * </pre>
 *
 * Несколько условий сразу (все должны выполниться, логическое И):
 * <pre>
 * {
 *   "id": "village_night",
 *   "priority": 44,
 *   "conditions": ["village", "night"],
 *   "sound": "music.night_village"
 * }
 * </pre>
 *
 * ВАЖНО: пустой конструктор обязателен. Gson вызывает именно его, и только тогда
 * значения по умолчанию из полей ниже сохраняются для ключей, которых нет в JSON.
 */
public class MusicTrigger {

    /** Уникальное имя триггера. Используется как ключ "какой трек сейчас играет". */
    public String id = "";

    /** Чем больше число — тем раньше проверяется. Побеждает первый подошедший триггер. */
    public int priority = 0;

    /**
     * Одно условие: always, day, night, rain, thunder, village, raid, wither, underground,
     * underwater, biome, biome_tag, dimension, entity, block, structure, credits.
     */
    public String condition = "always";

    /**
     * Несколько условий сразу (все должны выполниться). Если задан — поле "condition"
     * игнорируется. Пример: ["biome_tag", "rain"] для "дождь над океаном".
     */
    public List<String> conditions = null;

    /** Ключ трека из ModSounds/sounds.json, например "music.village". */
    public String sound = "";

    /** Подпись для тоста "сейчас играет". Если пусто — покажется ключ трека. */
    public String title = "";

    /** true — трек зацикливается (тема местности), false — играет один раз. */
    public boolean looping = true;

    /** Сколько тиков нарастает. 0 — взять общее значение из блока "crossfade". */
    public int fadeInTicks = 0;

    /** Сколько тиков затихает. 0 — взять общее значение из блока "crossfade". */
    public int fadeOutTicks = 0;

    // ------------------------------------------------------------------
    //  Параметры отдельных условий (нужны не всем)
    // ------------------------------------------------------------------

    /** Для condition = "biome": id биома, например "minecraft:forest". */
    public String biome = null;

    /** Для condition = "biome_tag": тег биома, например "minecraft:is_ocean". */
    public String biomeTag = null;

    /** Для condition = "dimension": id измерения, например "minecraft:the_nether". */
    public String dimension = null;

    /**
     * Дополнительный фильтр для ЛЮБОГО условия: триггер сработает только в этом измерении.
     * Например "minecraft:overworld" — чтобы ночь/дождь/пещеры не лезли в Нижний мир и Энд.
     * null или пусто = без ограничения.
     */
    public String onlyInDimension = null;

    /** Для condition = "entity": список типов сущностей, например ["minecraft:warden"]. */
    public List<String> entities = null;

    /** Для condition = "block": список блоков, например ["minecraft:end_portal_frame"]. */
    public List<String> blocks = null;

    /**
     * Для condition = "structure": сколько блоков-маркеров из "blocks" должно встретиться
     * в кубе поиска, чтобы считать, что игрок внутри структуры. По умолчанию 24.
     */
    public int minCount = 24;

    /** Для condition = "structure": шаг перебора (1 — каждый блок, 2 — каждый второй). */
    public int step = 1;

    /** Радиус поиска в блоках: для village, entity, block. */
    public int radius = 32;

    /**
     * Для condition = "underground": на сколько блоков ниже ПОВЕРХНОСТИ в этой точке
     * игрок должен быть, чтобы считаться под землёй. По умолчанию 8.
     *
     * Именно от поверхности, а не от фиксированного Y: иначе пещера в горе на высоте 80
     * не считалась бы пещерой (а на равнине пещерой становился бы дом на склоне).
     */
    public int depth = 8;

    /** Для condition = "underground": абсолютный порог Y — ниже него всегда пещера. */
    public int y = 50;

    // ------------------------------------------------------------------
    //  Кэши разобранных значений (transient — Gson их не пишет)
    // ------------------------------------------------------------------

    /** Разобранные условия. null — значит в конфиге опечатка (триггер отбрасывается). */
    private transient List<TriggerCondition> resolvedConditions;
    private transient boolean conditionsResolved;

    private transient Set<ResourceLocation> resolvedEntities;
    private transient Set<Block> resolvedBlocks;
    private transient TagKey<Biome> resolvedBiomeTag;
    private transient boolean biomeTagResolved;

    /** Нужен Gson: без него не применятся значения по умолчанию из полей выше. */
    public MusicTrigger() {
    }

    /**
     * Разобранные условия триггера.
     *
     * @return список условий (все должны выполниться) или null, если хотя бы одно имя
     *         в конфиге написано с ошибкой — такой триггер реестр отбрасывает.
     */
    public List<TriggerCondition> conditionTypes() {
        if (!this.conditionsResolved) {
            this.conditionsResolved = true;

            List<String> names = (this.conditions != null && !this.conditions.isEmpty())
                    ? this.conditions
                    : Collections.singletonList(this.condition);

            List<TriggerCondition> parsed = new ArrayList<>(names.size());
            for (String name : names) {
                TriggerCondition type = TriggerCondition.byName(name);
                if (type == null) {
                    parsed = null;
                    break;
                }
                parsed.add(type);
            }
            this.resolvedConditions = (parsed == null || parsed.isEmpty()) ? null : parsed;
        }
        return this.resolvedConditions;
    }

    /** Подходит ли триггер прямо сейчас: все условия выполнены и измерение разрешено. */
    public boolean test(Minecraft minecraft) {
        List<TriggerCondition> types = conditionTypes();
        if (types == null) {
            return false;
        }
        if (!dimensionAllowed(minecraft)) {
            return false;
        }
        for (TriggerCondition type : types) {
            if (!type.test(minecraft, this)) {
                return false;
            }
        }
        return true;
    }

    /** Проверка необязательного фильтра onlyInDimension. */
    private boolean dimensionAllowed(Minecraft minecraft) {
        if (this.onlyInDimension == null || this.onlyInDimension.isBlank()) {
            return true;
        }
        if (minecraft.level == null) {
            return false;
        }
        return minecraft.level.dimension().location().toString().equals(this.onlyInDimension);
    }

    /** Разобранный список типов сущностей (пустой, если ничего не задано). */
    public Set<ResourceLocation> entityIds() {
        if (this.resolvedEntities == null) {
            this.resolvedEntities = parseIds(this.entities);
        }
        return this.resolvedEntities;
    }

    /** Разобранный список блоков (пустой, если ничего не задано или id не существует). */
    public Set<Block> blockSet() {
        if (this.resolvedBlocks == null) {
            Set<Block> set = new LinkedHashSet<>();
            if (this.blocks != null) {
                for (String raw : this.blocks) {
                    ResourceLocation id = ResourceLocation.tryParse(raw);
                    if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                        set.add(BuiltInRegistries.BLOCK.get(id));
                    }
                }
            }
            this.resolvedBlocks = set;
        }
        return this.resolvedBlocks;
    }

    /** Разобранный тег биома или null, если он не задан / написан с ошибкой. */
    public TagKey<Biome> biomeTagKey() {
        if (!this.biomeTagResolved) {
            this.biomeTagResolved = true;
            if (this.biomeTag != null && !this.biomeTag.isBlank()) {
                ResourceLocation id = ResourceLocation.tryParse(this.biomeTag);
                if (id != null) {
                    this.resolvedBiomeTag = TagKey.create(Registries.BIOME, id);
                }
            }
        }
        return this.resolvedBiomeTag;
    }

    private static Set<ResourceLocation> parseIds(List<String> raw) {
        Set<ResourceLocation> set = new LinkedHashSet<>();
        if (raw != null) {
            for (String value : raw) {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id != null) {
                    set.add(id);
                }
            }
        }
        return set;
    }

    /** Человекочитаемое описание — для логов и отладки. */
    public String describe() {
        List<TriggerCondition> types = conditionTypes();
        String cond = (types == null) ? "?" : types.stream().map(TriggerCondition::id).toList().toString();
        return "MusicTrigger{id=" + this.id
                + ", priority=" + this.priority
                + ", conditions=" + cond
                + ", sound=" + this.sound + "}";
    }
}
