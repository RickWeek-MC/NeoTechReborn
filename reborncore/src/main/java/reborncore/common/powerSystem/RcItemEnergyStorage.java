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
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Exposes an {@link RcEnergyItem} to the rest of the world as a Forge Energy storage.
 * <p>
 * Port note: RebornCore counts energy in longs, Forge Energy in ints, so everything crossing this
 * boundary is clamped to Integer.MAX_VALUE. Tech Reborn's item capacities are far below that.
 */
public record RcItemEnergyStorage(ItemStack stack, RcEnergyItem item) implements IEnergyStorage {

	private static int clamp(long value) {
		return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
	}

	@Override
	public int receiveEnergy(int toReceive, boolean simulate) {
		if (toReceive <= 0 || !canReceive()) {
			return 0;
		}

		long stored = item.getStoredEnergy(stack);
		long space = item.getEnergyCapacity(stack) - stored;
		long accepted = Math.min(Math.min(toReceive, space), item.getEnergyMaxInput(stack));

		if (accepted <= 0) {
			return 0;
		}

		if (!simulate) {
			item.setStoredEnergy(stack, stored + accepted);
		}

		return clamp(accepted);
	}

	@Override
	public int extractEnergy(int toExtract, boolean simulate) {
		if (toExtract <= 0 || !canExtract()) {
			return 0;
		}

		long stored = item.getStoredEnergy(stack);
		long extracted = Math.min(Math.min(toExtract, stored), item.getEnergyMaxOutput(stack));

		if (extracted <= 0) {
			return 0;
		}

		if (!simulate) {
			item.setStoredEnergy(stack, stored - extracted);
		}

		return clamp(extracted);
	}

	@Override
	public int getEnergyStored() {
		return clamp(item.getStoredEnergy(stack));
	}

	@Override
	public int getMaxEnergyStored() {
		return clamp(item.getEnergyCapacity(stack));
	}

	@Override
	public boolean canExtract() {
		return item.getEnergyMaxOutput(stack) > 0;
	}

	@Override
	public boolean canReceive() {
		return item.getEnergyMaxInput(stack) > 0;
	}
}
