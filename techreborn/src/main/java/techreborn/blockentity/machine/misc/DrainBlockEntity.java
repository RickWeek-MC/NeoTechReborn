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

package techreborn.blockentity.machine.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;
import reborncore.api.IToolDrop;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.fluid.FluidValue;
import reborncore.common.fluid.container.FluidInstance;
import reborncore.common.util.Tank;
import techreborn.TechReborn;
import techreborn.config.TechRebornConfig;
import techreborn.init.TRBlockEntities;
import techreborn.init.TRContent;

/**
 * Takes one bucket of fluid out of the block above it (a fluid source, or a cauldron).
 * <p>
 * Port notes:
 * - FluidDrainable is BucketPickup and tryDrainFluid is pickupBlock; LeveledCauldronBlock is
 *   LayeredCauldronBlock (and {@code state.contains(p)} is {@code hasProperty(p)}).
 * - VINCOLO 6 of the notes: upstream asked the picked-up item {@code instanceof ItemFluidInfo} to
 *   learn which fluid it held, which worked only because a mixin made the vanilla bucket implement
 *   it. Here the fluid is read from the NeoForge item fluid capability, which both the vanilla
 *   bucket and our own cell expose (CellFluidHandler, proven in T9/T9 testing).
 * - upstream's local variable for the cauldron level was called {@code level}, which here would
 *   shadow the {@code level} field of BlockEntity: renamed to {@code cauldronLevel}.
 */
public class DrainBlockEntity extends MachineBaseBlockEntity implements IToolDrop {

	protected final Tank internalTank = new Tank("tank", FluidValue.BUCKET);

	public DrainBlockEntity(BlockPos pos, BlockState state) {
		super(TRBlockEntities.DRAIN, pos, state);
	}

	@Override
	public void tick(Level level, BlockPos pos, BlockState state, MachineBaseBlockEntity blockEntity) {
		super.tick(level, pos, state, blockEntity);
		if (level == null || level.isClientSide) {
			return;
		}

		int ticks = TechRebornConfig.ticksUntilNextDrainAttempt;
		if (ticks > 0 && level.getGameTime() % ticks == 0) {

			if (internalTank.isEmpty()) {
				tryDrain();
			}
		}
	}

	@Nullable
	@Override
	public Tank getTank() {
		return internalTank;
	}

	private void tryDrain() {
		// Position above drain
		BlockPos above = this.getBlockPos().above();

		// Block and state above drain
		BlockState aboveBlockState = level.getBlockState(above);
		Block aboveBlock = aboveBlockState.getBlock();

		if (aboveBlock instanceof BucketPickup bucketPickup) {
			ItemStack fluidContainer = bucketPickup.pickupBlock(null, level, above, aboveBlockState);
			Fluid drainFluid = getFluidOf(fluidContainer);
			if (drainFluid != null) {
				internalTank.setFluidInstance(new FluidInstance(drainFluid, FluidValue.BUCKET));
			} else {
				TechReborn.LOGGER.debug("Could not get Fluid from ItemStack {}", fluidContainer.getItem());
			}
		}
		if (aboveBlock instanceof LayeredCauldronBlock && aboveBlockState.is(Blocks.WATER_CAULDRON)) { //ensure Water cauldron
			Fluid drainFluid = Fluids.WATER;
			int cauldronLevel;
			if (aboveBlockState.hasProperty(LayeredCauldronBlock.LEVEL)) {
				cauldronLevel = aboveBlockState.getValue(LayeredCauldronBlock.LEVEL);
			}
			else {
				return;
			}
			level.setBlockAndUpdate(above, Blocks.CAULDRON.defaultBlockState());
			internalTank.setFluidInstance(
				new FluidInstance(drainFluid, FluidValue.BUCKET.fraction(3).multiply(cauldronLevel))
			);
		}
		if (aboveBlock instanceof LavaCauldronBlock) {
			level.setBlockAndUpdate(above, Blocks.CAULDRON.defaultBlockState());
			internalTank.setFluidInstance(
				new FluidInstance(Fluids.LAVA, FluidValue.BUCKET)
			);
		}
	}

	/** The fluid a filled container holds, from the item fluid capability, or null if it holds none. */
	@Nullable
	public static Fluid getFluidOf(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
		if (handler == null || handler.getTanks() == 0) {
			return null;
		}
		FluidStack fluidStack = handler.getFluidInTank(0);
		return fluidStack.isEmpty() ? null : fluidStack.getFluid();
	}

	@Override
	public ItemStack getToolDrop(Player p0) {
		return TRContent.Machine.DRAIN.getStack();
	}

	@Override
	public void saveAdditional(CompoundTag tagCompound, HolderLookup.Provider registryLookup) {
		super.saveAdditional(tagCompound, registryLookup);
		internalTank.write(tagCompound, registryLookup);
	}

	@Override
	public void loadAdditional(CompoundTag tagCompound, HolderLookup.Provider registryLookup) {
		super.loadAdditional(tagCompound, registryLookup);
		internalTank.read(tagCompound, registryLookup);
	}
}
