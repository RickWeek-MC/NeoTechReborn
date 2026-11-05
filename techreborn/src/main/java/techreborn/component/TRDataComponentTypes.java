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

package techreborn.component;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.mojang.serialization.Codec;
import techreborn.TechReborn;

/**
 * Port note: the component objects themselves are plain objects, built eagerly exactly as upstream
 * did, so every {@code stack.get(TRDataComponentTypes.X)} call site is untouched. Only the
 * registration moved: {@link #register(IEventBus)} queues them through a {@link DeferredRegister},
 * because the registry is frozen while the mod constructor runs.
 * <p>
 * RebornCore's content loaders run after the data component registry is built, so components are
 * available to the items that use them.
 */
public class TRDataComponentTypes {
	private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
		DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TechReborn.MOD_ID);

	private static final StreamCodec<RegistryFriendlyByteBuf, BlockState> BLOCK_STATE_PACKET_CODEC =
		StreamCodec.of(
			(buf, value) -> buf.writeNbt(NbtUtils.writeBlockState(value)),
			buf -> NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), buf.readNbt())
		);

	public static final DataComponentType<Boolean> IS_ACTIVE =
		DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();

	public static final DataComponentType<Boolean> AOE5 =
		DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();

	public static final DataComponentType<GlobalPos> FREQUENCY_TRANSMITTER =
		DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build();

	public static final DataComponentType<BlockState> PAINTING_COVER =
		DataComponentType.<BlockState>builder().persistent(BlockState.CODEC).networkSynchronized(BLOCK_STATE_PACKET_CODEC).build();

	public static final DataComponentType<Holder<Fluid>> FLUID =
		DataComponentType.<Holder<Fluid>>builder().persistent(BuiltInRegistries.FLUID.holderByNameCodec()).networkSynchronized(ByteBufCodecs.holderRegistry(Registries.FLUID)).build();

	public static void register(IEventBus modEventBus) {
		COMPONENTS.register("is_active", () -> IS_ACTIVE);
		COMPONENTS.register("aoe5", () -> AOE5);
		COMPONENTS.register("frequency_transmitter", () -> FREQUENCY_TRANSMITTER);
		COMPONENTS.register("painting_cover", () -> PAINTING_COVER);
		COMPONENTS.register("fluid", () -> FLUID);
		COMPONENTS.register(modEventBus);
	}
}
