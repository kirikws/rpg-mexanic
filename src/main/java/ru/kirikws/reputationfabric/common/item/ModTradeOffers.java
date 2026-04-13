package ru.kirikws.reputationfabric.common.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import ru.kirikws.reputationfabric.common.registry.ModItems;

/**
 * Helper class for registering villager trades related to the mod.
 * Karma compass is sold by Cartographer villagers at level 3.
 */
public class ModTradeOffers {
    /**
     * Registers all custom villager trades.
     */
    public static void registerTrades() {
        // Register karma compass trade with Cartographer at level 3
        TradeOfferHelper.registerVillagerOffers(
                net.minecraft.village.VillagerProfession.CARTOGRAPHER,
                3,
                factories -> factories.add((entity, random) -> new TradeOffer(
                        new ItemStack(Items.EMERALD, 15),
                        new ItemStack(ModItems.KARMA_COMPASS),
                        5,
                        10,
                        0.05f
                ))
        );
    }
}
