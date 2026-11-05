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

package reborncore.common.fluid.container;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import reborncore.common.fluid.FluidValue;

/**
 * A fluid plus an amount.
 * <p>
 * Port note: upstream wrapped Fabric's FluidVariant (fluid + components). NeoForge has no
 * amount-less equivalent, so this holds a plain {@link Fluid} and the component data is dropped.
 * Nothing in Tech Reborn stored components on a fluid, so no behaviour is lost.
 * {@link #toFluidStack()} and {@link #fromFluidStack(FluidStack)} are the bridge to NeoForge's API.
 */
public record FluidInstance(Fluid fluid, FluidValue amount) {
	public static final FluidInstance EMPTY = new FluidInstance(Fluids.EMPTY, FluidValue.EMPTY);

	public static final Codec<FluidInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidInstance::fluid),
		FluidValue.CODEC.fieldOf("amount").forGetter(FluidInstance::getAmount)
	).apply(instance, FluidInstance::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, FluidInstance> PACKET_CODEC = StreamCodec.composite(
		ByteBufCodecs.registry(Registries.FLUID), FluidInstance::fluid,
		FluidValue.PACKET_CODEC, FluidInstance::getAmount,
		FluidInstance::new
	);

	public FluidInstance(Fluid fluid) {
		this(fluid, FluidValue.EMPTY);
	}

	public FluidInstance() {
		this(Fluids.EMPTY);
	}

	@Override
	public Fluid fluid() {
		if (isEmpty()) return Fluids.EMPTY;
		return fluid;
	}

	public FluidValue getAmount() {
		return amount;
	}

	public FluidInstance withFluid(Fluid fluid) {
		return new FluidInstance(fluid, this.amount);
	}

	public FluidInstance withAmount(FluidValue amount) {
		return new FluidInstance(this.fluid, amount);
	}

	public FluidInstance subtractAmount(FluidValue amount) {
		return new FluidInstance(this.fluid, this.amount.subtract(amount));
	}

	public FluidInstance addAmount(FluidValue amount) {
		return new FluidInstance(this.fluid, this.amount.add(amount));
	}

	public boolean isEmpty() {
		return isEmptyFluid() || this.getAmount().isEmpty();
	}

	public boolean isEmptyFluid() {
		return this.fluid == Fluids.EMPTY;
	}

	/**
	 * This instance as a NeoForge FluidStack. Same unit on both sides, so nothing is converted.
	 */
	public FluidStack toFluidStack() {
		if (isEmpty()) {
			return FluidStack.EMPTY;
		}
		return new FluidStack(fluid, amount.getMillibuckets());
	}

	/**
	 * Builds an instance from a NeoForge FluidStack.
	 */
	public static FluidInstance fromFluidStack(FluidStack stack) {
		if (stack.isEmpty()) {
			return EMPTY;
		}
		return new FluidInstance(stack.getFluid(), FluidValue.fromMillibuckets(stack.getAmount()));
	}
}
