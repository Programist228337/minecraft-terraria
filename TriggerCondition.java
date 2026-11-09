package com.minecraft_terraria.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Условие, при котором должен играть трек.
 *
 * Каждое условие — это просто проверка "да/нет" по текущему состоянию клиента.
 * Триггер (см. {@link MusicTrigger}) ссылается на условия по именам из JSON-конфига,
 * поэтому набор условий фиксирован кодом, а вот сколько триггеров на них навесить —
 * решает пользователь в config/minecraftterrariamod-music.json.
 *
 * Условия можно комбинировать: если у триггера задан список "conditions",
 * все они должны выполниться одновременно (логическое И).
 *
 * Чтобы добавить СВОЁ условие — допишите константу в этот enum и реализуйте лямбду.
 * Имя для JSON — первый аргумент (он же используется в byName()).
 */
public enum TriggerCondition {

    /** Всегда подходит. Полезно как "фон" с самым низким приоритетом. */
    ALWAYS("always", (mc, t) -> true),

    /** День в мире игрока. */
    DAY("day", (mc, t) -> inWorld(mc) && isDay(mc)),

    /**
     * Ночь в мире игрока (в Нижнем мире/Энде, где времени нет, не сработает).
     *
     * Считаем по getDayTime(), а НЕ через level.isNight() — см. {@link #isDay(Minecraft)}.
     */
    NIGHT("night", (mc, t) -> inWorld(mc) && isNight(mc)),

    /**
     * Дождь, но НЕ гроза.
     *
     * ВАЖНО: проверяем не глобальное isRaining() (оно true на весь Верхний мир, включая
     * пещеры и пустыни), а isRainingAt() — он дополнительно требует, чтобы над игроком
     * было открытое небо и в биоме вообще шли осадки. Поэтому под землёй и в доме
     * дождевая музыка больше не включается.
     */
    RAIN("rain", (mc, t) -> inWorld(mc) && mc.level.isRaining()
            && !mc.level.isThundering() && isExposedToWeather(mc)),

    /** Гроза. Как и дождь — только если игрок реально под открытым небом. */
    THUNDER("thunder", (mc, t) -> inWorld(mc) && mc.level.isThundering()
            && isExposedToWeather(mc)),

    /** Голова игрока под водой. */
    UNDERWATER("underwater", (mc, t) -> inWorld(mc) && mc.player.isUnderWater()),

    /**
     * Игрок под землёй: либо ниже поверхности в своей точке на {@code depth} блоков,
     * либо ниже абсолютного порога {@code y}.
     *
     * Первое условие нужно для пещер в горах: раньше проверялось только "y < 50",
     * и пещера на высоте 80 вообще никогда не считалась пещерой.
     */
    UNDERGROUND("underground", (mc, t) -> inWorld(mc) && isUnderground(mc, t)),

    /** Конкретный биом, например "minecraft:forest". */
    BIOME("biome", (mc, t) -> inWorld(mc) && t.biome != null && currentBiomeId(mc).equals(t.biome)),

    /**
     * Биом по тегу, например "minecraft:is_ocean" — покрывает сразу все океаны.
     * Полезно, когда не хочется писать 10 отдельных триггеров.
     */
    BIOME_TAG("biome_tag", (mc, t) -> inWorld(mc) && hasBiomeTag(mc, t.biomeTagKey())),

    /** Конкретное измерение, например "minecraft:the_nether". */
    DIMENSION("dimension", (mc, t) -> inWorld(mc) && t.dimension != null && currentDimensionId(mc).equals(t.dimension)),

    /**
     * Рядом есть житель — считаем это деревней.
     *
     * Почему через сущности, а не через StructureManager: мод клиентский, а ClientLevel
     * не предоставляет доступ к StructureManager (проверено по исходникам 1.21.1).
     * Зато жители приходят в клиент обычным трекингом сущностей, и этого достаточно.
     * Радиус поиска задаётся полем "radius" у триггера (по умолчанию 32 блока).
     */
    VILLAGE("village", (mc, t) -> inWorld(mc) && hasNearbyVillager(mc, t.radius)),

    /**
     * Идёт рейд. Определяется по босс-бару рейда — см. {@link BossWatcher}.
     * Важно: пока HUD скрыт (F1), босс-бар не рисуется и условие не сработает.
     */
    RAID("raid", (mc, t) -> inWorld(mc) && BossWatcher.isActive(mc, BossWatcher.Kind.RAID)),

    /**
     * Активен НАСТОЯЩИЙ босс-Визер (тот, у которого полоска здоровья и с которого падает
     * звезда Края), а не визер-скелет.
     *
     * Основной признак — его босс-бар (имя "entity.minecraft.wither"); дополнительно
     * проверяем сущность minecraft:wither рядом, чтобы музыка не пропадала при скрытом HUD.
     * Визер-скелет имеет id minecraft:wither_skeleton и под это условие не попадает.
     */
    WITHER("wither", (mc, t) -> inWorld(mc) && isWitherBoss(mc, t)),

