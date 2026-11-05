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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import techreborn.TechReborn;
import techreborn.config.TechRebornConfig;

/**
 * Adds TechReborn's features to biomes. This class has no counterpart upstream: it replaces the
 * Fabric Biome Modification API, which NeoForge does not have (see the note on WorldGenerator).
 * <p>
 * Three files under {@code data/techreborn/neoforge/biome_modifier/} point at this type, one per
 * kind of feature. Each one only lists the features to add; which biome gets them, and whether the
 * config allows them at all, is decided here, with the same rules upstream used:
 * <ul>
 *   <li>ores: every biome, each ore only in the dimension its OreDistribution names (through the
 *       biome tag of TargetDimension), and only if its own config switch is on;</li>
 *   <li>rubber trees: forests, taigas and the swamp;</li>
 *   <li>oil lakes: the overworld.</li>
 * </ul>
 */
public record TRFeaturesBiomeModifier(Kind kind, HolderSet<PlacedFeature> features) implements BiomeModifier {

	public enum Kind implements StringRepresentable {
		ORES("ores", GenerationStep.Decoration.UNDERGROUND_ORES),
		RUBBER_TREE("rubber_tree", GenerationStep.Decoration.VEGETAL_DECORATION),
		OIL_LAKE("oil_lake", GenerationStep.Decoration.LAKES);

		public static final StringRepresentable.EnumCodec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

		private final String name;
		private final GenerationStep.Decoration step;

		Kind(String name, GenerationStep.Decoration step) {
			this.name = name;
			this.step = step;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final MapCodec<TRFeaturesBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			Kind.CODEC.fieldOf("kind").forGetter(TRFeaturesBiomeModifier::kind),
			RegistryCodecs.homogeneousList(Registries.PLACED_FEATURE).fieldOf("features").forGetter(TRFeaturesBiomeModifier::features)
		).apply(instance, TRFeaturesBiomeModifier::new)
	);

	private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
		DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, TechReborn.MOD_ID);

	public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<TRFeaturesBiomeModifier>> TYPE =
		SERIALIZERS.register("features", () -> CODEC);

	public static void register(IEventBus modEventBus) {
		SERIALIZERS.register(modEventBus);
	}

	@Override
	public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		if (phase != Phase.ADD) {
			return;
		}

		switch (kind) {
			case ORES -> {
				if (!TechRebornConfig.enableOreGeneration) return;
				for (Holder<PlacedFeature> feature : features) {
					ResourceKey<PlacedFeature> key = feature.unwrapKey().orElse(null);
					if (key == null) continue;
					TROreFeatureConfig oreConfig = WorldGenerator.oreFeatureOf(key);
					// a feature we do not know, or an ore switched off in the config: skip it
					if (oreConfig == null) continue;
					if (oreConfig.dimension().matches(biome)) {
						builder.getGenerationSettings().addFeature(kind.step, feature);
					}
				}
			}
			case RUBBER_TREE -> {
				if (!TechRebornConfig.enableRubberTreeGeneration) return;
				if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(Biomes.SWAMP)) {
					addAll(builder);
				}
			}
			case OIL_LAKE -> {
				if (!TechRebornConfig.enableOilLakeGeneration) return;
				if (biome.is(BiomeTags.IS_OVERWORLD)) {
					addAll(builder);
				}
			}
		}
	}

	private void addAll(ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		for (Holder<PlacedFeature> feature : features) {
			builder.getGenerationSettings().addFeature(kind.step, feature);
		}
	}

	@Override
	public MapCodec<? extends BiomeModifier> codec() {
		return TYPE.get();
	}
}
