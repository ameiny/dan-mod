package dev.meiny.dan.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public final class DanTrades {
    private static final int OFFER_COUNT = 4;
    private static final float DIAMOND_CHANCE = 0.2f;

    private DanTrades() {
    }

    public static MerchantOffers create(UUID danId) {
        Random random = new Random(danId.getMostSignificantBits() ^ danId.getLeastSignificantBits());

        List<Integer> chosen = new ArrayList<>();
        if (random.nextFloat() < DIAMOND_CHANCE) {
            chosen.add(6);
        }

        List<Integer> common = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        Collections.shuffle(common, random);
        for (int index : common) {
            if (chosen.size() >= OFFER_COUNT) {
                break;
            }
            chosen.add(index);
        }
        chosen.sort(Comparator.naturalOrder());

        MerchantOffers offers = new MerchantOffers();
        for (int index : chosen) {
            offers.add(createOffer(index));
        }
        return offers;
    }

    private static MerchantOffer createOffer(int index) {
        return switch (index) {
            case 0 -> offer(Items.POPPY, 1, Items.COPPER_INGOT, 1, 12);
            case 1 -> offer(Items.DANDELION, 2, Items.IRON_INGOT, 1, 8);
            case 2 -> offer(Items.CORNFLOWER, 8, Items.LAPIS_LAZULI, 4, 12);
            case 3 -> offer(Items.PINK_PETALS, 6, Items.AMETHYST_SHARD, 2, 12);
            case 4 -> offer(Items.OXEYE_DAISY, 8, Items.GOLD_INGOT, 1, 8);
            case 5 -> offer(Items.RED_TULIP, 4, Items.EMERALD, 1, 6);
            default -> offer(Items.TORCHFLOWER, 3, Items.DIAMOND, 1, 3);
        };
    }

    private static MerchantOffer offer(Item cost, int costCount, Item result, int resultCount, int maxUses) {
        return new MerchantOffer(new ItemCost(cost, costCount), new ItemStack(result, resultCount), maxUses, 0, 0.0f);
    }
}