    /**
     * Рядом сущность одного из перечисленных типов.
     * Список задаётся полем "entities": ["minecraft:warden", ...], радиус — "radius".
     */
    ENTITY("entity", (mc, t) -> inWorld(mc) && hasNearbyEntityType(mc, t.entityIds(), t.radius)),

    /**
     * Рядом есть блок одного из перечисленных типов.
     * Список задаётся полем "blocks": ["minecraft:end_portal_frame", ...], радиус — "radius".
     *
     * ВНИМАНИЕ: это перебор блоков в кубе, поэтому радиус жёстко ограничен 24 блоками
     * (24 -> ~118 тысяч проверок). Держите радиус разумным, иначе будут просадки FPS.
     */
    BLOCK("block", (mc, t) -> inWorld(mc) && hasNearbyBlock(mc, t.blockSet(), t.radius)),

    /**
     * Игрок ВНУТРИ постройки, опознанной по блокам-маркерам.
     *
     * Отличие от "block": тот срабатывает от одного блока в радиусе (годится для портала),
     * а этот считает, сколько блоков-маркеров встретилось в кубе вокруг игрока, и требует
     * минимум "minCount". Так ловится структура целиком — например, вся крепость
     * (она почти целиком из каменного кирпича), а не только зал с порталом.
     *
     * Перебор идёт с ранним выходом: как только набрали minCount — сразу true,
     * поэтому внутри структуры проверка почти ничего не стоит. Полный перебор
     * (куб radius*2+1) выполняется только когда игрок НЕ в структуре.
     */
    STRUCTURE("structure", (mc, t) -> inWorld(mc) && isInsideStructure(mc, t)),

    /** Открыт экран финальных титров (после дракона или из меню "Credits"). */
    CREDITS("credits", (mc, t) -> mc.screen instanceof WinScreen);

    private final String id;
    private final BiPredicate<Minecraft, MusicTrigger> predicate;

    TriggerCondition(String id, BiPredicate<Minecraft, MusicTrigger> predicate) {
        this.id = id;
        this.predicate = predicate;
    }

    public String id() {
        return this.id;
    }

    public boolean test(Minecraft minecraft, MusicTrigger trigger) {
        return this.predicate.test(minecraft, trigger);
    }

    /** Найти условие по имени из JSON. Регистр не важен. Возвращает null, если такого нет. */
    public static TriggerCondition byName(String name) {
        if (name == null) {
            return null;
        }
        String needle = name.trim().toLowerCase(Locale.ROOT);
        for (TriggerCondition condition : values()) {
            if (condition.id.equals(needle)) {
                return condition;
            }
        }
        return null;
    }

    /** Список всех доступных имён — для понятных сообщений об ошибке в логе. */
    public static List<String> names() {
        return Arrays.stream(values()).map(TriggerCondition::id).toList();
    }

    // ------------------------------------------------------------------
    //  Вспомогательные проверки
    // ------------------------------------------------------------------

    private static boolean inWorld(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null;
    }

    /** Граница "день/ночь" в тиках внутри суток: 0-13000 день и закат, 13000-23000 ночь. */
    private static final long NIGHT_START = 13000L;
    private static final long DAY_LENGTH = 24000L;

    /**
     * Сейчас день?
     *
     * ВАЖНО: специально НЕ используем {@code level.isDay()} / {@code level.isNight()}.
     * Они опираются на {@code skyDarken}, а на клиенте {@code updateSkyBrightness()}
     * вызывается РОВНО ОДИН РАЗ — в конструкторе ClientLevel (проверено по исходникам
     * 1.21.1: других вызовов на клиенте нет, в отличие от ServerLevel).
     * То есть значение замирает на моменте входа в мир: зашли днём — isNight() уже
     * никогда не станет true, и ночная музыка не включится. Это и был баг "ночь не работает".
     *
     * getDayTime() на клиенте тикает нормально (ClientLevel.tickTime) и приходит с сервера,
     * поэтому считаем по нему.
     */
    private static boolean isDay(Minecraft minecraft) {
        if (minecraft.level.dimensionType().hasFixedTime()) {
            // Нижний мир и Энд: цикла суток нет, ночи как таковой не бывает.
            return false;
        }
        long time = minecraft.level.getDayTime() % DAY_LENGTH;
        if (time < 0L) {
            time += DAY_LENGTH;
        }
        return time < NIGHT_START;
    }

    private static boolean isNight(Minecraft minecraft) {
        return !minecraft.level.dimensionType().hasFixedTime() && !isDay(minecraft);
    }

