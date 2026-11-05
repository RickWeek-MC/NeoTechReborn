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

package techreborn.client.render.entitys;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import techreborn.blockentity.cable.CableBlockEntity;
import techreborn.blocks.cable.CableBlock;

/**
 * Disegna il blocco di rivestimento di un cavo coperto (legno di default, o quello messo con l'attrezzo di pittura).
 */
public class CableCoverRenderer implements BlockEntityRenderer<CableBlockEntity> {

	public CableCoverRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	@Override
	public void render(CableBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!blockEntity.getBlockState().getValue(CableBlock.COVERED) || blockEntity.getLevel() == null) {
			return;
		}

		final BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
		final BlockState renderData = blockEntity.getCover();
		final BlockState coverState = renderData != null ? renderData : Blocks.OAK_PLANKS.defaultBlockState();
		final VertexConsumer consumer = bufferSource.getBuffer(ItemBlockRenderTypes.getChunkRenderType(coverState));
		blockRenderer.renderBatched(coverState, blockEntity.getBlockPos(), blockEntity.getLevel(), poseStack, consumer, true, RandomSource.create());
	}

}
