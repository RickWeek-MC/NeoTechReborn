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
package reborncore.common.powerSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import reborncore.api.IListInfoProvider;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.blockentity.RedstoneConfiguration;
import reborncore.common.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Port note: upstream stored energy in Fabric's SimpleSidedEnergyContainer. Here the amount is a
 * plain field and {@link #getSideEnergyStorage(Direction)} hands out a per-side Forge Energy view.
 * RebornCore counts in longs and Forge Energy in ints, so everything crossing that view is clamped.
 */
public abstract class PowerAcceptorBlockEntity extends MachineBaseBlockEntity implements IListInfoProvider {

	private long energy;
	private RcEnergyTier blockEntityPowerTier;

	public long extraPowerStorage;
	public long extraPowerInput;
	public int extraTier;
	public long powerChange;
	public long powerLastTick;
	public boolean checkOverfill = true; // Set false to disable overfill check.

	// Lazily created Forge Energy views: one per Direction ordinal, index 6 is the null side.
	private final IEnergyStorage[] sideEnergyStorages = new IEnergyStorage[7];

	public PowerAcceptorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		checkTier();
	}

	private static int clampToInt(long value) {
		return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
	}

	/**
	 * A Forge Energy view of this machine for one side. Hand this to
	 * RebornCapabilities.registerMachines.
	 */
	public IEnergyStorage getSideEnergyStorage(@Nullable Direction side) {
		// The views hold no state of their own, so one per side (and one for null) is enough.
		int index = side == null ? 6 : side.ordinal();
		IEnergyStorage storage = sideEnergyStorages[index];
		if (storage == null) {
			storage = new SideEnergyStorage(side);
			sideEnergyStorages[index] = storage;
		}
		return storage;
	}

	private class SideEnergyStorage implements IEnergyStorage {
		@Nullable
		private final Direction side;

		private SideEnergyStorage(@Nullable Direction side) {
			this.side = side;
		}

		@Override
		public int receiveEnergy(int toReceive, boolean simulate) {
			if (toReceive <= 0) {
				return 0;
			}

			long accepted = Math.min(Math.min(toReceive, getFreeSpace()), getMaxInput(side));

			if (accepted <= 0) {
				return 0;
			}

			if (!simulate) {
				setStored(getStored() + accepted);
			}

			return clampToInt(accepted);
		}

		@Override
		public int extractEnergy(int toExtract, boolean simulate) {
			if (toExtract <= 0 || !canProvideEnergy(side)) {
				return 0;
			}

			long extracted = Math.min(Math.min(toExtract, getStored()), getMaxOutput(side));

			if (extracted <= 0) {
				return 0;
			}

			if (!simulate) {
				setStored(getStored() - extracted);
			}

			return clampToInt(extracted);
		}

		@Override
		public int getEnergyStored() {
			return clampToInt(getStored());
		}

		@Override
		public int getMaxEnergyStored() {
			return clampToInt(getMaxStoredPower());
		}

		@Override
		public boolean canExtract() {
			return getMaxOutput(side) > 0;
		}

		@Override
		public boolean canReceive() {
			return getMaxInput(side) > 0;
		}
	}

	public void checkTier() {
		if (this.getMaxInput(null) == 0) {
			blockEntityPowerTier = RcEnergyTier.getTier(this.getBaseMaxOutput());
		} else {
			blockEntityPowerTier = RcEnergyTier.getTier(this.getBaseMaxInput());
		}
	}

	/**
	 * Get amount of missing energy
	 *
	 * @return {@code long} Free space for energy in internal buffer
	 */
	public long getFreeSpace() {
		return getMaxStoredPower() - getStored();
	}

	/**
	 * Adds energy to block entity
	 *
	 * @param amount {@code long} Amount to add
	 */
	public void addEnergy(long amount){
		setStored(getEnergy() + amount);
	}

	public void addEnergyProbabilistic(double amount) {
		long integerPart = (long) Math.floor(amount);
		addEnergy(integerPart);

		double fractionalPart = amount - integerPart;
		if (ThreadLocalRandom.current().nextDouble() <= fractionalPart) {
			addEnergy(1);
		}
	}

	/**
	 * Use energy from block entity
	 *
	 * @param amount {@code long} Amount of energy to use
	 */
	public void useEnergy(long amount){
		if (getEnergy() > amount) {
			setStored(getEnergy() - amount);
		} else {
			setStored(0);
		}
	}

	/**
	 * Charge machine from battery placed inside inventory slot
	 *
	 * @param slot {@code int} Slot ID for battery slot
	 */
	public void charge(int slot) {
		if (level == null || level.isClientSide) {
			return;
		}

		long chargeEnergy = Math.min(getFreeSpace(), getMaxInput(null));
		if (chargeEnergy <= 0) {
			return;
		}
		if (getOptionalInventory().isEmpty()) {
			return;
		}
		Container inventory = getOptionalInventory().get();
		ItemStack batteryStack = inventory.getItem(slot);
		if (batteryStack.isEmpty()) {
			return;
		}

		IEnergyStorage battery = batteryStack.getCapability(Capabilities.EnergyStorage.ITEM);
		if (battery == null) {
			return;
		}

		moveEnergy(battery, getSideEnergyStorage(null), clampToInt(chargeEnergy));
	}

	/**
	 * Charge battery placed inside inventory slot from machine
	 *
	 * @param slot {@code int} Slot ID for battery slot
	 */
	public void discharge(int slot) {
		if (level == null || level.isClientSide) {
			return;
		}

		if (getOptionalInventory().isEmpty()){
			return;
		}

		Container inventory = getOptionalInventory().get();
		ItemStack batteryStack = inventory.getItem(slot);
		if (batteryStack.isEmpty()) {
			return;
		}

		IEnergyStorage battery = batteryStack.getCapability(Capabilities.EnergyStorage.ITEM);
		if (battery == null) {
			return;
		}

		moveEnergy(getSideEnergyStorage(null), battery, Integer.MAX_VALUE);
	}

	/**
	 * Moves at most {@code maxAmount} from one storage to the other.
	 * <p>
	 * Port note: replaces EnergyStorageUtil.move. The source is only drained for real once the
	 * destination says how much it accepted, so nothing can be created or lost in between.
	 */
	private static int moveEnergy(@Nullable IEnergyStorage from, @Nullable IEnergyStorage to, int maxAmount) {
		if (from == null || to == null || maxAmount <= 0) {
			return 0;
		}

		int available = from.extractEnergy(maxAmount, true);
		if (available <= 0) {
			return 0;
		}

		int accepted = to.receiveEnergy(available, true);
		if (accepted <= 0) {
			return 0;
		}

		int extracted = from.extractEnergy(accepted, false);
		if (extracted <= 0) {
			return 0;
		}

		int inserted = to.receiveEnergy(extracted, false);

		if (inserted < extracted) {
			// Should not happen, but never destroy the difference.
			from.receiveEnergy(extracted - inserted, false);
		}

		return inserted;
	}

	/**
	 * Calculates the comparator output of a powered BE with the formula
	 * <pre>
	 *  ceil(blockEntity.getStored() * 15.0 / storage.getMaxPower())
	 * </pre>
	 *
	 * @param blockEntity {@link BlockEntity} the powered BE
	 * @return {@code int} the calculated comparator output or 0 if {@link BlockEntity}
	 * is not a {@link PowerAcceptorBlockEntity}
	 */
	public static int calculateComparatorOutputFromEnergy(@Nullable BlockEntity blockEntity) {
		if (blockEntity instanceof PowerAcceptorBlockEntity storage) {
			return Mth.ceil(storage.getStored() * 15.0 / storage.getMaxStoredPower());
		} else {
			return 0;
		}
	}

	/**
	 * Check if machine should load energy data from NBT
	 */
	protected boolean shouldHandleEnergyNBT() {
		return true;
	}

	/**
	 * Check if block entity can accept energy from a particular side
	 */
	protected boolean canAcceptEnergy(@Nullable Direction side){
		return true;
	}

	/**
	 * Check if block entity can provide energy via a particular side
	 */
	protected boolean canProvideEnergy(@Nullable Direction side){
		return true;
	}

	public long getExtraPowerStorage(){
		return extraPowerStorage;
	}

	public void setExtraPowerStorage(long extraPowerStorage) {
		this.extraPowerStorage = extraPowerStorage;
	}

	public long getPowerChange() {
		return powerChange;
	}

	public void setPowerChange(long powerChange) {
		this.powerChange = powerChange;
	}

	public long getEnergy() {
		return getStored();
	}

	public void setEnergy(long energy) {
		setStored(energy);
	}

	/**
	 * Returns base size of internal Energy buffer of a particular machine before any upgrades applied
	 */
	public abstract long getBaseMaxPower();

	/**
	 * Returns base output rate or zero if machine doesn't output energy
	 */
	public abstract long getBaseMaxOutput();

	/**
	 * Returns base input rate or zero if machine doesn't accept energy
	 */
	public abstract long getBaseMaxInput();

	// MachineBaseBlockEntity
	@Override
	public void tick(Level world, BlockPos pos, BlockState state, MachineBaseBlockEntity blockEntity2) {
		super.tick(world, pos, state, blockEntity2);
		if (world == null || world.isClientSide) {
			return;
		}
		if (getStored() <= 0) {
			return;
		}
		if (!isActive(RedstoneConfiguration.Element.POWER_IO)) {
			return;
		}

		for (Direction side : Direction.values()) {
			// Check the output rate first: machines that cannot give energy away (most of them)
			// used to do a neighbour capability lookup on all 6 sides every tick for nothing.
			int maxOutput = clampToInt(getMaxOutput(side));
			if (maxOutput <= 0) {
				continue;
			}

			IEnergyStorage neighbour = world.getCapability(
				Capabilities.EnergyStorage.BLOCK,
				pos.relative(side),
				side.getOpposite()
			);

			moveEnergy(getSideEnergyStorage(side), neighbour, maxOutput);
		}

		powerChange = getStored() - powerLastTick;
		powerLastTick = getStored();
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registryLookup) {
		super.loadAdditional(tag, registryLookup);
		CompoundTag data = tag.getCompound("PowerAcceptor");
		if (shouldHandleEnergyNBT()) {
			// Bypass overfill check in setStored() because upgrades have not yet been applied.
			this.energy = data.getLong("energy");
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registryLookup) {
		super.saveAdditional(tag, registryLookup);
		CompoundTag data = new CompoundTag();
		data.putLong("energy", getStored());
		tag.put("PowerAcceptor", data);
	}

	@Override
	public void resetUpgrades() {
		super.resetUpgrades();
		extraPowerStorage = 0;
		extraTier = 0;
		extraPowerInput = 0;
	}

	@Override
	protected void afterUpgradesApplication() {
		if (checkOverfill && getStored() > getMaxStoredPower()) {
			setStored(getStored());
		}
	}

	public long getStored() {
		return energy;
	}

	public void setStored(long amount) {
		long previous = energy;
		energy = amount;
		if (checkOverfill) {
			energy = Math.max(Math.min(energy, getMaxStoredPower()), 0);
		}
		setChanged();

		// Port note: Fabric called this from the transaction's onFinalCommit.
		if (previous != energy && level != null) {
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	public long getMaxStoredPower() {
		return getBaseMaxPower() + extraPowerStorage;
	}

	public long getMaxOutput(@Nullable Direction face) {
		if (!isActive(RedstoneConfiguration.Element.POWER_IO)) {
			return 0;
		}
		if(!canProvideEnergy(face)) {
			return 0;
		}
		if (this.extraTier > 0) {
			return this.getTier().getMaxOutput();
		}
		return getBaseMaxOutput();
	}

	public long getMaxInput(@Nullable Direction face) {
		if (!isActive(RedstoneConfiguration.Element.POWER_IO)) {
			return 0;
		}
		if (!canAcceptEnergy(face)) {
			return 0;
		}
		if (this.extraTier > 0) {
			return this.getTier().getMaxInput();
		}
		return getBaseMaxInput() + extraPowerInput;
	}

	public RcEnergyTier getTier() {
		if (blockEntityPowerTier == null) {
			checkTier();
		}

		if (extraTier > 0) {
			for (RcEnergyTier enumTier : RcEnergyTier.values()) {
				if (enumTier.ordinal() == blockEntityPowerTier.ordinal() + extraTier) {
					return enumTier;
				}
			}
			return RcEnergyTier.INFINITE;
		}
		return blockEntityPowerTier;
	}

	public boolean tryUseExact(long energy) {
		if (getStored() >= energy) {
			addEnergy(-energy);
			return true;
		} else {
			return false;
		}
	}

	// IListInfoProvider
	@Override
	public void addInfo(List<Component> info, boolean isReal, boolean hasData) {
		if (!isReal && hasData) {
			info.add(
					Component.translatable("reborncore.tooltip.energy")
							.withStyle(ChatFormatting.GRAY)
							.append(": ")
							.append(PowerSystem.getLocalizedPower(getStored()))
							.withStyle(ChatFormatting.GOLD)
			);
		}

		info.add(
				Component.translatable("reborncore.tooltip.energy.maxEnergy")
				.withStyle(ChatFormatting.GRAY)
				.append(": ")
				.append(PowerSystem.getLocalizedPower(getMaxStoredPower()))
				.withStyle(ChatFormatting.GOLD)
		);

		if (getMaxInput(null) != 0) {
			info.add(
					Component.translatable("reborncore.tooltip.energy.inputRate")
							.withStyle(ChatFormatting.GRAY)
							.append(": ")
							.append(PowerSystem.getLocalizedPower(getMaxInput(null)))
							.withStyle(ChatFormatting.GOLD)
			);
		}
		if (getMaxOutput(null) > 0) {
			info.add(
					Component.translatable("reborncore.tooltip.energy.outputRate")
							.withStyle(ChatFormatting.GRAY)
							.append(": ")
							.append(PowerSystem.getLocalizedPower(getMaxOutput(null)))
							.withStyle(ChatFormatting.GOLD)
			);
		}

		info.add(
				Component.translatable("reborncore.tooltip.energy.tier")
						.withStyle(ChatFormatting.GRAY)
						.append(": ")
						.append(StringUtils.toFirstCapitalAllLowercase(getTier().toString()))
						.withStyle(ChatFormatting.GOLD)
		);

		if (isReal) {
			info.add(
					Component.translatable("reborncore.tooltip.energy.change")
							.withStyle(ChatFormatting.GRAY)
							.append(": ")
							.append(PowerSystem.getLocalizedPower(powerChange))
							.append("/t")
							.withStyle(ChatFormatting.GOLD)
			);
		}



		super.addInfo(info, isReal, hasData);
	}
}
