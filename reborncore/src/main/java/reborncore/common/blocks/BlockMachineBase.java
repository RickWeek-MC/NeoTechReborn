/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 TeamReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package reborncore.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import reborncore.api.ToolManager;
import reborncore.api.blockentity.IMachineGuiHandler;
import reborncore.api.blockentity.IUpgrade;
import reborncore.api.blockentity.IUpgradeable;
import reborncore.common.BaseBlockEntityProvider;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.fluid.FluidUtils;
import reborncore.common.util.ItemHandlerUtils;
import reborncore.common.util.Tank;
import reborncore.common.util.WrenchUtils;

public abstract class BlockMachineBase extends BaseBlockEntityProvider implements WorldlyContainerHolder {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	boolean hasCustomStates;

	public BlockMachineBase(BlockBehaviour.Properties builder) {
		this(builder, false);
	}

	public BlockMachineBase(BlockBehaviour.Properties builder, boolean hasCustomStates) {
		super(builder);
		this.hasCustomStates = hasCustomStates;
		if (!hasCustomStates) {
			this.registerDefaultState(
					this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
		}
		BlockWrenchEventHandler.wrenchableBlocks.add(this);
	}

	public void setFacing(Direction facing, Level world, BlockPos pos) {
		if (hasCustomStates) {
			return;
		}
		world.setBlockAndUpdate(pos, world.getBlockState(pos).setValue(FACING, facing));
	}

	public Direction getFacing(BlockState state) {
		return state.getValue(FACING);
	}

	public void setActive(Boolean active, Level world, BlockPos pos) {
		if (hasCustomStates) {
			return;
		}
		Direction facing = world.getBlockState(pos).getValue(FACING);
		BlockState state = world.getBlockState(pos).setValue(ACTIVE, active).setValue(FACING, facing);
		world.setBlock(pos, state, 3);
	}

	public boolean isActive(BlockState state) {
		return state.getValue(ACTIVE);
	}

	public boolean isAdvanced() {
		return false;
	}

	public abstract IMachineGuiHandler getGui();

	// BaseBlockEntityProvider
	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(worldIn, pos, state, placer, stack);
		setFacing(placer.getDirection().getOpposite(), worldIn, pos);

		BlockEntity blockEntity = worldIn.getBlockEntity(pos);
		if (blockEntity instanceof MachineBaseBlockEntity) {
			((MachineBaseBlockEntity) blockEntity).onPlace(worldIn, pos, state, placer, stack);
		}
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return null;
	}

	// Block
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
	}

	@Override
	protected void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			ItemHandlerUtils.dropContainedItems(worldIn, pos);
			super.onRemove(state, worldIn, pos, newState, isMoving);
		}
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos blockPos, BlockState blockState, Player playerEntity) {
		BlockEntity blockEntity = world.getBlockEntity(blockPos);
		if (blockEntity instanceof MachineBaseBlockEntity) {
			((MachineBaseBlockEntity) blockEntity).onBreak(world, playerEntity, blockPos, blockState);
		}
		return super.playerWillDestroy(world, blockPos, blockState, playerEntity);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
		return AbstractContainerMenu.getRedstoneSignalFromContainer(getContainer(state, world, pos));
	}


	/**
	 *
	 * 	Right-click should open GUI for all non-wrench items
	 * 	Shift-Right-click should apply special action, like fill\drain bucket, install behavior, etc.
	 *
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player playerIn, BlockHitResult hitResult) {

		ItemStack stack = playerIn.getItemInHand(InteractionHand.MAIN_HAND);
		BlockEntity blockEntity = worldIn.getBlockEntity(pos);

		// We extended BlockTileBase. Thus, we should always have blockEntity entity. I hope.
		if (blockEntity == null) {
			return InteractionResult.PASS;
		}

		if (blockEntity instanceof MachineBaseBlockEntity) {
			Tank tank = ((MachineBaseBlockEntity) blockEntity).getTank();
			if (tank != null && FluidUtils.interactWithFluidHandler(playerIn, tank)) {
				return InteractionResult.SUCCESS;
			}
		}

		if (!stack.isEmpty()) {
			if (ToolManager.INSTANCE.canHandleTool(stack)) {
				if (WrenchUtils.handleWrench(stack, worldIn, pos, playerIn, hitResult.getDirection())) {
					return InteractionResult.SUCCESS;
				}
			} else if (stack.getItem() instanceof IUpgrade && blockEntity instanceof IUpgradeable upgradeableEntity) {
				if (upgradeableEntity.canBeUpgraded()) {
					ItemStack offered = stack.copy();
					ItemStack remainder = ItemHandlerHelper.insertItem(
							new InvWrapper(upgradeableEntity.getUpgradeInventory()),
							offered,
							false
					);
					int inserted = offered.getCount() - remainder.getCount();
					if (inserted > 0) {
						stack.shrink(inserted);
						return InteractionResult.sidedSuccess(worldIn.isClientSide());
					}
				}
			}
		}

		if (getGui() != null && !playerIn.isShiftKeyDown()) {
			getGui().open(playerIn, pos, worldIn);
			return InteractionResult.SUCCESS;
		}

		return super.useWithoutItem(state, worldIn, pos, playerIn, hitResult);
	}

	/**
	 * Port note: upstream carried its own insertItemStacked because Fabric's transfer API had no
	 * equivalent yet. NeoForge's ItemHandlerHelper.insertItem already fills partial stacks first,
	 * so the helper is gone and callers use that directly.
	 */
	public static int insertItemStacked(IItemHandler inventory, ItemStack stack) {
		ItemStack offered = stack.copy();
		ItemStack remainder = ItemHandlerHelper.insertItem(inventory, offered, false);
		return offered.getCount() - remainder.getCount();
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	// WorldlyContainerHolder
	@Override
	public WorldlyContainer getContainer(BlockState blockState, LevelAccessor world, BlockPos blockPos) {
		BlockEntity blockEntity = world.getBlockEntity(blockPos);
		if (blockEntity instanceof MachineBaseBlockEntity) {
			return (MachineBaseBlockEntity) blockEntity;
		}
		return null;
	}
}