    /**
     * Игрок реально находится под дождём: над ним открытое небо, в биоме идут осадки
     * и над головой нет крыши/свода пещеры.
     *
     * isRainingAt() в 1.21.1 проверяет ровно это (isRaining + canSeeSky + хайтмапа
     * MOTION_BLOCKING + precipitation биома), поэтому берём его, а не canSeeSky вручную.
     */
    private static boolean isExposedToWeather(Minecraft minecraft) {
        return minecraft.level.isRainingAt(minecraft.player.blockPosition());
    }

    /** Игрок под землёй: глубоко относительно поверхности своей колонки или ниже порога y. */
    private static boolean isUnderground(Minecraft minecraft, MusicTrigger trigger) {
        double playerY = minecraft.player.getY();

        // Абсолютный порог: ниже него пещера в любом случае.
        if (playerY < trigger.y) {
            return true;
        }

        BlockPos pos = minecraft.player.blockPosition();
        int surfaceY = minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        int depth = Math.max(1, trigger.depth);

        // Пещера в горе: поверхность высоко, а игрок сидит глубоко под ней.
        return playerY < surfaceY - depth;
    }

    private static String currentBiomeId(Minecraft minecraft) {
        return minecraft.level.getBiome(minecraft.player.blockPosition())
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");
    }

    private static String currentDimensionId(Minecraft minecraft) {
        return minecraft.level.dimension().location().toString();
    }

    private static boolean hasBiomeTag(Minecraft minecraft, TagKey<Biome> tag) {
        if (tag == null) {
            return false;
        }
        return minecraft.level.getBiome(minecraft.player.blockPosition()).is(tag);
    }

    private static boolean hasNearbyVillager(Minecraft minecraft, int radius) {
        return !minecraft.level
                .getEntitiesOfClass(Villager.class, minecraft.player.getBoundingBox().inflate(radius))
                .isEmpty();
    }

    /**
     * Настоящий босс-Визер. Визер-скелет — это отдельная сущность
     * (minecraft:wither_skeleton), под это условие он не попадает.
     *
     * Id лежит в отдельном holder-классе, чтобы ResourceLocation инициализировался
     * только при первом обращении, а не при загрузке самого enum условий.
     */
    private static final class WitherBossId {
        static final Set<ResourceLocation> IDS = Set.of(ResourceLocation.tryParse("minecraft:wither"));

        private WitherBossId() {
        }
    }

    private static boolean isWitherBoss(Minecraft minecraft, MusicTrigger trigger) {
        // Полоска HP — самый точный признак: она есть только у настоящего босса.
        if (BossWatcher.isActive(minecraft, BossWatcher.Kind.WITHER)) {
            return true;
        }
        // Запасной путь на случай скрытого HUD (F1): бары не рисуются, смотрим сущность.
        return hasNearbyEntityType(minecraft, WitherBossId.IDS, trigger.radius);
    }

    private static boolean hasNearbyEntityType(Minecraft minecraft, Set<ResourceLocation> ids, int radius) {
        if (ids.isEmpty()) {
            return false;
        }
        double centerX = minecraft.player.getX();
        double centerY = minecraft.player.getY();
        double centerZ = minecraft.player.getZ();
        double radiusSq = (double) radius * (double) radius;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity == minecraft.player) {
                continue;
            }
            if (entity.distanceToSqr(centerX, centerY, centerZ) > radiusSq) {
                continue;
            }
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (entityId != null && ids.contains(entityId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbyBlock(Minecraft minecraft, Set<Block> blocks, int radius) {
        if (blocks.isEmpty()) {
            return false;
        }
        int r = Mth.clamp(radius, 1, 24);
        BlockPos center = minecraft.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r),
                center.offset(r, r, r))) {
            if (blocks.contains(minecraft.level.getBlockState(pos).getBlock())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Внутри ли игрок постройки из блоков-маркеров.
     *
     * Считаем не "есть ли блок рядом", а плотность: структура окружена своими блоками
     * со всех сторон, поэтому в кубе их много. Одного блока мало — он может быть
     * случайным (или поставленным вручную).
     */
    private static boolean isInsideStructure(Minecraft minecraft, MusicTrigger trigger) {
        Set<Block> signature = trigger.blockSet();
        if (signature.isEmpty()) {
            return false;
        }

        int radius = Mth.clamp(trigger.radius, 4, 32);
        int step = Mth.clamp(trigger.step, 1, 4);
        int needed = Math.max(1, trigger.minCount);

        BlockPos center = minecraft.player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int found = 0;

        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dy = -radius; dy <= radius; dy += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (signature.contains(minecraft.level.getBlockState(cursor).getBlock())) {
                        found++;
                        // Набрали достаточно — дальше искать незачем.
                        if (found >= needed) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
