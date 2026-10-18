package dev.meiny.dan.advancement;

import dev.meiny.dan.Dan;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

public final class DanAdvancementProgress {
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static final String TREE_PREFIX = "dan/";
    private static final Identifier ROOT = Dan.id("dan/root");
    private static final Identifier COMPLETIONIST = Dan.id("dan/all_dan_advancements");

    private DanAdvancementProgress() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % CHECK_INTERVAL_TICKS == 0) {
                checkAll(server);
            }
        });
    }

    private static void checkAll(MinecraftServer server) {
        ServerAdvancementManager manager = server.getAdvancements();
        AdvancementHolder completionist = manager.get(COMPLETIONIST);
        if (completionist == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerAdvancements advancements = player.getAdvancements();
            if (advancements.getOrStartProgress(completionist).isDone()) {
                continue;
            }
            for (AdvancementHolder holder : manager.getAllAdvancements()) {
                if (isCounted(holder) && advancements.getOrStartProgress(holder).isDone()) {
                    ModCriteria.COMPLETED_DAN_ADVANCEMENT.trigger(player, holder.id());
                }
            }
        }
    }

    private static boolean isCounted(AdvancementHolder holder) {
        Identifier id = holder.id();
        return id.getNamespace().equals(Dan.MOD_ID)
                && id.getPath().startsWith(TREE_PREFIX)
                && !id.equals(ROOT)
                && !id.equals(COMPLETIONIST)
                && holder.value().display().isPresent();
    }
}
