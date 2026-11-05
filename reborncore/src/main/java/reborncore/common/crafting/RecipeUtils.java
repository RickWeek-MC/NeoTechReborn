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

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class RecipeUtils {
	/**
	 * Recipe lists per recipe manager and type. Machines ask for these every second (and on every
	 * item insertion check), so rebuilding the list each time was the heaviest part of an idle tick.
	 * <p>
	 * Keyed weakly by the manager instance: /reload builds a new manager on the server, so the old
	 * entry is simply never hit again. The client manager is the same instance when recipes are
	 * re-synced, and the server can re-send them without a new manager, so {@link #clearCache()} is
	 * also called from the datapack sync and recipes updated events.
	 * <p>
	 * Only the recipe values are cached. Recipe ids (see {@link #getRecipeEntries}) are still read
	 * straight from the manager every time.
	 */
	private static final Map<net.minecraft.world.item.crafting.RecipeManager, Map<RecipeType<?>, List<? extends RebornRecipe>>> RECIPE_CACHE =
		Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * Drops every cached recipe list. Call whenever recipes may have changed.
	 */
	public static void clearCache() {
		RECIPE_CACHE.clear();
	}

	@SuppressWarnings("unchecked")
	public static <T extends RebornRecipe> List<T> getRecipes(Level world, RecipeType<T> type) {
		Map<RecipeType<?>, List<? extends RebornRecipe>> perType = RECIPE_CACHE.computeIfAbsent(world.getRecipeManager(), manager -> new ConcurrentHashMap<>());
		return (List<T>) perType.computeIfAbsent(type, key -> streamRecipeEntries(world, type).map(RecipeHolder::value).toList());
	}

	public static <T extends RebornRecipe> List<RecipeHolder<T>> getRecipeEntries(Level world, RecipeType<T> type) {
		return streamRecipeEntries(world, type).toList();
	}

	private static <T extends RebornRecipe> Stream<RecipeHolder<T>> streamRecipeEntries(Level world, RecipeType<T> type) {
		// Port note: upstream needed an access widener for getAllOfType; the Mojang-mapped
		// equivalent getAllRecipesFor is already public.
		return world.getRecipeManager().getAllRecipesFor(type).stream();
	}

	/**
	 * Adds the following toast/recipe defaults to an advancement builder:
	 * <ul>
	 *     <li>parent as "recipes/root"</li>
	 *     <li>criterion "has_the_recipe" via OR</li>
	 *     <li>reward: the specified recipe</li>
	 * </ul>
	 * @param builder the advancement task builder to expand
	 * @param recipeId the ID of the recipe
	 * @throws NullPointerException If any parameter refers to <code>null</code>.
	 */
	public static void addToastDefaults(@NotNull Advancement.Builder builder, @NotNull ResourceLocation recipeId) {
		Objects.requireNonNull(builder);
		Objects.requireNonNull(recipeId);
		builder
			.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
			.rewards(AdvancementRewards.Builder.recipe(recipeId))
			.requirements(AdvancementRequirements.Strategy.OR);
	}

}
