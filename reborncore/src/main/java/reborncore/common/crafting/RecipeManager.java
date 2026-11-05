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

package reborncore.common.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Port note: upstream registered the recipe type and its serializer the moment newRecipeType was
 * called. NeoForge registries are frozen outside RegisterEvent, so, exactly like RebornRegistry,
 * the objects are created immediately (callers keep their reference) and queued; the actual
 * registration happens during the event. {@link #init(IEventBus)} must run from the mod constructor.
 */
public class RecipeManager {

	private record Pending(ResourceLocation name, RecipeType<?> type, RecipeSerializer<?> serializer) { }

	private static final List<Pending> PENDING = new ArrayList<>();
	private static boolean flushed = false;

	public static void init(IEventBus modEventBus) {
		modEventBus.addListener(RecipeManager::onRegister);
	}

	private static void onRegister(RegisterEvent event) {
		event.register(Registries.RECIPE_TYPE, helper -> {
			for (Pending pending : PENDING) {
				helper.register(pending.name(), pending.type());
			}
		});

		event.register(Registries.RECIPE_SERIALIZER, helper -> {
			for (Pending pending : PENDING) {
				helper.register(pending.name(), pending.serializer());
			}
			flushed = true;
		});
	}

	public static RecipeType<RebornRecipe> newRecipeType(ResourceLocation name) {
		return newRecipeType(name, RebornRecipe.CODEC, RebornRecipe.PACKET_CODEC);
	}

	public static <R extends RebornRecipe> RecipeType<R> newRecipeType(ResourceLocation name, Function<RecipeType<R>, MapCodec<R>> codec, Function<RecipeType<R>, StreamCodec<RegistryFriendlyByteBuf, R>> packetCodec) {
		if (flushed) {
			throw new IllegalStateException("RecipeManager is closed, recipe types must be created during mod construction");
		}

		RecipeType<R> type = new RecipeType<R>() {
			@Override
			public String toString() {
				return name.toString();
			}
		};

		RebornRecipeSerializer<R> serializer = new RebornRecipeSerializer<>(codec.apply(type), packetCodec.apply(type));
		PENDING.add(new Pending(name, type, serializer));

		return type;
	}

	public static List<RecipeType<?>> getRecipeTypes(String namespace) {
		return BuiltInRegistries.RECIPE_TYPE.registryKeySet().stream()
			.filter(key -> key.location().getNamespace().startsWith(namespace))
			.map((Function<ResourceKey<RecipeType<?>>, RecipeType<?>>) key -> BuiltInRegistries.RECIPE_TYPE.get(key.location()))
			.toList();
	}
}
