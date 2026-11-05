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

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;

/**
 * Where an ore generates.
 * <p>
 * Port note: upstream held a Fabric {@code Predicate<BiomeSelectionContext>} per constant, from
 * {@code BiomeSelectors.foundInOverworld()} and friends, which asked the generator whether the biome
 * can really appear in that dimension. NeoForge has no such selector: biomes are filtered by a biome
 * TAG, in the biome modifier that places the feature. So each constant carries two tags, plus the
 * dimension key for anything that works per dimension.
 * <p>
 * Why two: the vanilla tag ({@code minecraft:is_overworld} and friends) is the obvious one, but a
 * modded biome does not always put itself in it, and then it would get no ores at all -- on Fabric it
 * got them. NeoForge ships the conventional tags ({@code c:is_overworld}...) exactly for this, so the
 * modifier accepts EITHER tag. With a pack of 400 mods this is the difference between "the ores are
 * there" and "the ores are there except in half the world".
 */
public enum TargetDimension {
	OVERWORLD(BiomeTags.IS_OVERWORLD, Tags.Biomes.IS_OVERWORLD, Level.OVERWORLD),
	NETHER(BiomeTags.IS_NETHER, Tags.Biomes.IS_NETHER, Level.NETHER),
	END(BiomeTags.IS_END, Tags.Biomes.IS_END, Level.END);

	public static final StreamCodec<ByteBuf, TargetDimension> STREAM_CODEC = ByteBufCodecs.INT
		.map(integer -> TargetDimension.values()[integer], Enum::ordinal);

	public final TagKey<Biome> biomeTag;
	public final TagKey<Biome> commonBiomeTag;
	public final ResourceKey<Level> dimension;

	TargetDimension(TagKey<Biome> biomeTag, TagKey<Biome> commonBiomeTag, ResourceKey<Level> dimension) {
		this.biomeTag = biomeTag;
		this.commonBiomeTag = commonBiomeTag;
		this.dimension = dimension;
	}

	/** True if this biome belongs to this dimension, by either tag. */
	public boolean matches(net.minecraft.core.Holder<Biome> biome) {
		return biome.is(biomeTag) || biome.is(commonBiomeTag);
	}
}
