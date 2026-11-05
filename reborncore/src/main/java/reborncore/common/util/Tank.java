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

package reborncore.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import reborncore.common.fluid.FluidValue;
import reborncore.common.fluid.container.FluidInstance;
import reborncore.common.screen.Syncable;
import reborncore.common.util.serialization.SerializationUtil;

import java.util.function.UnaryOperator;

/**
 * A single-slot fluid tank.
 * <p>
 * Port note: upstream implemented Fabric's SingleSlotStorage on top of SnapshotParticipant, where a
 * transaction could roll an insert back. NeoForge has no transactions: IFluidHandler asks for
 * SIMULATE or EXECUTE instead, so the snapshot machinery is gone and simulation simply computes the
 * result without writing it.
 */
public class Tank implements Syncable, IFluidHandler {
	private final String name;
	private FluidInstance fluidInstance = new FluidInstance();
	private final FluidValue capacity;

	public Tank(String name, FluidValue capacity) {
		super();
		this.name = name;
		this.capacity = capacity;
	}

	@NotNull
	public FluidInstance getFluidInstance() {
		return fluidInstance;
	}

	@NotNull
	public Fluid getFluid() {
		return getFluidInstance().fluid();
	}

	public FluidValue getFluidValueCapacity() {
		return capacity;
	}

	public FluidValue getFreeSpace() {
		return getFluidValueCapacity().subtract(getFluidAmount());
	}

	public boolean canFit(Fluid fluid, FluidValue amount) {
		return (isEmpty() || getFluid() == fluid) && getFreeSpace().equalOrMoreThan(amount);
	}

	public boolean isEmpty() {
		return getFluidInstance().isEmpty();
	}

	public boolean isFull() {
		return !getFluidInstance().isEmpty() && getFluidInstance().getAmount().equalOrMoreThan(getFluidValueCapacity());
	}

	public final CompoundTag write(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
		CompoundTag tankData = SerializationUtil.writeNbt(FluidInstance.CODEC, fluidInstance, wrapperLookup);
		nbt.put(name, tankData);
		return nbt;
	}

	public void setFluidAmount(FluidValue amount) {
		if (!fluidInstance.isEmptyFluid()) {
			modifyFluid(fluidInstance -> fluidInstance.withAmount(amount));
		}
	}

	public final Tank read(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
		if (nbt.contains(name)) {
			// allow reading empty tanks
			setFluid(Fluids.EMPTY);

			CompoundTag tankData = nbt.getCompound(name);
			fluidInstance = SerializationUtil.parseNbt(FluidInstance.CODEC, tankData, wrapperLookup, () -> FluidInstance.EMPTY, "tank data");
		}
		return this;
	}

	public void setFluid(@NotNull Fluid f) {
		modifyFluid(fluidInstance -> fluidInstance.withFluid(f));
	}

	@Override
	public void configureSync(Context context) {
		context.sync(FluidInstance.PACKET_CODEC, this::getFluidInstance, this::setFluidInstance);
	}

	public FluidValue getFluidAmount() {
		return getFluidInstance().getAmount();
	}

	public void modifyFluid(UnaryOperator<FluidInstance> operator) {
		setFluidInstance(operator.apply(fluidInstance));
	}

	public void setFluidInstance(@NotNull FluidInstance fluidInstance) {
		this.fluidInstance = fluidInstance;
	}

	// ---- IFluidHandler ----

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public @NotNull FluidStack getFluidInTank(int tank) {
		return fluidInstance.toFluidStack();
	}

	@Override
	public int getTankCapacity(int tank) {
		return capacity.getMillibuckets();
	}

	@Override
	public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
		return isEmpty() || stack.getFluid() == getFluid();
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) {
			return 0;
		}

		if (!isEmpty() && resource.getFluid() != getFluid()) {
			return 0;
		}

		FluidValue offered = FluidValue.fromMillibuckets(resource.getAmount());
		FluidValue accepted = offered.min(getFreeSpace());

		int filled = accepted.getMillibuckets();

		if (filled <= 0) {
			return 0;
		}

		if (action.execute()) {
			final Fluid fluid = resource.getFluid();
			final FluidValue added = FluidValue.fromMillibuckets(filled);
			if (isEmpty()) {
				setFluidInstance(new FluidInstance(fluid, added));
			} else {
				modifyFluid(instance -> instance.addAmount(added));
			}
		}

		return filled;
	}

	@Override
	public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || isEmpty() || resource.getFluid() != getFluid()) {
			return FluidStack.EMPTY;
		}

		return drain(resource.getAmount(), action);
	}

	@Override
	public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain <= 0 || isEmpty()) {
			return FluidStack.EMPTY;
		}

		int drained = Math.min(maxDrain, getFluidAmount().getMillibuckets());

		if (drained <= 0) {
			return FluidStack.EMPTY;
		}

		final Fluid fluid = getFluid();

		if (action.execute()) {
			final FluidValue removed = FluidValue.fromMillibuckets(drained);
			modifyFluid(instance -> instance.subtractAmount(removed));
			if (getFluidAmount().isEmpty()) {
				setFluidInstance(FluidInstance.EMPTY);
			}
		}

		return new FluidStack(fluid, drained);
	}
}
