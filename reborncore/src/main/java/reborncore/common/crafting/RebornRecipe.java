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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;
import reborncore.RebornCore;
import reborncore.api.recipe.IRecipeCrafterProvider;
import reborncore.common.util.DefaultedListCollector;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface RebornRecipe extends Recipe<RebornRecipeInput> {
	Function<RecipeType<RebornRecipe>, MapCodec<RebornRecipe>> CODEC = type -> RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.list(SizedIngredient.CODEC.codec()).fieldOf("ingredients").forGetter(RebornRecipe::ingredients),
		Codec.list(ItemStack.CODEC).fieldOf("outputs").forGetter(RebornRecipe::outputs),
		ExtraCodecs.POSITIVE_INT.fieldOf("power").forGetter(RebornRecipe::power),
		ExtraCodecs.POSITIVE_INT.fieldOf("time").forGetter(RebornRecipe::time)
	).apply(instance, (ingredients, outputs, power, time) -> new Default(type, ingredients, outputs, power, time)));
	Function<RecipeType<RebornRecipe>, StreamCodec<RegistryFriendlyByteBuf, RebornRecipe>> PACKET_CODEC = type -> StreamCodec.composite(
		SizedIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()), RebornRecipe::ingredients,
		ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), RebornRecipe::outputs,
		ByteBufCodecs.INT, RebornRecipe::power,
		ByteBufCodecs.INT, RebornRecipe::time,
		(ingredients, outputs, power, time) -> new Default(type, ingredients, outputs, power, time)
	);

	@ApiStatus.Internal
	record Default(RecipeType<?> type, List<SizedIngredient> ingredients, List<ItemStack> outputs, int power, int time) implements RebornRecipe {
	}

	RecipeType<?> type();
	List<SizedIngredient> ingredients();
	List<ItemStack> outputs();
	int power();
	int time();

	@Override
	default ItemStack getToastSymbol() {
		ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(type());
		Optional<Item> catalyst = BuiltInRegistries.ITEM.getOptional(typeId);

		if (catalyst.isPresent()) {
			return new ItemStack(catalyst.get());
		}

		RebornCore.LOGGER.warn("Missing toast icon for {}!", typeId);
		return Recipe.super.getToastSymbol();
	}

	@Override
	default RecipeSerializer<?> getSerializer() {
		return BuiltInRegistries.RECIPE_SERIALIZER.get(BuiltInRegistries.RECIPE_TYPE.getKey(getType()));
	}

	@Override
	default RecipeType<?> getType() {
		return type();
	}

	/**
	 * use the {@link SizedIngredient} version to ensure stack sizes are checked
	 */
	@Deprecated
	@Override
	default NonNullList<Ingredient> getIngredients() {
		return this.ingredients().stream().map(SizedIngredient::ingredient).collect(DefaultedListCollector.toList());
	}

	/**
	 * @param blockEntity {@link BlockEntity} The blockEntity that is doing the crafting
	 * @return {@code boolean} If true, the recipe will craft, if false it will not
	 */
	default boolean canCraft(BlockEntity blockEntity) {
		if (blockEntity instanceof IRecipeCrafterProvider) {
			return ((IRecipeCrafterProvider) blockEntity).canCraft(this);
		}
		return true;
	}

	/**
	 * @param blockEntity {@link BlockEntity} The blockEntity that is doing the crafting
	 * @return {@code boolean} Returns true if fluid was taken and should craft
	 */
	default boolean onCraft(BlockEntity blockEntity) {
		return true;
	}

	// Done as our recipes do not support these functions, hopefully nothing blindly calls them
	@Deprecated
	@Override
	default boolean matches(RebornRecipeInput inv, Level worldIn) {
		throw new UnsupportedOperationException();
	}


	@Override
	default ItemStack assemble(RebornRecipeInput inventory, HolderLookup.Provider lookup) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	default boolean canCraftInDimensions(int width, int height) {
		throw new UnsupportedOperationException();
	}


	/**
	 * Do not call directly, this is implemented only as a fallback.
	 * {@link RebornRecipe#outputs()} will return all the outputs
	 */
	@Deprecated
	@Override
	default ItemStack getResultItem(HolderLookup.Provider registriesLookup) {
		if (outputs().isEmpty()) {
			return ItemStack.EMPTY;
		}
		return outputs().get(0);
	}

	@Override
	default NonNullList<ItemStack> getRemainingItems(RebornRecipeInput input) {
		throw new UnsupportedOperationException();
	}

	// Done to try and stop the table from loading it
	@Override
	default boolean isSpecial() {
		return true;
	}
}
