package ru.totemguard.protection;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import ru.kirikws.reputationfabric.api.ReputationAPIs;
import ru.totemguard.guest.ProtectionRules;
import ru.totemguard.storage.RegionState;
import ru.totemguard.storage.TrustedManager;

import java.util.UUID;

/**
 * Central protection handler for TotemGuard regions.
 * Registers all Fabric events for block, explosion, mob, and PvP protection.
 */
public class ProtectionHandler {
    // Server reference set via tick event
    private static MinecraftServer currentServer;

    public static void register() {
        // Capture server reference every tick
        ServerTickEvents.START_SERVER_TICK.register(server -> currentServer = server);

        registerBlockBreak();
        registerBlockUse();
        registerPvPRules();

        ru.totemguard.TotemGuardMod.LOGGER.info("Protection handlers registered.");
    }

    // ========================
    // 1. BLOCK BREAK
    // ========================

    private static void registerBlockBreak() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient || player.isCreative()) return true;
            if (currentServer == null) return true;

            RegionState regions = RegionState.getOrCreate(currentServer);
            UUID regionOwner = regions.getOwner(pos);
            if (regionOwner == null) return true; // No region here

            TrustedManager trust = TrustedManager.getOrCreate(currentServer);
            UUID playerId = player.getUuid();

            if (playerId.equals(regionOwner) || trust.isTrusted(regionOwner, playerId)) {
                return true; // Owner or trusted can break
            }

            player.sendMessage(Text.translatable("message.totemguard.not_owner"), true);
            return false;
        });
    }

    // ========================
    // 2. BLOCK USE
    // ========================

    private static void registerBlockUse() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (currentServer == null) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            RegionState regions = RegionState.getOrCreate(currentServer);
            UUID regionOwner = regions.getOwner(pos);
            if (regionOwner == null) return ActionResult.PASS; // No region

            TrustedManager trust = TrustedManager.getOrCreate(currentServer);
            UUID playerId = player.getUuid();

            // Owner or trusted: always allow
            if (playerId.equals(regionOwner) || trust.isTrusted(regionOwner, playerId)) {
                return ActionResult.PASS;
            }

            // Guests: always allow doors and plants
            if (isAlwaysAllowed(block)) {
                return ActionResult.PASS;
            }

            // Functional blocks: deny
            if (isFunctionalBlock(block)) {
                player.sendMessage(Text.translatable("message.totemguard.not_owner"), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    private static boolean isAlwaysAllowed(Block block) {
        return block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof PlantBlock
                || block instanceof FlowerBlock
                || block instanceof FernBlock;
    }

    private static boolean isFunctionalBlock(Block block) {
        return block instanceof ChestBlock
                || block instanceof BarrelBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof DropperBlock
                || block instanceof LeverBlock
                || block instanceof ButtonBlock
                || block instanceof PressurePlateBlock
                || block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof EnchantingTableBlock
                || block instanceof BeaconBlock;
    }

    // ========================
    // 3. PVP RULES
    // ========================

    private static void registerPvPRules() {
        AttackEntityCallback.EVENT.register((attacker, world, hand, victim, hitResult) -> {
            if (!(attacker instanceof ServerPlayerEntity attackerPlayer)) return ActionResult.PASS;
            if (!(victim instanceof ServerPlayerEntity victimPlayer)) return ActionResult.PASS;
            if (currentServer == null) return ActionResult.PASS;

            UUID attackerId = attackerPlayer.getUuid();
            UUID victimId = victimPlayer.getUuid();

            // Check if victim is intruder — owner can attack without penalty
            if (ProtectionRules.isIntruder(victimId)) {
                RegionState regions = RegionState.getOrCreate(currentServer);
                UUID regionOwner = regions.getOwner(attackerPlayer.getBlockPos());
                if (regionOwner != null && victimId.equals(regionOwner)) {
                    return ActionResult.PASS; // Allow attack on intruder
                }
                if (regions.getOwner(victimPlayer.getBlockPos()) != null) {
                    return ActionResult.PASS;
                }
            }

            // Check PASSIVE mode (reputation-fabric integration)
            if (ReputationAPIs.isPassive(attackerPlayer)) {
                attackerPlayer.sendMessage(Text.translatable("message.reputation-fabric.attack_blocked_passive"), true);
                return ActionResult.FAIL;
            }
            if (ReputationAPIs.isPassive(victimPlayer)) {
                attackerPlayer.sendMessage(Text.translatable("message.reputation-fabric.attack_blocked_target_passive"), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
}
