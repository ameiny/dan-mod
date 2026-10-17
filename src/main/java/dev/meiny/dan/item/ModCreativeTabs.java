package dev.meiny.dan.item;

import dev.meiny.dan.Dan;
import dev.meiny.dan.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> DAN_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(Dan.MOD_ID, "dan_tab"));

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DAN_TAB_KEY, FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.dan.dan_tab"))
                .icon(() -> new ItemStack(ModItems.DAN_SPAWN_EGG))
                .displayItems((context, output) -> {
                    output.accept(ModItems.DAN_SPAWN_EGG);
                    output.accept(ModItems.VEGAN_DOENER);
                    output.accept(ModItems.MILK_JUMBO);
                    output.accept(ModBlocks.DAN_BLOCK);
                    output.accept(ModBlocks.DAN_STAIRS);
                    output.accept(ModBlocks.DAN_SLAB);
                    output.accept(ModBlocks.POLISHED_DAN_BLOCK);
                    output.accept(ModBlocks.POLISHED_DAN_STAIRS);
                    output.accept(ModBlocks.POLISHED_DAN_SLAB);
                })
                .build());
    }
}