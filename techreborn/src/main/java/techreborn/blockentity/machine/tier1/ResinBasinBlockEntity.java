/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 TechReborn
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

package techreborn.blockentity.machine.tier1;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.util.WorldUtils;
import techreborn.blocks.machine.tier1.ResinBasinBlock;
import techreborn.blocks.misc.BlockRubberLog;
import techreborn.config.TechRebornConfig;
import techreborn.init.ModSounds;
import techreborn.init.TRBlockEntities;
import techreborn.init.TRContent;

public class ResinBasinBlockEntity extends MachineBaseBlockEntity {
	private Direction direction = Direction.NORTH;

	// State
	private boolean isPouring = false;
	private boolean isFull = false;

	private int pouringTimer = 0;

	public ResinBasinBlockEntity(BlockPos pos, BlockState state) {
		super(TRBlockEntities.RESIN_BASIN, pos, state);

		/* TODO is this the right place? */
		this.isFull = state.getValue(ResinBasinBlock.FULL);

		if (state.getValue(ResinBasinBlock.POURING)) {
			this.isPouring = true;
			pouringTimer = TechRebornConfig.sapTimeTicks;
		}
	}

	@Override
	public void tick(Level level, BlockPos pos, BlockState state, MachineBaseBlockEntity blockEntity) {
		super.tick(level, pos, state, blockEntity);
		if (level == null || level.isClientSide) return;

		boolean shouldUpdateState = false;

		if (isPouring) {
			pouringTimer--;

			// Play pouring audio
			if (level.getGameTime() % 20 == 0) {
				level.playSound(null, pos, ModSounds.SAP_EXTRACT.get(), SoundSource.BLOCKS, 1F, 1F);
			}

			if (pouringTimer <= 0) {
				isPouring = false;
				isFull = true;
				shouldUpdateState = true;
			}
		}

		// Try and deposit
		if (isFull) {
			// Get inventory
			IItemHandler invBelow = getInventoryBelow();
			if (invBelow != null) {
				int sentAmount = getSapAmount();
				ItemStack toSend = new ItemStack(TRContent.Parts.SAP, sentAmount);
				ItemStack remainder = ItemHandlerHelper.insertItem(invBelow, toSend, false);
				// Same rule as upstream: if at least one sap went in, the basin counts as emptied.
				if (remainder.getCount() < sentAmount) {
					isFull = false;
					shouldUpdateState = true;
				}
			}
		}

		boolean readyToHarvest = !isFull && !isPouring;

		// Ensuring it's placed on a log
		if ((readyToHarvest || level.getGameTime() % 20 == 0) && !validPlacement()) {
			// Not placed on log, drop on ground
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			WorldUtils.dropItem(TRContent.Machine.RESIN_BASIN.asItem(), level, pos);
			return;
		}

		if (readyToHarvest) {
			// Check for rubber
			if (level.getGameTime() % TechRebornConfig.checkForSapTime == 0) {
				BlockPos targetRubber = getLogWithSap();

				if (targetRubber != null) {
					// We have a valid sap log, harvest it
					level.setBlockAndUpdate(targetRubber, level.getBlockState(targetRubber).setValue(BlockRubberLog.HAS_SAP, false).setValue(BlockRubberLog.SAP_SIDE, Direction.from2DDataValue(0)));
					isPouring = true;
					pouringTimer = TechRebornConfig.sapTimeTicks;
					shouldUpdateState = true;
				}
			}
		}

		if (shouldUpdateState) {
			setPouringState(isPouring);
			setFullState(isFull);
		}
	}

	public int getSapAmount() {
		if (!isFull)
			return 0;
		return (Math.random() <= 0.5) ? 1 : 2;
	}

	public ItemStack empty() {
		if (isFull) {
			int sapAmount = getSapAmount();

			this.isPouring = false;
			this.isFull = false;
			setFullState(false);
			setPouringState(false);

			return new ItemStack(TRContent.Parts.SAP, sapAmount);
		}

		return new ItemStack(TRContent.Parts.SAP, 0);
	}

	@Override
	public void onBreak(Level level, Player playerEntity, BlockPos blockPos, BlockState blockState) {
		super.onBreak(level, playerEntity, blockPos, blockState);

		// Drop a sap if full
		if (this.isFull) {
			ItemStack out = new ItemStack(TRContent.Parts.SAP, getSapAmount());
			WorldUtils.dropItem(out, level, getBlockPos());
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();

		if (level == null || level.isClientSide) return;

		// Set facing
		direction = level.getBlockState(getBlockPos()).getValue(ResinBasinBlock.FACING).getOpposite();
	}

	private IItemHandler getInventoryBelow() {
		return this.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, this.getBlockPos().relative(Direction.DOWN), Direction.UP);
	}

	private boolean validPlacement() {
		return level.getBlockState(this.getBlockPos().relative(direction)).getBlock() == TRContent.RUBBER_LOG;
	}


	private BlockPos getLogWithSap() {
		// Checking origin block
		BlockPos originPos = this.getBlockPos().relative(direction);
		BlockState originState = level.getBlockState(originPos);

		if (originState.getValue(BlockRubberLog.HAS_SAP)) {
			return originPos;
		}

		boolean shouldExit = false;
		BlockPos current = originPos;

		// Progress Up
		while (!shouldExit) {
			current = current.relative(Direction.UP);

			BlockState state = level.getBlockState(current);
			if (state.getBlock() == TRContent.RUBBER_LOG) {
				if (state.getValue(BlockRubberLog.HAS_SAP)) {
					return current;
				}
			} else {
				shouldExit = true;
			}
		}

		current = originPos;
		shouldExit = false;
		// Progress Down
		while (!shouldExit) {
			current = current.relative(Direction.DOWN);

			BlockState state = level.getBlockState(current);
			if (state.getBlock() == TRContent.RUBBER_LOG) {
				if (state.getValue(BlockRubberLog.HAS_SAP)) {
					return current;
				}
			} else {
				shouldExit = true;
			}
		}

		// Could not find a rubber log with sap
		return null;
	}

	private void setPouringState(boolean value) {
		if (level != null) {
			level.setBlockAndUpdate(getBlockPos(), level.getBlockState(getBlockPos()).setValue(ResinBasinBlock.POURING, value));
		}
	}

	private void setFullState(boolean value) {
		if (level != null) {
			level.setBlockAndUpdate(getBlockPos(), level.getBlockState(getBlockPos()).setValue(ResinBasinBlock.FULL, value));
		}
	}

	@Override
	public boolean hasSlotConfig() {
		return false;
	}

	@Override
	public boolean canBeUpgraded() {
		return false;
	}
}
