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

package reborncore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import reborncore.common.misc.MultiBlockBreakingTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Extends the block outline to every block a multi-block breaking tool would break.
 * <p>
 * Port note: Fabric's WorldRenderEvents.BLOCK_OUTLINE becomes NeoForge's RenderHighlightEvent.Block,
 * which carries the same things under different names -- pose stack, buffer source, camera and the
 * hit result instead of a block position. Returning true from the Fabric callback meant "keep
 * drawing the vanilla outline", which here is simply not cancelling the event.
 */
public class BlockOutlineRenderer {

	public static void onBlockOutline(RenderHighlightEvent.Block event) {
		final Minecraft minecraft = Minecraft.getInstance();
		final LocalPlayer player = minecraft.player;
		final Level level = minecraft.level;
		if (player == null || level == null) {
			return;
		}

		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty() || !(stack.getItem() instanceof MultiBlockBreakingTool tool)) {
			return;
		}

		BlockPos targetPos = event.getTarget().getBlockPos();
		Set<BlockPos> blockPosList = tool.getBlocksToBreak(stack, level, targetPos, player);

		List<VoxelShape> shapes = new ArrayList<>();
		for (BlockPos pos : blockPosList) {
			if (pos.equals(targetPos)) {
				continue;
			}

			BlockState blockState = level.getBlockState(pos);
			shapes.add(blockState.getShape(level, pos, CollisionContext.of(player))
				.move(pos.getX() - targetPos.getX(), pos.getY() - targetPos.getY(), pos.getZ() - targetPos.getZ()));
		}

		if (shapes.isEmpty()) {
			return;
		}

		VoxelShape shape = level.getBlockState(targetPos).getShape(level, targetPos, CollisionContext.of(player));
		for (VoxelShape voxelShape : shapes) {
			shape = Shapes.or(shape, voxelShape);
		}

		PoseStack poseStack = event.getPoseStack();
		Vec3 camera = event.getCamera().getPosition();
		LevelRenderer.renderVoxelShape(
			poseStack,
			event.getMultiBufferSource().getBuffer(RenderType.lines()),
			shape,
			targetPos.getX() - camera.x,
			targetPos.getY() - camera.y,
			targetPos.getZ() - camera.z,
			0.0F, 0.0F, 0.0F, 0.4F,
			true
		);
	}
}
