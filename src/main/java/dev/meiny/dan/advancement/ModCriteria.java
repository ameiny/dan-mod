package dev.meiny.dan.advancement;

import dev.meiny.dan.Dan;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModCriteria {
    public static final DanTradeTrigger TRADED_WITH_DAN = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES, Dan.id("traded_with_dan"), new DanTradeTrigger());

    public static final PlayerTrigger BOUGHT_OUT_DAN = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES, Dan.id("bought_out_dan"), new PlayerTrigger());

    public static final PlayerTrigger SAW_DANS_MEET = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES, Dan.id("saw_dans_meet"), new PlayerTrigger());

    public static final PlayerTrigger LURED_DAN_HOME = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES, Dan.id("lured_dan_home"), new PlayerTrigger());

    public static final PlayerTrigger BEFRIENDED_DAN = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES, Dan.id("befriended_dan"), new PlayerTrigger());

    public static final CompletedAdvancementTrigger COMPLETED_DAN_ADVANCEMENT = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES, Dan.id("completed_dan_advancement"), new CompletedAdvancementTrigger());

    public static void init() {
        DanAdvancementProgress.init();
    }
}
