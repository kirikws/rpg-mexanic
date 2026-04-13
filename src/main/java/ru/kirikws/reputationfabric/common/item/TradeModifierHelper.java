package ru.kirikws.reputationfabric.common.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOffer;
import ru.kirikws.reputationfabric.api.ReputationAPIs;
import ru.kirikws.reputationfabric.config.ReputationConfig;

/**
 * Helper class for modifying trade prices based on player karma.
 * Provides static methods to calculate price multipliers.
 */
public class TradeModifierHelper {
    /**
     * Calculates the price multiplier for a trade based on player karma.
     * Returns a value greater than 1.0 for bad karma (penalty),
     * less than 1.0 for good karma (discount).
     *
     * @param player the player making the trade
     * @return the price multiplier
     */
    public static double getPriceMultiplier(PlayerEntity player) {
        int karma = ReputationAPIs.getKarma(player);
        ReputationConfig config = ReputationConfig.get();

        if (karma < config.trade_block_threshold) {
            // Trading blocked for very bad karma
            return -1.0;
        } else if (karma < 0) {
            // Bad karma - penalty
            double ratio = Math.abs(karma) / (double) Math.abs(config.trade_block_threshold);
            return 1.0 + (config.trade_penalty_multiplier - 1.0) * ratio;
        } else if (karma > 0) {
            // Good karma - bonus
            double ratio = karma / (double) config.threshold_good;
            return 1.0 - (1.0 - config.trade_bonus_multiplier) * ratio;
        }

        return 1.0; // Neutral karma - no change
    }

    /**
     * Checks if trading is blocked for the player.
     *
     * @param player the player to check
     * @return true if trading should be blocked
     */
    public static boolean isTradeBlocked(PlayerEntity player) {
        int karma = ReputationAPIs.getKarma(player);
        return karma < ReputationConfig.get().trade_block_threshold;
    }

    /**
     * Applies karma-based price modification to a trade offer.
     *
     * @param player the player making the trade
     * @param offer the original trade offer
     * @return the modified trade offer, or null if trade is blocked
     */
    public static TradeOffer applyTradeModifier(PlayerEntity player, TradeOffer offer) {
        if (isTradeBlocked(player)) {
            return null;
        }

        double multiplier = getPriceMultiplier(player);
        if (multiplier == 1.0) {
            return offer;
        }

        net.minecraft.item.ItemStack firstBuyItem = offer.getOriginalFirstBuyItem().copy();
        int originalCount = firstBuyItem.getCount();
        int modifiedCount = (int) Math.ceil(originalCount * multiplier);
        firstBuyItem.setCount(Math.max(1, modifiedCount));

        // Create new trade offer with modified price
        return new TradeOffer(
                firstBuyItem,
                offer.getSellItem().copy(),
                offer.getMaxUses(),
                offer.getDemandBonus(),
                0.05f
        );
    }
}
