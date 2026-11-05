/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 TeamReborn
 *
 * NeoForge port (c) 2026 RickWeek, based on the upstream Fabric sources.
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

package reborncore.client.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import reborncore.common.blockentity.MachineBaseBlockEntity;

/**
 * Block entity renderer that draws a machine's multiblock preview when it is toggled on.
 * <p>
 * RebornCore only provides the class: the mod that owns the block entity types registers it, from a
 * listener on EntityRenderersEvent.RegisterRenderers. That is TechReborn's job.
 * <p>
 * Port note: BlockEntityRendererFactory.Context is BlockEntityRendererProvider.Context, and
 * rendersOutsideBoundingBox is shouldRenderOffScreen.
 */
public class MultiblockRenderer<T extends MachineBaseBlockEntity> implements BlockEntityRenderer<T> {

	public MultiblockRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	@Override
	public void render(T blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
		if (blockEntity.renderMultiblock) {
			blockEntity.writeMultiblock(
				new HologramRenderer(blockEntity.getLevel(), poseStack, bufferSource, 0.4F)
					.rotate(blockEntity.getFacing().getOpposite())
			);
		}
	}

	@Override
	public boolean shouldRenderOffScreen(T blockEntity) {
		return true;
	}
}
