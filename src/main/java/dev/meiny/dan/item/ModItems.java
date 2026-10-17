package dev.meiny.dan.item;

import dev.meiny.dan.Dan;
import dev.meiny.dan.entity.ModEntityTypes;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

public class ModItems {
    private static final int VEGAN_DOENER_EFFECT_TICKS = 200;
    private static final int MILK_JUMBO_EFFECT_TICKS = 160;

    public static final FoodProperties VEGAN_DOENER_FOOD = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(0.8f)
            .build();

    public static final Consumable VEGAN_DOENER_CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.SPEED, VEGAN_DOENER_EFFECT_TICKS, 0),
                    new MobEffectInstance(MobEffects.REGENERATION, VEGAN_DOENER_EFFECT_TICKS, 0)
            )))
            .build();

    public static final FoodProperties MILK_JUMBO_FOOD = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.3f)
            .alwaysEdible()
            .build();

    public static final Consumable MILK_JUMBO_CONSUMABLE = Consumables.defaultFood()
            .consumeSeconds(0.8f)
            .onConsume(new ApplyStatusEffectsConsumeEffect(
                    new MobEffectInstance(MobEffects.SPEED, MILK_JUMBO_EFFECT_TICKS, 1)))
            .build();

    public static final Item DAN_SPAWN_EGG = register(
            "dan_spawn_egg",
            SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntityTypes.DAN)
    );

    public static final Item VEGAN_DOENER = register(
            "vegan_doener",
            Item::new,
            new Item.Properties().food(VEGAN_DOENER_FOOD, VEGAN_DOENER_CONSUMABLE)
    );

    public static final Item MILK_JUMBO = register(
            "milk_jumbo",
            Item::new,
            new Item.Properties().food(MILK_JUMBO_FOOD, MILK_JUMBO_CONSUMABLE)
    );

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Dan.MOD_ID, name));
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
            output.accept(DAN_SPAWN_EGG);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> {
            output.accept(VEGAN_DOENER);
            output.accept(MILK_JUMBO);
        });
    }
}