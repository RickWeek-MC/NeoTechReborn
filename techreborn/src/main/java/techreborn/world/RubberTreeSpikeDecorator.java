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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import techreborn.init.TRContent;

import java.util.Comparator;

/**
 * The spike of leaves on top of a rubber tree.
 * <p>
 * Port notes: {@code getType} is {@code type}, {@code generate(Generator)} is
 * {@code place(Context)}, and inside the context {@code getLogPositions/getLevel/getRandom/replace}
 * are {@code logs/level/random/setBlock}. {@code testBlockState} is {@code isStateAtPosition} and
 * {@code BlockStateProvider.TYPE_CODEC} is just {@code CODEC} ({@code get} is {@code getState}).
 * <p>
 * The two codec field names ("spire_height", "provider") must stay exactly as they are: the
 * configured feature JSON copied from the official jar was written by the upstream datagen and
 * uses those names.
 */
public class RubberTreeSpikeDecorator extends TreeDecorator {
	public static final MapCodec<RubberTreeSpikeDecorator> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			Codec.INT.fieldOf("spire_height").forGetter(RubberTreeSpikeDecorator::getSpireHeight),
			BlockStateProvider.CODEC.fieldOf("provider").forGetter(RubberTreeSpikeDecorator::getProvider)
		).apply(instance, RubberTreeSpikeDecorator::new)
	);

	private final int spireHeight;
	private final BlockStateProvider provider;

	public RubberTreeSpikeDecorator(int spireHeight, BlockStateProvider spireBlockState) {
		this.spireHeight = spireHeight;
		this.provider = spireBlockState;
	}

	@Override
	protected TreeDecoratorType<?> type() {
		return WorldGenerator.RUBBER_TREE_SPIKE.get();
	}

	@Override
	public void place(Context generator) {
		generator.logs().stream()
			.max(Comparator.comparingInt(BlockPos::getY))
			.ifPresent(blockPos -> {
				for (int i = 0; i < spireHeight; i++) {
					BlockPos sPos = blockPos.above(i);
					if (!generator.level().isStateAtPosition(sPos, state -> state.isAir() || state.is(TRContent.RUBBER_LOG) || state.is(BlockTags.REPLACEABLE_BY_TREES))) {
						return;
					}
					generator.setBlock(sPos, provider.getState(generator.random(), sPos));
				}
			});
	}

	public int getSpireHeight() {
		return spireHeight;
	}

	public BlockStateProvider getProvider() {
		return provider;
	}
}
