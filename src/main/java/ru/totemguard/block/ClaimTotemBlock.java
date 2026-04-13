package ru.totemguard.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import ru.totemguard.config.TotemConfig;
import ru.totemguard.storage.RegionState;

/**
 * The Claim Totem block.
 * Creates and maintains a protected region around itself.
 * Cannot be broken by non-owners while active.
 */
public class ClaimTotemBlock extends BlockWithEntity {
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    public ClaimTotemBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(ACTIVE, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TotemBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, TotemBlocks.BLOCK_ENTITY_TYPE, TotemBlockEntity::serverTick);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.isClient) return;

        if (placer instanceof ServerPlayerEntity player) {
            if (world.getBlockEntity(pos) instanceof TotemBlockEntity be) {
                be.setOwner(player.getUuid());

                // Регистрируем тотем в системе регионов
                RegionState regions = RegionState.getOrCreate(player.getServer());
                regions.addTotem(pos, player.getUuid(), TotemConfig.get().base_radius);
                regions.save();

                player.sendMessage(
                        Text.literal("⚡ Тотем установлен!").formatted(net.minecraft.util.Formatting.GREEN),
                        false
                );
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (world.getBlockEntity(pos) instanceof TotemBlockEntity be) {
            player.openHandledScreen(be);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof TotemBlockEntity be) {
                for (int i = 0; i < be.size(); i++) {
                    ItemStack stack = be.getStack(i);
                    if (!stack.isEmpty()) {
                        net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
                                world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                        itemEntity.setToDefaultPickupDelay();
                        world.spawnEntity(itemEntity);
                    }
                }
                be.clear();
            }
            if (world.getServer() != null) {
                ru.totemguard.storage.RegionState regionState = ru.totemguard.storage.RegionState.getOrCreate(
                        world.getServer());
                regionState.removeTotem(pos);
                regionState.save();
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(ACTIVE, true);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
