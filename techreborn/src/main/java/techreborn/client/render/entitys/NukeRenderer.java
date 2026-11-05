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
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import techreborn.entities.EntityNukePrimed;
import techreborn.init.TRContent;

/**
 * Created by Mark on 13/03/2016.
 * <p>
 * Port notes: MatrixStack is PoseStack (push/pop/multiply -> pushPose/popPose/mulPose),
 * VertexConsumerProvider is MultiBufferSource, BlockRenderManager is BlockRenderDispatcher,
 * EntityRendererFactory.Context is EntityRendererProvider.Context, RotationAxis is
 * com.mojang.math.Axis, {@code getTexture} is {@code getTextureLocation} (and it is abstract, so
 * it cannot return null) and TntMinecartEntityRenderer.renderFlashingBlock is
 * TntMinecartRenderer.renderWhiteSolidBlock.
 */
public class NukeRenderer extends EntityRenderer<EntityNukePrimed> {
	private final BlockRenderDispatcher blockRenderManager;

	public NukeRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.shadowRadius = 0.5F;
		this.blockRenderManager = ctx.getBlockRenderDispatcher();
	}

	@Override
	public ResourceLocation getTextureLocation(EntityNukePrimed entityNukePrimed) {
		return InventoryMenu.BLOCK_ATLAS;
	}

	@Override
	public void render(EntityNukePrimed entity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i) {
		matrixStack.pushPose();
		matrixStack.translate(1D, 0.5D, 0);
		if ((float) entity.getFuse() - g + 1.0F < 10.0F) {
			float h = 1.0F - ((float) entity.getFuse() - g + 1.0F) / 10.0F;
			h = Mth.clamp(h, 0.0F, 1.0F);
			h *= h;
			h *= h;
			float j = 1.0F + h * 0.3F;
			matrixStack.scale(j, j, j);
		}

		matrixStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
		matrixStack.translate(-0.5D, -0.5D, 0.5D);
		TntMinecartRenderer.renderWhiteSolidBlock(blockRenderManager, TRContent.NUKE.defaultBlockState(), matrixStack, vertexConsumerProvider, i, entity.getFuse() / 5 % 2 == 0);
		matrixStack.popPose();
		super.render(entity, f, g, matrixStack, vertexConsumerProvider, i);
	}
}
