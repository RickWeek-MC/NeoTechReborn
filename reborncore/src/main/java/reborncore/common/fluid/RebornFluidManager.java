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

package reborncore.common.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * Port note: two changes from upstream.
 * <ol>
 *     <li>Registration is queued and flushed during RegisterEvent, like RebornRegistry, because
 *     NeoForge registries are frozen outside of it.</li>
 *     <li>NeoForge also wants a FluidType per fluid, which Fabric has no concept of. One is created
 *     here for every registered fluid and handed to it, so callers see no difference.</li>
 * </ol>
 * {@link #init(IEventBus)} must run from the mod constructor.
 */
public class RebornFluidManager {

	private static final HashMap<ResourceLocation, RebornFluid> fluids = new HashMap<>();

	private record Pending(ResourceLocation name, RebornFluid fluid, FluidType fluidType) { }

	private static final List<Pending> PENDING = new ArrayList<>();
	private static boolean flushed = false;

	public static void init(IEventBus modEventBus) {
		modEventBus.addListener(RebornFluidManager::onRegister);
	}

	private static void onRegister(RegisterEvent event) {
		event.register(NeoForgeRegistries.Keys.FLUID_TYPES, helper -> {
			for (Pending pending : PENDING) {
				helper.register(pending.name(), pending.fluidType());
			}
		});

		event.register(Registries.FLUID, helper -> {
			for (Pending pending : PENDING) {
				helper.register(pending.name(), pending.fluid());
			}
			flushed = true;
		});
	}

	public static void register(RebornFluid rebornFluid, ResourceLocation identifier) {
		if (flushed) {
			throw new IllegalStateException("RebornFluidManager is closed, fluids must be registered during mod construction");
		}

		fluids.put(identifier, rebornFluid);

		FluidType fluidType = new FluidType(FluidType.Properties.create());
		rebornFluid.setFluidType(() -> fluidType);

		PENDING.add(new Pending(identifier, rebornFluid, fluidType));
	}

	public static HashMap<ResourceLocation, RebornFluid> getFluids() {
		return fluids;
	}

	public static Stream<RebornFluid> getFluidStream() {
		return fluids.values().stream();
	}
}
