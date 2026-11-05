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

package reborncore.common.blocks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import reborncore.api.ToolManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Port note: Fabric's UseBlockCallback is replaced by NeoForge's RightClickBlock event
 * on the game event bus.
 */
public class BlockWrenchEventHandler {

	public static List<Block> wrenchableBlocks = new ArrayList<>();


	public static void setup() {
		NeoForge.EVENT_BUS.addListener(BlockWrenchEventHandler::onRightClickBlock);
	}

	private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() == InteractionHand.OFF_HAND) {
			// Wrench should be in main hand
			return;
		}

		if (!ToolManager.INSTANCE.canHandleTool(event.getEntity().getItemInHand(InteractionHand.MAIN_HAND))) {
			return;
		}

		BlockState state = event.getLevel().getBlockState(event.getPos());
		if (!wrenchableBlocks.contains(state.getBlock())) {
			return;
		}

		state.useWithoutItem(event.getLevel(), event.getEntity(), event.getHitVec());
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
	}

}
