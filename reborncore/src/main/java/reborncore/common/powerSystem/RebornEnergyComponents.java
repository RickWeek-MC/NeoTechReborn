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

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Port note: the Fabric energy API shipped its own energy data component
 * (EnergyStorage.ENERGY_COMPONENT). NeoForge has none, so RebornCore registers its own.
 */
public class RebornEnergyComponents {

	private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
		DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "reborncore");

	/** Stored energy of an item, in the same unit the rest of RebornCore uses. */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ENERGY =
		COMPONENTS.register("energy", () -> DataComponentType.<Long>builder()
			.persistent(Codec.LONG)
			.networkSynchronized(ByteBufCodecs.VAR_LONG)
			.build());

	public static void register(IEventBus modEventBus) {
		COMPONENTS.register(modEventBus);
	}

	private RebornEnergyComponents() {
	}
}
