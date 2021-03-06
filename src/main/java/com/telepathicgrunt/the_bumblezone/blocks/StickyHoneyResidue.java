package com.telepathicgrunt.the_bumblezone.blocks;

import com.telepathicgrunt.the_bumblezone.items.BzItems;
import com.telepathicgrunt.the_bumblezone.mixin.VineBlockAccessor;
import net.minecraft.block.*;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Map;

public class StickyHoneyResidue extends VineBlock {
    public static final BooleanProperty DOWN = SixWayBlock.DOWN;
    protected static final VoxelShape DOWN_AABB = Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 0.8D, 16.0D);
    public static final Map<Direction, BooleanProperty> FACING_TO_PROPERTY_MAP =
            SixWayBlock.FACING_TO_PROPERTY_MAP.entrySet().stream().collect(Util.toMapCollector());

    public StickyHoneyResidue() {
        super(Block.Properties.create(BzBlocks.RESIDUE, MaterialColor.ADOBE).doesNotBlockMovement().hardnessAndResistance(6.0f, 0.0f).nonOpaque());
        this.setDefaultState(this.stateContainer.getBaseState()
                .with(UP, false)
                .with(NORTH, false)
                .with(EAST, false)
                .with(SOUTH, false)
                .with(WEST, false)
                .with(DOWN, false));
    }

    /**
     * Set up properties.
     */
    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH, EAST, SOUTH, WEST, DOWN);
    }

    /**
     * Returns the shape based on the state of the block.
     */
    @Override
    public VoxelShape getShape(BlockState blockstate, IBlockReader world, BlockPos pos, ISelectionContext context) {
        VoxelShape voxelshape = VoxelShapes.empty();
        if (blockstate.get(UP)) {
            voxelshape = VoxelShapes.or(voxelshape, VineBlockAccessor.getUP_SHAPE());
        }

        if (blockstate.get(SOUTH)) {
            voxelshape = VoxelShapes.or(voxelshape, VineBlockAccessor.getNORTH_SHAPE());
        }

        if (blockstate.get(WEST)) {
            voxelshape = VoxelShapes.or(voxelshape, VineBlockAccessor.getEAST_SHAPE());
        }

        if (blockstate.get(NORTH)) {
            voxelshape = VoxelShapes.or(voxelshape, VineBlockAccessor.getSOUTH_SHAPE());
        }

        if (blockstate.get(EAST)) {
            voxelshape = VoxelShapes.or(voxelshape, VineBlockAccessor.getWEST_SHAPE());
        }

        if (blockstate.get(DOWN)) {
            voxelshape = VoxelShapes.or(voxelshape, DOWN_AABB);
        }

        return voxelshape;
    }


    /**
     * Slows all entities inside the block.
     */
    @Deprecated
    @Override
    public void onEntityCollision(BlockState blockstate, World world, BlockPos pos, Entity entity) {

        AxisAlignedBB axisalignedbb = getShape(blockstate, world, pos, null).getBoundingBox().offset(pos);
        List<? extends Entity> list = world.getEntitiesIncludingUngeneratedChunks(LivingEntity.class, axisalignedbb);

        if (list.contains(entity)) {
            entity.setMotionMultiplier(blockstate, new Vector3d(0.35D, 0.2F, 0.35D));
        }
    }

    /**
     * Is spot valid (has at least 1 face possible).
     */
    @Override
    public boolean isValidPosition(BlockState blockstate, IWorldReader world, BlockPos pos) {
        return this.hasAtleastOneAttachment(this.setAttachments(blockstate, world, pos));
    }

    /**
     * Returns true if the block has at least one face (it exists).
     */
    private boolean hasAtleastOneAttachment(BlockState blockstate) {
        return this.numberOfAttachments(blockstate) > 0;
    }

    /**
     * How many faces this block has at this time.
     */
    private int numberOfAttachments(BlockState blockstate) {
        int i = 0;

        for (BooleanProperty booleanproperty : FACING_TO_PROPERTY_MAP.values()) {
            if (blockstate.get(booleanproperty)) {
                ++i;
            }
        }

        return i;
    }

    /**
     * Set the state based on solid blocks around it.
     */
    private BlockState setAttachments(BlockState blockstate, IWorldReader blockReader, BlockPos pos) {

        for (Direction direction : Direction.values()) {
            BooleanProperty booleanproperty = FACING_TO_PROPERTY_MAP.get(direction);
            if (blockstate.get(booleanproperty)) {
                boolean flag = canAttachTo(blockReader, pos.offset(direction), direction);
                blockstate = blockstate.with(booleanproperty, flag);
            }
        }

        return blockstate;
    }

    /**
     * allows player to add more faces to this block based on player's direction.
     */
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState currentBlockstate = context.getWorld().getBlockState(context.getPos());
        boolean isSameBlock = currentBlockstate.getBlock() == this;
        BlockState newBlockstate = isSameBlock ? currentBlockstate : this.getDefaultState();

        for (Direction direction : context.getNearestLookingDirections()) {
            BooleanProperty booleanproperty = FACING_TO_PROPERTY_MAP.get(direction);
            boolean faceIsAlreadyTrue = isSameBlock && currentBlockstate.get(booleanproperty);
            if (!faceIsAlreadyTrue && VineBlock.canAttachTo(context.getWorld(), context.getPos().offset(direction), direction)) {
                return newBlockstate.with(booleanproperty, true);
            }
        }

        return isSameBlock ? newBlockstate : null;
    }

    /**
     * double check to make sure this block has at least one face and can attach.
     */
    @Override
    public BlockState updatePostPlacement(BlockState blockstate, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos) {
        BlockState newBlockstate = this.setAttachments(blockstate, world, currentPos);
        return !this.hasAtleastOneAttachment(newBlockstate) ? Blocks.AIR.getDefaultState() : newBlockstate;
    }

    /**
     * Destroyed by pistons.
     */
    @Override
    public PushReaction getPushReaction(BlockState blockstate) {
        return PushReaction.DESTROY;
    }

    /**
     * tell redstone that this can be use with comparator
     */
    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return true;
    }

    /**
     * the power fed into comparator (1 - 4)
     */
    @Override
    public int getComparatorInputOverride(BlockState blockstate, World world, BlockPos pos) {
        return numberOfAttachments(blockstate);
    }


    /**
     * This block is full of holes and can let light through
     */
    @Override
    public int getOpacity(BlockState state, IBlockReader world, BlockPos pos) {
        return 1;
    }

    /**
     * Allow player to remove this block with water buckets, water bottles, or wet sponges
     */
    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onUse(BlockState blockstate, World world, BlockPos position, PlayerEntity playerEntity, Hand playerHand, BlockRayTraceResult raytraceResult) {
        ItemStack itemstack = playerEntity.getHeldItem(playerHand);

        if ((itemstack.getItem() instanceof BucketItem &&
                ((BucketItem) itemstack.getItem()).getFluid().isIn(FluidTags.WATER)) ||
                itemstack.getOrCreateTag().getString("Potion").contains("water") ||
                itemstack.getItem() == Items.WET_SPONGE ||
                itemstack.getItem() == BzItems.SUGAR_WATER_BOTTLE) {

            world.destroyBlock(position, false);

            world.playSound(playerEntity, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(),
                    SoundEvents.ENTITY_PHANTOM_SWOOP, SoundCategory.NEUTRAL, 1.0F, 1.0F);

            if (world instanceof ServerWorld) {
                if (blockstate.get(UP)) {
                    ((ServerWorld) world).spawnParticle(
                            (ServerPlayerEntity) playerEntity,
                            ParticleTypes.FALLING_WATER,
                            true,
                            position.getX() + 0.5D,
                            position.getY() + 0.95D,
                            position.getZ() + 0.5D,
                            6,
                            0.3D,
                            0.0D,
                            0.3D,
                            1);
                }

                if (blockstate.get(NORTH)) {
                    ((ServerWorld) world).spawnParticle(
                            (ServerPlayerEntity) playerEntity,
                            ParticleTypes.FALLING_WATER,
                            true,
                            position.getX() + 0.5D,
                            position.getY() + 0.5D,
                            position.getZ() + 0.05D,
                            6,
                            0.3D,
                            0.3D,
                            0.0D,
                            1);
                }

                if (blockstate.get(EAST)) {
                    ((ServerWorld) world).spawnParticle(
                            (ServerPlayerEntity) playerEntity,
                            ParticleTypes.FALLING_WATER,
                            true,
                            position.getX() + 0.95D,
                            position.getY() + 0.5D,
                            position.getZ() + 0.5D,
                            6, 0.0D,
                            0.3D,
                            0.3D,
                            1);
                }

                if (blockstate.get(SOUTH)) {
                    ((ServerWorld) world).spawnParticle(
                            (ServerPlayerEntity) playerEntity,
                            ParticleTypes.FALLING_WATER,
                            true,
                            position.getX() + 0.5D,
                            position.getY() + 0.5D,
                            position.getZ() + 0.95D,
                            6,
                            0.3D,
                            0.3D,
                            0.0D,
                            1);
                }

                if (blockstate.get(WEST)) {
                    ((ServerWorld) world).spawnParticle(
                            (ServerPlayerEntity) playerEntity,
                            ParticleTypes.FALLING_WATER,
                            true,
                            position.getX() + 0.05D,
                            position.getY() + 0.5D,
                            position.getZ() + 0.5D,
                            6,
                            0.0D,
                            0.3D,
                            0.3D,
                            1);
                }

                if (blockstate.get(DOWN)) {
                    ((ServerWorld) world).spawnParticle(
                            (ServerPlayerEntity) playerEntity,
                            ParticleTypes.FALLING_WATER,
                            true,
                            position.getX() + 0.5D,
                            position.getY() + 0.05D,
                            position.getZ() + 0.5D,
                            6,
                            0.3D,
                            0.0D,
                            0.3D,
                            1);
                }
            }

            return ActionResultType.SUCCESS;
        }

        return super.onUse(blockstate, world, position, playerEntity, playerHand, raytraceResult);
    }

}
