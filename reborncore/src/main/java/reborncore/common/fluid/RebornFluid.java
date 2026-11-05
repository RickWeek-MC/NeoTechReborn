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

package reborncore.common.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Port note: NeoForge requires every fluid to have a {@link FluidType}, which Fabric has no
 * equivalent of. RebornFluidManager creates and registers one per fluid and hands it back here, so
 * the constructor signature Tech Reborn uses is unchanged.
 */
public abstract class RebornFluid extends FlowingFluid {

	private final boolean still;

	private final FluidSettings fluidSettings;
	private final Supplier<RebornFluidBlock> fluidBlockSupplier;
	private final Supplier<RebornBucketItem> bucketItemSuppler;
	private final Supplier<RebornFluid> flowingSuppler;
	private final Supplier<RebornFluid> stillSuppler;

	private Supplier<FluidType> fluidType;

	public RebornFluid(boolean still, FluidSettings fluidSettings, Supplier<RebornFluidBlock> fluidBlockSupplier, Supplier<RebornBucketItem> bucketItemSuppler, Supplier<RebornFluid> flowingSuppler, Supplier<RebornFluid> stillSuppler) {
		this.still = still;
		this.fluidSettings = fluidSettings;
		this.fluidBlockSupplier = fluidBlockSupplier;
		this.bucketItemSuppler = bucketItemSuppler;
		this.flowingSuppler = flowingSuppler;
		this.stillSuppler = stillSuppler;
	}

	/**
	 * Called by {@link RebornFluidManager} at registration time.
	 */
	void setFluidType(Supplier<FluidType> fluidType) {
		this.fluidType = fluidType;
	}

	@Override
	public FluidType getFluidType() {
		if (fluidType == null) {
			throw new IllegalStateException("Fluid was never registered through RebornFluidManager, so it has no FluidType");
		}
		return fluidType.get();
	}

	public FluidSettings getFluidSettings() {
		return fluidSettings;
	}

	@Override
	public RebornFluid getFlowing() {
		return flowingSuppler.get();
	}

	@Override
	public RebornFluid getSource() {
		return stillSuppler.get();
	}

	@Override
	protected boolean canConvertToSource(Level world) {
		return false;
	}

	@Override
	public boolean isSource(FluidState fluidState) {
		return still;
	}

	@Override
	protected void beforeDestroyingBlock(LevelAccessor world, BlockPos pos, BlockState state) {
		// Should behave same as water
		BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
		Block.dropResources(state, world, pos, blockEntity);
	}

	@Override
	protected int getSlopeFindDistance(LevelReader world) {
		return 4;
	}

	@Override
	protected int getDropOff(LevelReader world) {
		return 1;
	}

	@Override
	public Item getBucket() {
		return bucketItemSuppler.get();
	}

	@Override
	protected boolean canBeReplacedWith(FluidState fluidState, BlockGetter blockView, BlockPos blockPos, Fluid fluid, Direction direction) {
		// Same as vanilla water
		return direction == Direction.DOWN && !this.isSame(fluid);
	}

	@Override
	public boolean isSame(Fluid fluid) {
		return getFlowing() == fluid || getSource() == fluid;
	}

	@Override
	public int getTickDelay(LevelReader world) {
		return 10;
	}

	@Override
	protected float getExplosionResistance() {
		return 100F;
	}

	@Override
	protected BlockState createLegacyBlock(FluidState fluidState) {
		return fluidBlockSupplier.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
	}

	@Override
	public int getAmount(FluidState fluidState) {
		return still ? 8 : fluidState.getValue(LEVEL);
	}

	@Override
	protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> stateBuilder) {
		super.createFluidStateDefinition(stateBuilder);
		if (!still) {
			stateBuilder.add(LEVEL);
		}
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL);
	}

}
