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
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techreborn.TechReborn;
import techreborn.init.TRContent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The keys of TechReborn's worldgen, plus the two things worldgen needs registered in code.
 * <p>
 * PORT NOTE, this is the big one. Upstream added its features to biomes from code, with the Fabric
 * Biome Modification API ({@code BiomeModifications.create(...).add(phase, BiomeSelectors.all(),
 * ...)}). NeoForge has no such API: features are added to biomes by DATA, through biome modifier
 * files under {@code data/techreborn/neoforge/biome_modifier/}. Writing plain
 * {@code neoforge:add_features} files would have thrown away the config switches
 * (enableOreGeneration, enableRubberTreeGeneration, enableOilLakeGeneration and the per-ore ones),
 * because a JSON cannot read the config. So the modifier is OUR OWN type,
 * {@link TRFeaturesBiomeModifier}: the three files say which features to add, and the code keeps
 * deciding, exactly as upstream did, which biome gets what and whether the config allows it.
 * <p>
 * The configured features and placed features themselves are pure data and are NOT built here:
 * they come from {@code data/techreborn/worldgen/}, copied from the official jar (the upstream
 * datagen wrote them; see "Risorse: il datagen non c'e'" in the notes).
 * <p>
 * Also: SaplingGenerator is TreeGrower, and both the tree decorator type and the biome modifier
 * codec go through DeferredRegister, so {@link #register(IEventBus)} must be called from the mod
 * constructor. {@code initWorldGen()} is gone: there is nothing left to do at startup.
 */
public class WorldGenerator {
	/**
	 * I minerali che si generano, con le loro chiavi.
	 * <p>
	 * PIGRO DI PROPOSITO: calcolarlo in un campo statico farebbe costruire i 27 blocchi dei minerali
	 * (TRContent.Ores) nel momento in cui questa classe viene inizializzata, cioe' dentro il
	 * costruttore della mod, dove i registri sono congelati. Upstream leggeva ORE_FEATURES solo da
	 * initWorldGen(), dopo la registrazione. Oggi passerebbe comunque, ma e' esattamente l'invariante
	 * del blocco 21 e non va violata.
	 */
	private static List<TROreFeatureConfig> oreFeatures;

	public static List<TROreFeatureConfig> oreFeatures() {
		if (oreFeatures == null) {
			oreFeatures = getOreFeatures();
		}
		return oreFeatures;
	}

	public static final ResourceLocation OIL_LAKE_ID = ResourceLocation.fromNamespaceAndPath("techreborn", "oil_lake");
	public static final ResourceKey<ConfiguredFeature<?, ?>> OIL_LAKE_FEATURE = ResourceKey.create(Registries.CONFIGURED_FEATURE, OIL_LAKE_ID);
	public static final ResourceKey<PlacedFeature> OIL_LAKE_PLACED_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, OIL_LAKE_ID);

	public static final ResourceLocation RUBBER_TREE_ID = ResourceLocation.fromNamespaceAndPath("techreborn", "rubber_tree");
	public static final ResourceKey<ConfiguredFeature<?, ?>> RUBBER_TREE_FEATURE = ResourceKey.create(Registries.CONFIGURED_FEATURE, RUBBER_TREE_ID);
	public static final ResourceKey<PlacedFeature> RUBBER_TREE_PLACED_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, RUBBER_TREE_ID);

	public static final ResourceLocation RUBBER_TREE_PATCH_ID = ResourceLocation.fromNamespaceAndPath("techreborn", "rubber_tree_patch");
	public static final ResourceKey<ConfiguredFeature<?, ?>> RUBBER_TREE_PATCH_FEATURE = ResourceKey.create(Registries.CONFIGURED_FEATURE, RUBBER_TREE_PATCH_ID);
	public static final ResourceKey<PlacedFeature> RUBBER_TREE_PATCH_PLACED_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, RUBBER_TREE_PATCH_ID);

	private static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS =
		DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, TechReborn.MOD_ID);

	public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<RubberTreeSpikeDecorator>> RUBBER_TREE_SPIKE =
		TREE_DECORATORS.register("rubber_tree_spike", () -> new TreeDecoratorType<>(RubberTreeSpikeDecorator.CODEC));

	/**
	 * What the sapling grows into. The tree itself is the configured feature from the data pack, so
	 * this only needs its key.
	 */
	public static final TreeGrower RUBBER_TREE_GROWER = new TreeGrower(
		RUBBER_TREE_ID.toString(),
		Optional.empty(),
		Optional.of(RUBBER_TREE_FEATURE),
		Optional.empty()
	);

	public static void register(IEventBus modEventBus) {
		TREE_DECORATORS.register(modEventBus);
		TRFeaturesBiomeModifier.register(modEventBus);
	}

	private static List<TROreFeatureConfig> getOreFeatures() {
		return Arrays.stream(TRContent.Ores.values())
				.filter(ores -> ores.distribution != null)
				.filter(ores -> ores.distribution.isGenerating())
				.map(TROreFeatureConfig::of)
				.toList();
	}

	/** The ore that owns this placed feature key, or null if the key is not one of ours. */
	public static TROreFeatureConfig oreFeatureOf(ResourceKey<PlacedFeature> key) {
		for (TROreFeatureConfig config : oreFeatures()) {
			if (config.placedFeature() == key || config.placedFeature().location().equals(key.location())) {
				return config;
			}
		}
		return null;
	}
}
