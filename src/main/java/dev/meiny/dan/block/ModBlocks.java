package dev.meiny.dan.block;

import dev.meiny.dan.Dan;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
    public static final Block DAN_BLOCK = register(
            "dan_block",
            Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)
    );

    public static final Block DAN_STAIRS = register(
            "dan_stairs",
            properties -> new StairBlock(DAN_BLOCK.defaultBlockState(), properties),
            BlockBehaviour.Properties.ofFullCopy(DAN_BLOCK)
    );

    public static final Block DAN_SLAB = register(
            "dan_slab",
            SlabBlock::new,
            BlockBehaviour.Properties.ofFullCopy(DAN_BLOCK)
    );

    public static final Block POLISHED_DAN_BLOCK = register(
            "polished_dan_block",
            Block::new,
            BlockBehaviour.Properties.ofFullCopy(DAN_BLOCK)
    );

    public static final Block POLISHED_DAN_STAIRS = register(
            "polished_dan_stairs",
            properties -> new StairBlock(POLISHED_DAN_BLOCK.defaultBlockState(), properties),
            BlockBehaviour.Properties.ofFullCopy(POLISHED_DAN_BLOCK)
    );

    public static final Block POLISHED_DAN_SLAB = register(
            "polished_dan_slab",
            SlabBlock::new,
            BlockBehaviour.Properties.ofFullCopy(POLISHED_DAN_BLOCK)
    );

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Dan.id(name));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Dan.id(name));

        Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
        return block;
    }

    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
            output.accept(DAN_BLOCK);
            output.accept(DAN_STAIRS);
            output.accept(DAN_SLAB);
            output.accept(POLISHED_DAN_BLOCK);
            output.accept(POLISHED_DAN_STAIRS);
            output.accept(POLISHED_DAN_SLAB);
        });
    }
}