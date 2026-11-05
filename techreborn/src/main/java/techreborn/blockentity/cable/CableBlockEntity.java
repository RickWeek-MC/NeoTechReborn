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

package techreborn.blockentity.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import reborncore.api.IListInfoProvider;
import reborncore.api.IToolDrop;
import reborncore.common.network.NetworkManager;
import reborncore.common.network.clientbound.CustomDescriptionPayload;
import reborncore.common.powerSystem.PowerSystem;
import reborncore.common.util.StringUtils;
import techreborn.blocks.cable.CableBlock;
import techreborn.init.TRBlockEntities;
import techreborn.init.TRContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Port note: Fabric Energy (long, con transazioni) diventa IEnergyStorage (int) e le BlockApiCache
 * diventano BlockCapabilityCache. L'energia interna resta un long ({@link #amount}); l'int compare solo
 * sulla porta verso l'esterno. Il "render attachment" di Fabric (il blocco di rivestimento letto dal
 * modello) non esiste: il rivestimento lo disegna il renderer del block entity, che chiama getCover().
 */
public class CableBlockEntity extends BlockEntity
	implements BlockEntityTicker<CableBlockEntity>, IListInfoProvider, IToolDrop {

	/** Energia contenuta in questo cavo. Il tipo non c'e' ancora quando il BE viene costruito, quindi niente contenitore a capacita' fissa. */
	long amount = 0;

	/** Una vista dell'energia per ognuno dei sei lati piu' quella "senza lato" (indice 6). */
	private final SideStorage[] sideStorages = new SideStorage[7];

	private TRContent.Cables cableType = null;
	@Nullable
	private BlockState cover = null;
	long lastTick = 0;
	// null means that it needs to be re-queried
	List<CableTarget> targets = null;
	/**
	 * Adjacent caches, used to quickly query the energy of adjacent blocks.
	 */
	@SuppressWarnings("unchecked")
	private final BlockCapabilityCache<IEnergyStorage, Direction>[] adjacentCaches = new BlockCapabilityCache[6];
	/**
	 * Bitmask to prevent input or output into/from the cable when the cable already transferred in the target direction.
	 * This prevents double transfer rates, and back and forth between two cables.
	 */
	int blockedSides = 0;

	/**
	 * This is only used during the cable tick, whereas {@link #blockedSides} is used between ticks.
	 */
	boolean ioBlocked = false;

	public CableBlockEntity(BlockPos pos, BlockState state) {
		super(TRBlockEntities.CABLE, pos, state);
	}

	public CableBlockEntity(BlockPos pos, BlockState state, TRContent.Cables type) {
		super(TRBlockEntities.CABLE, pos, state);
		this.cableType = type;
	}

	TRContent.Cables getCableType() {
		if (cableType != null) {
			return cableType;
		}
		if (level == null) {
			return TRContent.Cables.COPPER;
		}
		Block block = level.getBlockState(worldPosition).getBlock();
		if (block instanceof CableBlock) {
			return ((CableBlock) block).type;
		}
		//Something has gone wrong if this happens
		return TRContent.Cables.COPPER;
	}

	long getCapacity() {
		return getCableType().transferRate * 4L;
	}

	private boolean allowTransfer(@Nullable Direction side) {
		if (side == null) {
			return true;
		}

		return !ioBlocked && (blockedSides & (1 << side.ordinal())) == 0;
	}

	/** Quello che il cavo mostra al mondo su un dato lato (registrato come capability in TechReborn). */
	public IEnergyStorage getSideEnergyStorage(@Nullable Direction side) {
		int index = side == null ? 6 : side.ordinal();
		if (sideStorages[index] == null) {
			sideStorages[index] = new SideStorage(side);
		}
		return sideStorages[index];
	}

	public @Nullable BlockState getCover() {
		return cover;
	}

	public void setCover(@Nullable BlockState cover) {
		this.cover = cover;
		if (level != null && !level.isClientSide) {
			NetworkManager.sendToTracking(new CustomDescriptionPayload(getBlockPos(), this.saveWithoutMetadata(level.registryAccess())), this);
		}
	}

	public long getEnergy() {
		return amount;
	}

	public void setEnergy(long energy) {
		amount = energy;
	}

	private BlockCapabilityCache<IEnergyStorage, Direction> getAdjacentCache(Direction direction) {
		if (adjacentCaches[direction.get3DDataValue()] == null) {
			adjacentCaches[direction.get3DDataValue()] = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, (ServerLevel) level, worldPosition.relative(direction), direction.getOpposite());
		}
		return adjacentCaches[direction.get3DDataValue()];
	}

	@Nullable
	BlockEntity getAdjacentBlockEntity(Direction direction) {
		// Non si carica (ne' si genera) un chunk vicino solo per guardarci dentro.
		if (!(level instanceof ServerLevel serverLevel) || !serverLevel.isLoaded(worldPosition.relative(direction))) {
			return null;
		}
		return level.getBlockEntity(worldPosition.relative(direction));
	}

	void appendTargets(List<OfferedEnergyStorage> targetStorages) {
		ServerLevel serverWorld = (ServerLevel) level;
		if (serverWorld == null) {
			return;
		}

		// Update our targets if necessary.
		if (targets == null) {
			BlockState newBlockState = getBlockState();

			targets = new ArrayList<>();
			for (Direction direction : Direction.values()) {
				boolean foundSomething = false;

				if (serverWorld.isLoaded(worldPosition.relative(direction))) {
					BlockCapabilityCache<IEnergyStorage, Direction> adjCache = getAdjacentCache(direction);

					if (getAdjacentBlockEntity(direction) instanceof CableBlockEntity adjCable) {
						if (adjCable.getCableType().transferRate == getCableType().transferRate) {
							// Make sure cables are not used as regular targets.
							foundSomething = true;
						}
					} else if (adjCache.getCapability() != null) {
						foundSomething = true;
						targets.add(new CableTarget(direction, adjCache));
					}
				}

				newBlockState = newBlockState.setValue(CableBlock.PROPERTY_MAP.get(direction), foundSomething);
			}

			serverWorld.setBlockAndUpdate(getBlockPos(), newBlockState);
		}

		// Fill the list.
		for (CableTarget target : targets) {
			IEnergyStorage storage = target.find();

			if (storage == null) {
				// Schedule a rebuild next tick.
				// This is just a reference change, the iterator remains valid.
				targets = null;
			} else {
				targetStorages.add(new OfferedEnergyStorage(this, target.directionTo, storage));
			}
		}

		// Reset blocked sides.
		blockedSides = 0;
	}

	// BlockEntity
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
		return saveWithoutMetadata(registryLookup);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registryLookup) {
		super.loadAdditional(compound, registryLookup);
		if (compound.contains("energy")) {
			amount = compound.getLong("energy");
		}
		if (compound.contains("cover")) {
			cover = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), compound.getCompound("cover"));
		} else {
			cover = null;
		}
	}

	@Override
	protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registryLookup) {
		super.saveAdditional(compound, registryLookup);
		compound.putLong("energy", amount);
		if (cover != null) {
			compound.put("cover", NbtUtils.writeBlockState(cover));
		}
	}

	public void neighborUpdate() {
		targets = null;
	}

	// BlockEntityTicker
	@Override
	public void tick(Level world, BlockPos pos, BlockState state, CableBlockEntity blockEntity2) {
		if (world == null || world.isClientSide) {
			return;
		}

		CableTickManager.handleCableTick(this);
	}

	// IListInfoProvider
	@Override
	public void addInfo(List<Component> info, boolean isReal, boolean hasData) {
		info.add(
			Component.translatable("techreborn.tooltip.transferRate")
				.withStyle(ChatFormatting.GRAY)
				.append(": ")
				.append(PowerSystem.getLocalizedPower(getCableType().transferRate))
				.withStyle(ChatFormatting.GOLD)
				.append("/t")
		);

		info.add(
			Component.translatable("techreborn.tooltip.tier")
				.withStyle(ChatFormatting.GRAY)
				.append(": ")
				.append(
					Component.literal(StringUtils.toFirstCapitalAllLowercase(getCableType().tier.toString()))
						.withStyle(ChatFormatting.GOLD)
				)
		);

		if (!getCableType().canKill) {
			info.add(Component.translatable("techreborn.tooltip.cable.can_cover").withStyle(ChatFormatting.GRAY));
		}
	}

	// IToolDrop
	@Override
	public ItemStack getToolDrop(Player playerIn) {
		return new ItemStack(getCableType().block);
	}

	private record CableTarget(Direction directionTo, BlockCapabilityCache<IEnergyStorage, Direction> cache) {

		@Nullable
		IEnergyStorage find() {
			return cache.getCapability();
		}
	}

	/**
	 * Equivalente del SimpleSidedEnergyContainer di Fabric: stesso contenitore, ma ogni lato ha i
	 * suoi limiti (zero se il lato e' bloccato). Dentro il cavo l'energia e' un long, qui si
	 * limita a un int.
	 */
	private class SideStorage implements IEnergyStorage {
		private final @Nullable Direction side;

		SideStorage(@Nullable Direction side) {
			this.side = side;
		}

		private long limit() {
			return allowTransfer(side) ? getCableType().transferRate : 0;
		}

		@Override
		public int receiveEnergy(int toReceive, boolean simulate) {
			long accepted = Math.min(Math.min(toReceive, limit()), getCapacity() - amount);
			if (accepted <= 0) {
				return 0;
			}
			if (!simulate) {
				amount += accepted;
				setChanged();
			}
			return (int) accepted;
		}

		@Override
		public int extractEnergy(int toExtract, boolean simulate) {
			long given = Math.min(Math.min(toExtract, limit()), amount);
			if (given <= 0) {
				return 0;
			}
			if (!simulate) {
				amount -= given;
				setChanged();
			}
			return (int) given;
		}

		@Override
		public int getEnergyStored() {
			return (int) Math.min(amount, Integer.MAX_VALUE);
		}

		@Override
		public int getMaxEnergyStored() {
			return (int) Math.min(getCapacity(), Integer.MAX_VALUE);
		}

		@Override
		public boolean canExtract() {
			return limit() > 0;
		}

		@Override
		public boolean canReceive() {
			return limit() > 0;
		}
	}
}
