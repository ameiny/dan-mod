package dev.meiny.dan.entity;

import dev.meiny.dan.block.ModBlocks;
import dev.meiny.dan.item.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

public final class DanTrades {
    private static final int OFFER_COUNT = 4;
    private static final float DIAMOND_CHANCE = 0.2f;
    private static final int DIAMOND_INDEX = 7;
    private static final List<Integer> COMMON_INDICES = List.of(0, 1, 2, 3, 4, 5, 6);

    private DanTrades() {
    }

    public static MerchantOffers create(UUID danId) {
        Random random = new Random(danId.getMostSignificantBits() ^ danId.getLeastSignificantBits());

        List<Integer> chosen = new ArrayList<>();
        if (random.nextFloat() < DIAMOND_CHANCE) {
            chosen.add(DIAMOND_INDEX);
        }

        List<Integer> common = new ArrayList<>(COMMON_INDICES);
        Collections.shuffle(common, random);
        for (int index : common) {
            if (chosen.size() >= OFFER_COUNT) {
                break;
            }
            chosen.add(index);
        }
        chosen.sort(Comparator.naturalOrder());

        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(Items.PINK_PETALS, 4, Items.AMETHYST_SHARD, 1, 12));

        for (int index : chosen) {
            offers.add(createOffer(index));
        }

        return offers;
    }

    public static MerchantOffers createAldi() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(Items.AMETHYST_SHARD, 2, ModItems.MILK_JUMBO, 3, 12));
        offers.add(offer(Items.AMETHYST_SHARD, 1, ModItems.VEGAN_DOENER, 1, 8));
        return offers;
    }

    private static MerchantOffer createOffer(int index) {
        return switch (index) {
            case 0 -> offer(Items.POPPY, 1, Items.COPPER_INGOT, 1, 12);
            case 1 -> offer(Items.DANDELION, 2, Items.IRON_INGOT, 1, 8);
            case 2 -> offer(Items.CORNFLOWER, 8, Items.LAPIS_LAZULI, 4, 12);
            case 3 -> offer(Items.OXEYE_DAISY, 8, Items.GOLD_INGOT, 1, 8);
            case 4 -> offer(Items.RED_TULIP, 4, Items.EMERALD, 1, 6);
            case 5 -> offer(Items.AMETHYST_SHARD, 1, ModItems.VEGAN_DOENER, 1, 8);
            case 6 -> offer(Items.AMETHYST_SHARD, 1, ModBlocks.DAN_BLOCK, 4, 12);
            default -> offer(Items.TORCHFLOWER, 3, Items.DIAMOND, 1, 3);
        };
    }

    private static MerchantOffer offer(ItemLike cost, int costCount, ItemLike result, int resultCount, int maxUses) {
        return new MerchantOffer(new ItemCost(cost, costCount), new ItemStack(result, resultCount), maxUses, 0, 0.0f);
    }
}