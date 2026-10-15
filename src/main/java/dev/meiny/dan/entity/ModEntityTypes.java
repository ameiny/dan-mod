package dev.meiny.dan.entity;

import dev.meiny.dan.Dan;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class ModEntityTypes {
    private static final float CHERRY_CHANCE = 0.6f;
    private static final float MEADOW_CHANCE = 0.2f;
    private static final float FLOWER_FOREST_CHANCE = 0.6f;
    private static final float FOREST_CHANCE = 0.3f;
    private static final float JUNGLE_CHANCE = 0.25f;
    private static final float MANGROVE_CHANCE = 0.1f;

    public static final ResourceKey<EntityType<?>> DAN_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Dan.MOD_ID, "dan"));

    public static final EntityType<DanEntity> DAN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, DAN_KEY,
            FabricEntityType.Builder.<DanEntity>createMob(DanEntity::new, MobCategory.CREATURE, mob -> mob
                            .spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    (type, level, reason, pos, random) ->
                                            isGoodSpawnBlock(level.getBlockState(pos.below()))
                                                    && level.getRawBrightness(pos, 0) > 8
                                                    && random.nextFloat() < spawnChance(level.getBiome(pos)))
                            .defaultAttributes(DanEntity::createAttributes))
                    .sized(0.6f, 1.8f)
                    .build(DAN_KEY));

    public static void init() {
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(Biomes.CHERRY_GROVE),
                MobCategory.CREATURE, DAN, 2, 1, 2);

        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(Biomes.FLOWER_FOREST, Biomes.MEADOW),
                MobCategory.CREATURE, DAN, 1, 1, 2);

        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        Biomes.JUNGLE, Biomes.MANGROVE_SWAMP, Biomes.BAMBOO_JUNGLE,
                        Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST),
                MobCategory.CREATURE, DAN, 1, 1, 1);
    }

    private static boolean isGoodSpawnBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MANGROVE_ROOTS)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    private static float spawnChance(Holder<Biome> biome) {
        if (biome.is(Biomes.CHERRY_GROVE)) {
            return CHERRY_CHANCE;
        }
        if (biome.is(Biomes.MEADOW)) {
            return MEADOW_CHANCE;
        }
        if (biome.is(Biomes.FLOWER_FOREST)) {
            return FLOWER_FOREST_CHANCE;
        }
        if (biome.is(Biomes.FOREST) || biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return FOREST_CHANCE;
        }
        if (biome.is(Biomes.MANGROVE_SWAMP)) {
            return MANGROVE_CHANCE;
        }
        return JUNGLE_CHANCE;
    }
}