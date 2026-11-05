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

package techreborn.blockentity.storage.energy.idsu;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.screen.BuiltScreenHandler;
import reborncore.common.screen.BuiltScreenHandlerProvider;
import reborncore.common.screen.builder.ScreenHandlerBuilder;
import techreborn.blockentity.storage.energy.EnergyStorageBlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import techreborn.config.TechRebornConfig;
import techreborn.init.TRBlockEntities;
import techreborn.init.TRContent;

public class InterdimensionalSUBlockEntity extends EnergyStorageBlockEntity implements BuiltScreenHandlerProvider {

	public String ownerUdid;

	// This is the energy value that is synced to the client
	private long clientEnergy;

	public InterdimensionalSUBlockEntity(BlockPos pos, BlockState state) {
		super(TRBlockEntities.INTERDIMENSIONAL_SU, pos, state, "IDSU", 2, TRContent.Machine.INTERDIMENSIONAL_SU.block, RcEnergyTier.INSANE, TechRebornConfig.idsuMaxEnergy);
	}

	/**
	 * Nota di porting: in upstream era un DelegatingEnergyStorage di Fabric Energy (long, con transazioni).
	 * Qui e' un IEnergyStorage (int) che scrive direttamente nel conto del giocatore, con i limiti
	 * di ingresso/uscita di questa IDSU.
	 */
	@Override
	public IEnergyStorage getSideEnergyStorage(@Nullable Direction side) {
		if (ownerUdid == null || ownerUdid.isEmpty()) {
			return EmptyEnergyStorage.INSTANCE;
		}
		if (level.isClientSide) {
			// Il conto globale sul client non c'e': un finto contenitore, solo per i controlli di esistenza.
			return EmptyEnergyStorage.INSTANCE;
		}
		return new IEnergyStorage() {
			private IDSUManager.IDSUPlayer account() {
				return IDSUManager.getPlayer(level.getServer(), ownerUdid);
			}

			@Override
			public int receiveEnergy(int toReceive, boolean simulate) {
				long amount = Math.min(toReceive, getMaxInput(side));
				if (simulate) {
					return (int) Math.max(0, Math.min(amount, TechRebornConfig.idsuMaxEnergy - account().getEnergy()));
				}
				return (int) account().insert(amount);
			}

			@Override
			public int extractEnergy(int toExtract, boolean simulate) {
				long amount = Math.min(toExtract, getMaxOutput(side));
				if (simulate) {
					return (int) Math.max(0, Math.min(amount, account().getEnergy()));
				}
				return (int) account().extract(amount);
			}

			@Override
			public int getEnergyStored() {
				return (int) Math.min(Integer.MAX_VALUE, account().getEnergy());
			}

			@Override
			public int getMaxEnergyStored() {
				return TechRebornConfig.idsuMaxEnergy;
			}

			@Override
			public boolean canExtract() {
				return getMaxOutput(side) > 0;
			}

			@Override
			public boolean canReceive() {
				return getMaxInput(side) > 0;
			}
		};
	}

	/** Contenitore vuoto: non accetta ne' da', ma esiste (la IDSU senza proprietario). */
	private enum EmptyEnergyStorage implements IEnergyStorage {
		INSTANCE;

		@Override public int receiveEnergy(int toReceive, boolean simulate) { return 0; }
		@Override public int extractEnergy(int toExtract, boolean simulate) { return 0; }
		@Override public int getEnergyStored() { return 0; }
		@Override public int getMaxEnergyStored() { return 0; }
		@Override public boolean canExtract() { return false; }
		@Override public boolean canReceive() { return false; }
	}

	@Override
	public long getStored() {
		if (ownerUdid == null || ownerUdid.isEmpty()) {
			return 0;
		}
		if (level.isClientSide) {
			return clientEnergy;
		}
		return IDSUManager.getPlayer(level.getServer(), ownerUdid).getEnergy();
	}

	@Override
	public void setStored(long energy) {
		if (ownerUdid == null || ownerUdid.isEmpty()) {
			return;
		}
		if (level.isClientSide) {
			clientEnergy = energy;
		} else {
			IDSUManager.getPlayer(level.getServer(), ownerUdid).setEnergy(energy);
		}
	}

	@Override
	public void useEnergy(long extract) {
		if (ownerUdid == null || ownerUdid.isEmpty()) {
			return;
		}
		if (level.isClientSide) {
			throw new UnsupportedOperationException("cannot set energy on the client!");
		}
		long energy = IDSUManager.getPlayer(level.getServer(), ownerUdid).getEnergy();
		if (extract > energy) {
			extract = energy;
		}

		setStored(energy - extract);
	}

	@Override
	protected boolean shouldHandleEnergyNBT() {
		return false;
	}

	@Override
	public void loadAdditional(CompoundTag nbtCompound, HolderLookup.Provider registryLookup) {
		super.loadAdditional(nbtCompound, registryLookup);
		this.ownerUdid = nbtCompound.getString("ownerUdid");
	}

	@Override
	public void saveAdditional(CompoundTag nbtCompound, HolderLookup.Provider registryLookup) {
		super.saveAdditional(nbtCompound, registryLookup);
		if (ownerUdid == null || StringUtils.isEmpty(ownerUdid)) {
			return;
		}
		nbtCompound.putString("ownerUdid", this.ownerUdid);
	}

	@Override
	public BuiltScreenHandler createScreenHandler(int syncID, final Player player) {
		return new ScreenHandlerBuilder("idsu").player(player.getInventory()).inventory().hotbar().armor()
				.complete(8, 18).addArmor().addInventory().blockEntity(this).energySlot(0, 62, 45).energySlot(1, 98, 45)
				.syncEnergyValue().addInventory().create(this, syncID);
	}
}
