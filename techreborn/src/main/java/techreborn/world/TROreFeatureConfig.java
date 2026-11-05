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

package techreborn.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import techreborn.init.TRContent;

/**
 * One ore and the two worldgen keys that belong to it.
 * <p>
 * Port note: upstream's {@code biomeSelector()} returned a Fabric
 * {@code Predicate&lt;BiomeSelectionContext&gt;}. NeoForge filters biomes by tag instead, so this
 * just hands out the ore's TargetDimension (which carries the tags) and the filtering happens in
 * {@link TRFeaturesBiomeModifier}.
 */
public record TROreFeatureConfig(ResourceLocation id, TRContent.Ores ore, ResourceKey<ConfiguredFeature<?, ?>> configuredFeature, ResourceKey<PlacedFeature> placedFeature) {
	public static TROreFeatureConfig of(TRContent.Ores ore) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("techreborn", ore.name + "_ore");
		return new TROreFeatureConfig(
			id,
			ore,
			ResourceKey.create(Registries.CONFIGURED_FEATURE, id),
			ResourceKey.create(Registries.PLACED_FEATURE, id)
		);
	}

	public TargetDimension dimension() {
		return ore.distribution.dimension;
	}
}
