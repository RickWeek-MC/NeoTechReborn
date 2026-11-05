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
package reborncore.common.blockentity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reborncore.common.util.NBTSerializable;

import java.util.*;

/**
 * Port note: the neighbour lookup used to be FluidStorage.SIDED.find(...) and the move
 * StorageUtil.move(...). On NeoForge those become a block capability lookup and
 * FluidUtil.tryFluidTransfer, which works in millibuckets -- hence the conversion on the amount.
 */
public class FluidConfiguration implements NBTSerializable {
	private static final StreamCodec<ByteBuf, Map<Direction, FluidConfig>> SIDE_MAP_PACKET_CODEC = ByteBufCodecs.map(
		HashMap::new,
		Direction.STREAM_CODEC,
		FluidConfig.PACKET_CODEC
	);
	public static final StreamCodec<ByteBuf, FluidConfiguration> PACKET_CODEC = StreamCodec.composite(
		SIDE_MAP_PACKET_CODEC, FluidConfiguration::getSideMap,
		ByteBufCodecs.BOOL, FluidConfiguration::autoInput,
		ByteBufCodecs.BOOL, FluidConfiguration::autoOutput,
		FluidConfiguration::new
	);

	Map<Direction, FluidConfig> sideMap;
	boolean input, output;

	public FluidConfiguration() {
		sideMap = new HashMap<>();
		Arrays.stream(Direction.values()).forEach(facing -> sideMap.put(facing, new FluidConfig(facing)));
	}

	public FluidConfiguration(CompoundTag tagCompound) {
		sideMap = new HashMap<>();
		read(tagCompound);
	}

	private FluidConfiguration(Map<Direction, FluidConfig> sideMap, boolean input, boolean output) {
		this.sideMap = sideMap;
		this.input = input;
		this.output = output;
	}

	public FluidConfig getSideDetail(Direction side) {
		if (side == null) {
			return sideMap.get(Direction.NORTH);
		}
		return sideMap.get(side);
	}

	public List<FluidConfig> getAllSides() {
		return new ArrayList<>(sideMap.values());
	}

	public Map<Direction, FluidConfig> getSideMap() {
		return sideMap;
	}

	public void updateFluidConfig(FluidConfig config) {
		FluidConfig toEdit = sideMap.get(config.side);
		toEdit.ioConfig = config.ioConfig;
	}

	public void update(MachineBaseBlockEntity machineBase) {
		if (!input && !output) {
			return;
		}
		if (machineBase.getTank() == null || machineBase.getLevel().getGameTime() % machineBase.slotTransferSpeed() != 0) {
			return;
		}

		final int transferAmount = machineBase.fluidTransferAmount().getMillibuckets();

		for (Direction facing : Direction.values()) {
			FluidConfig fluidConfig = getSideDetail(facing);
			if (fluidConfig == null || !fluidConfig.getIoConfig().isEnabled()) {
				continue;
			}

			@Nullable
			IFluidHandler neighbour = getTank(machineBase, facing);
			if (neighbour == null) {
				continue;
			}

			if (autoInput() && fluidConfig.getIoConfig().isInsert()) {
				FluidUtil.tryFluidTransfer(machineBase.getTank(), neighbour, transferAmount, true);
			}
			if (autoOutput() && fluidConfig.getIoConfig().isExtract()) {
				FluidUtil.tryFluidTransfer(neighbour, machineBase.getTank(), transferAmount, true);
			}
		}
	}

	@Nullable
	private IFluidHandler getTank(MachineBaseBlockEntity machine, Direction facing) {
		BlockPos pos = machine.getBlockPos().relative(facing);
		return machine.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, facing.getOpposite());
	}

	public boolean autoInput() {
		return input;
	}

	public boolean autoOutput() {
		return output;
	}

	public void setInput(boolean input) {
		this.input = input;
	}

	public void setOutput(boolean output) {
		this.output = output;
	}

	@NotNull
	@Override
	public CompoundTag write() {
		CompoundTag compound = new CompoundTag();
		Arrays.stream(Direction.values()).forEach(facing -> compound.put("side_" + facing.ordinal(), sideMap.get(facing).write()));
		compound.putBoolean("input", input);
		compound.putBoolean("output", output);
		return compound;
	}

	@Override
	public void read(@NotNull CompoundTag nbt) {
		sideMap.clear();
		Arrays.stream(Direction.values()).forEach(facing -> {
			CompoundTag compound = nbt.getCompound("side_" + facing.ordinal());
			FluidConfig config = new FluidConfig(compound);
			sideMap.put(facing, config);
		});
		input = nbt.getBoolean("input");
		output = nbt.getBoolean("output");
	}

	public static class FluidConfig implements NBTSerializable {
		public static final StreamCodec<ByteBuf, FluidConfig> PACKET_CODEC = StreamCodec.composite(
			Direction.STREAM_CODEC, FluidConfig::getSide,
			ExtractConfig.PACKET_CODEC, FluidConfig::getIoConfig,
			FluidConfig::new
		);

		Direction side;
		FluidConfiguration.ExtractConfig ioConfig;

		public FluidConfig(Direction side) {
			this.side = side;
			this.ioConfig = ExtractConfig.ALL;
		}

		public FluidConfig(Direction side, FluidConfiguration.ExtractConfig ioConfig) {
			this.side = side;
			this.ioConfig = ioConfig;
		}

		public FluidConfig(CompoundTag tagCompound) {
			read(tagCompound);
		}

		public Direction getSide() {
			return side;
		}

		public ExtractConfig getIoConfig() {
			return ioConfig;
		}

		@NotNull
		@Override
		public CompoundTag write() {
			CompoundTag tagCompound = new CompoundTag();
			tagCompound.putInt("side", side.ordinal());
			tagCompound.putInt("config", ioConfig.ordinal());
			return tagCompound;
		}

		@Override
		public void read(@NotNull CompoundTag nbt) {
			side = Direction.values()[nbt.getInt("side")];
			ioConfig = FluidConfiguration.ExtractConfig.values()[nbt.getInt("config")];
		}
	}

	public enum ExtractConfig {
		NONE(false, false),
		INPUT(false, true),
		OUTPUT(true, false),
		ALL(true, true);

		public static final StreamCodec<ByteBuf, ExtractConfig> PACKET_CODEC = ByteBufCodecs.INT
			.map(integer -> ExtractConfig.values()[integer], Enum::ordinal);

		boolean extract;
		boolean insert;

		ExtractConfig(boolean extract, boolean insert) {
			this.extract = extract;
			this.insert = insert;
		}

		public boolean isExtract() {
			return extract;
		}

		public boolean isInsert() {
			return insert;
		}

		public boolean isEnabled() {
			return extract || insert;
		}

		public FluidConfiguration.ExtractConfig getNext() {
			int i = this.ordinal() + 1;
			if (i >= FluidConfiguration.ExtractConfig.values().length) {
				i = 0;
			}
			return FluidConfiguration.ExtractConfig.values()[i];
		}
	}
}
