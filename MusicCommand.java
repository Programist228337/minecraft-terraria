package com.minecraft_terraria.mod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.List;

/**
 * Клиентская команда для отладки музыки: /terrariamusic [now|list|reload]
 *
 * Нужна, чтобы не гадать "почему играет не то": команда показывает, какой триггер
 * победил прямо сейчас, с каким приоритетом и по каким условиям.
 *
 * Команда чисто клиентская (RegisterClientCommandsEvent), на сервере её нет.
 */
@EventBusSubscriber(modid = MinecraftTerrariamod.MODID, value = Dist.CLIENT)
public final class MusicCommand {

    private MusicCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("terrariamusic")
                // Без аргументов — то же, что "now"
                .executes(context -> showNow(context.getSource()))
                .then(Commands.literal("now")
                        .executes(context -> showNow(context.getSource())))
                .then(Commands.literal("list")
                        .executes(context -> showList(context.getSource())))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource()))));
    }

    /** Какой триггер выигрывает прямо сейчас и почему. */
    private static int showNow(CommandSourceStack source) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            source.sendSuccess(() -> Component.literal("§7Мира нет — играет музыка меню."), false);
            return 0;
        }

        MusicTrigger trigger = TriggerRegistry.resolve(minecraft);
        if (trigger == null) {
            source.sendSuccess(() -> Component.literal("§cНи один триггер не подошёл — музыки нет."), false);
            return 0;
        }

        String conditions = (trigger.conditionTypes() == null)
                ? "?"
                : trigger.conditionTypes().toString();

        source.sendSuccess(() -> Component.literal(
                "§6Сейчас: §f" + trigger.title
                        + " §8[" + trigger.id + ", priority " + trigger.priority + "]"), false);
        source.sendSuccess(() -> Component.literal(
                "§7условия: §f" + conditions
                        + "§7  трек: §f" + trigger.sound), false);

        double y = minecraft.player.getY();
        int surface = minecraft.level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                minecraft.player.blockPosition().getX(),
                minecraft.player.blockPosition().getZ());
        source.sendSuccess(() -> Component.literal(
                "§7Y=" + String.format("%.1f", y) + "  поверхность=" + surface
                        + "  глубина=" + String.format("%.1f", surface - y)
                        + "  дождь тут: " + (minecraft.level.isRainingAt(minecraft.player.blockPosition()) ? "да" : "нет")), false);

        return 1;
    }

    /** Список всех триггеров в порядке проверки. */
    private static int showList(CommandSourceStack source) {
        List<MusicTrigger> triggers = TriggerRegistry.triggers();
        source.sendSuccess(() -> Component.literal("§6Триггеров: §f" + triggers.size()
                + " §7(в порядке проверки)"), false);

        for (MusicTrigger trigger : triggers) {
            source.sendSuccess(() -> Component.literal(
                    "§8" + trigger.priority + " §f" + trigger.id + " §8-> §7" + trigger.sound), false);
        }
        return triggers.size();
    }

    /** Перечитать JSON с диска, не дожидаясь авто-перезагрузки. */
    private static int reload(CommandSourceStack source) {
        TriggerRegistry.forceReload();
        source.sendSuccess(() -> Component.literal("§aКонфиг триггеров перечитан."), false);
        return 1;
    }
}
