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

package reborncore.common.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Port note: the serializer used to register itself in a static initialiser. On NeoForge it goes
 * through a DeferredRegister, so {@link #register(IEventBus)} must be called from the mod constructor.
 */
public class PaddedShapedRecipe extends ShapedRecipe {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reborncore", "padded");

	private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
		DeferredRegister.create(Registries.RECIPE_SERIALIZER, "reborncore");

	public static final DeferredHolder<RecipeSerializer<?>, Serializer> PADDED =
		SERIALIZERS.register("padded", Serializer::new);

	public static void register(IEventBus modEventBus) {
		SERIALIZERS.register(modEventBus);
	}

	final ShapedRecipePattern raw;
	final ItemStack result;

	public PaddedShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern raw, ItemStack result, boolean showNotification) {
		super(group, category, raw, result, showNotification);
		this.raw = raw;
		this.result = result;
	}

	public static ShapedRecipePattern create(Map<Character, Ingredient> key, List<String> pattern) {
		ShapedRecipePattern.Data data = new ShapedRecipePattern.Data(key, pattern);
		return fromData(data).getOrThrow();
	}

	// Basically a copy of ShapedRecipePattern.unpack
	private static DataResult<ShapedRecipePattern> fromData(ShapedRecipePattern.Data data) {
		String[] strings = data.pattern().toArray(String[]::new);
		int width = strings[0].length();
		int height = strings.length;

		NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
		CharSet charSet = new CharArraySet(data.key().keySet());

		for(int i = 0; i < strings.length; ++i) {
			String string = strings[i];

			for(int l = 0; l < string.length(); ++l) {
				char c = string.charAt(l);
				Ingredient ingredient = c == ' ' ? Ingredient.EMPTY : data.key().get(c);
				if (ingredient == null) {
					return DataResult.error(() -> "Pattern references symbol '" + c + "' but it's not defined in the key");
				}

				charSet.remove(c);
				ingredients.set(l + width * i, ingredient);
			}
		}

		if (!charSet.isEmpty()) {
			return DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charSet);
		}

		return DataResult.success(new ShapedRecipePattern(width, height, ingredients, Optional.of(data)));
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return PADDED.get();
	}

	public ShapedRecipePattern getRaw() {
		return raw;
	}

	public ItemStack getResult() {
		return result;
	}

	public static class Serializer implements RecipeSerializer<PaddedShapedRecipe> {

		public static final MapCodec<PaddedShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(Codec.STRING.optionalFieldOf("group", "").forGetter(PaddedShapedRecipe::getGroup),
					CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(PaddedShapedRecipe::category),
					ShapedRecipePattern.MAP_CODEC.forGetter(PaddedShapedRecipe::getRaw),
					ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PaddedShapedRecipe::getResult),
					Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(PaddedShapedRecipe::showNotification))
				.apply(instance, PaddedShapedRecipe::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, PaddedShapedRecipe> PACKET_CODEC =
			StreamCodec.of(PaddedShapedRecipe.Serializer::write, PaddedShapedRecipe.Serializer::read);

		@Override
		public MapCodec<PaddedShapedRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, PaddedShapedRecipe> streamCodec() {
			return PACKET_CODEC;
		}

		private static PaddedShapedRecipe read(RegistryFriendlyByteBuf buf) {
			String string = buf.readUtf();
			CraftingBookCategory craftingRecipeCategory = buf.readEnum(CraftingBookCategory.class);
			ShapedRecipePattern rawShapedRecipe = ShapedRecipePattern.STREAM_CODEC.decode(buf);
			ItemStack itemStack = ItemStack.STREAM_CODEC.decode(buf);
			boolean showNotification = buf.readBoolean();
			return new PaddedShapedRecipe(string, craftingRecipeCategory, rawShapedRecipe, itemStack, showNotification);
		}

		private static void write(RegistryFriendlyByteBuf buf, PaddedShapedRecipe recipe) {
			buf.writeUtf(recipe.getGroup());
			buf.writeEnum(recipe.category());
			ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.raw);
			ItemStack.STREAM_CODEC.encode(buf, recipe.result);
			buf.writeBoolean(recipe.showNotification());
		}
	}
}
