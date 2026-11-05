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

package techreborn.items;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import reborncore.common.fluid.FluidValue;
import techreborn.component.TRDataComponentTypes;

/**
 * The fluid handler of a single cell: one tank holding exactly one bucket, all or nothing.
 * <p>
 * Port note: upstream used Fabric's {@code SingleVariantItemStorage}, which has no NeoForge
 * equivalent, so this is written by hand. The fluid lives in the stack's
 * {@link TRDataComponentTypes#FLUID} component, which is what the rest of Tech Reborn reads, so a
 * cell filled by a machine and a cell filled by {@link DynamicCellItem#getCellWithFluid} are the
 * same item.
 * <p>
 * It follows NeoForge's convention for item handlers: the handler owns the stack it was built from,
 * mutates it for real on {@link net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction#EXECUTE},
 * and hands the result back through {@link #getContainer()}. Callers pass in a copy and only write
 * that copy back once they know the result fits -- see RebornCore's {@code FluidUtils}, which is
 * what the machines go through.
 * <p>
 * Partial moves are refused on purpose: upstream threw on any amount that was not 0 or 1 bucket.
 * Here a partial fill or drain simply reports 0, so the caller gives up and nothing changes.
 */
public class CellFluidHandler implements IFluidHandlerItem {

	/** A cell holds exactly one bucket. */
	public static final int CAPACITY = FluidValue.BUCKET.getMillibuckets();

	private final ItemStack container;

	public CellFluidHandler(ItemStack container) {
		this.container = container;
	}

	@Override
	public ItemStack getContainer() {
		return container;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		Fluid fluid = getFluid();
		return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, CAPACITY);
	}

	@Override
	public int getTankCapacity(int tank) {
		return CAPACITY;
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		// Any fluid fits; a cell that already holds something takes nothing more.
		return true;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || resource.getAmount() < CAPACITY || !isSingleCell()) {
			return 0;
		}

		if (getFluid() != Fluids.EMPTY) {
			return 0;
		}

		if (action.execute()) {
			container.set(TRDataComponentTypes.FLUID, resource.getFluidHolder());
		}

		return CAPACITY;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !resource.is(getFluid())) {
			return FluidStack.EMPTY;
		}

		return drain(resource.getAmount(), action);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		Fluid fluid = getFluid();

		if (fluid == Fluids.EMPTY || maxDrain < CAPACITY || !isSingleCell()) {
			return FluidStack.EMPTY;
		}

		if (action.execute()) {
			container.set(TRDataComponentTypes.FLUID, Fluids.EMPTY.builtInRegistryHolder());
		}

		return new FluidStack(fluid, CAPACITY);
	}

	/**
	 * Filling or emptying changes a component on the whole stack, so it is only safe on a single
	 * cell: on a stack of 16 it would turn all sixteen into one bucket's worth. Callers inside this
	 * mod already hand over a copy of one (RebornCore's {@code FluidUtils}), this is the guard for
	 * anyone who does not.
	 */
	private boolean isSingleCell() {
		return container.getCount() == 1;
	}

	private Fluid getFluid() {
		return container.getOrDefault(TRDataComponentTypes.FLUID, Fluids.EMPTY.builtInRegistryHolder()).value();
	}
}
