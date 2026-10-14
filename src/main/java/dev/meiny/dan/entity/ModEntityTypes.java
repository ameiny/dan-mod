package dev.meiny.dan.entity;

import dev.meiny.dan.Dan;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {
    public static final ResourceKey<EntityType<?>> DAN_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Dan.MOD_ID, "dan"));

    public static final EntityType<DanEntity> DAN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, DAN_KEY,
            EntityType.Builder.<DanEntity>of(DanEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .build(DAN_KEY));

    public static void init() {
        FabricDefaultAttributeRegistry.register(DAN, DanEntity.createAttributes());
    }
}