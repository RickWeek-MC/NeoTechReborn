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
 *
 * NeoForge port (c) 2026 RickWeek, based on the upstream Fabric sources.
 */

package reborncore.common.powerSystem;

import net.minecraft.world.item.ItemStack;

/**
 * Port note: replacement for team.reborn.energy.api.base.SimpleEnergyItem from the Fabric energy
 * API. The method names are kept exactly as upstream, because Tech Reborn calls them in around
 * eighty places.
 * <p>
 * Energy lives in the {@link RebornEnergyComponents#ENERGY} data component.
 */
public interface SimpleEnergyItem {

	/**
	 * @return the maximum amount of energy this stack can hold.
	 */
	long getEnergyCapacity(ItemStack stack);

	/**
	 * @return how much energy may be inserted per operation.
	 */
	long getEnergyMaxInput(ItemStack stack);

	/**
	 * @return how much energy may be extracted per operation.
	 */
	long getEnergyMaxOutput(ItemStack stack);

	default long getStoredEnergy(ItemStack stack) {
		return getStoredEnergyUnchecked(stack);
	}

	default void setStoredEnergy(ItemStack stack, long newAmount) {
		setStoredEnergyUnchecked(stack, Math.max(0, Math.min(newAmount, getEnergyCapacity(stack))));
	}

	/**
	 * Takes {@code amount} out of the stack, all or nothing.
	 *
	 * @return true when the full amount was available and has been taken.
	 */
	default boolean tryUseEnergy(ItemStack stack, long amount) {
		long newAmount = getStoredEnergy(stack) - amount;

		if (newAmount < 0) {
			return false;
		}

		setStoredEnergy(stack, newAmount);
		return true;
	}

	static long getStoredEnergyUnchecked(ItemStack stack) {
		return stack.getOrDefault(RebornEnergyComponents.ENERGY.get(), 0L);
	}

	static void setStoredEnergyUnchecked(ItemStack stack, long newAmount) {
		if (newAmount <= 0) {
			stack.remove(RebornEnergyComponents.ENERGY.get());
		} else {
			stack.set(RebornEnergyComponents.ENERGY.get(), newAmount);
		}
	}
}
