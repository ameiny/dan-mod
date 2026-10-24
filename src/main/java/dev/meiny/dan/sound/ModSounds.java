package dev.meiny.dan.sound;

import dev.meiny.dan.Dan;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent DAN_GREET1 = register("entity.dan.greet");
    public static final SoundEvent DAN_HURT = register("entity.dan.hurt");
    public static final SoundEvent DAN_TRADE = register("entity.dan.trade");
    public static final SoundEvent DAN_DEATH = register("entity.dan.death");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Dan.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
    }
}