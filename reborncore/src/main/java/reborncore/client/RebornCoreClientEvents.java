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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import reborncore.api.items.ArmorFovHandler;
import reborncore.common.crafting.RecipeUtils;

/**
 * Client counterpart of {@link reborncore.common.RebornCoreEvents}: every client hook RebornCore
 * needs, in one place.
 * <p>
 * Port note: this is where upstream's two client mixins went, in keeping with the zero-mixin rule
 * from block 13.
 * <ul>
 * <li>MixinDebugRenderer, which drew the chunk loader boxes, is now a RenderLevelStageEvent
 *     listener. The event carries no buffer source, so the one the level renderer uses is taken from
 *     Minecraft and flushed here -- a debug line strip is not part of any later batch.</li>
 * <li>MixinGameRenderer, which redirected getFovModifier to let armour change the FOV, is now
 *     ComputeFovModifierEvent. The event already hands over the vanilla value, so the armour
 *     handlers just adjust it in turn, exactly as the Redirect did.</li>
 * </ul>
 */
public final class RebornCoreClientEvents {

	private RebornCoreClientEvents() {
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(BlockOutlineRenderer::onBlockOutline);
		NeoForge.EVENT_BUS.addListener(StackToolTipHandler::onItemTooltip);
		NeoForge.EVENT_BUS.addListener(ItemStackRenderer::onRenderGui);
		NeoForge.EVENT_BUS.addListener(RebornCoreClientEvents::onClientTick);
		NeoForge.EVENT_BUS.addListener(RebornCoreClientEvents::onComputeFovModifier);
		NeoForge.EVENT_BUS.addListener(RebornCoreClientEvents::onRenderLevelStage);
		NeoForge.EVENT_BUS.addListener(RebornCoreClientEvents::onRecipesUpdated);
	}

	/**
	 * The client recipe manager is updated in place when the server syncs recipes (join, /reload),
	 * so the cached recipe lists have to be dropped here. See {@link RecipeUtils#clearCache()}.
	 */
	private static void onRecipesUpdated(RecipesUpdatedEvent event) {
		RecipeUtils.clearCache();
	}

	/**
	 * Fires {@link ClientJumpEvent} while the jump key is held, as upstream did from END_CLIENT_TICK.
	 */
	private static void onClientTick(ClientTickEvent.Post event) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.keyJump.isDown()) {
			NeoForge.EVENT_BUS.post(new ClientJumpEvent());
		}
	}

	/**
	 * Lets armour pieces implementing {@link ArmorFovHandler} change the field of view.
	 */
	private static void onComputeFovModifier(ComputeFovModifierEvent event) {
		float fov = event.getNewFovModifier();

		for (ItemStack stack : event.getPlayer().getArmorSlots()) {
			if (stack.getItem() instanceof ArmorFovHandler fovHandler) {
				fov = fovHandler.changeFov(fov, stack, event.getPlayer());
			}
		}

		event.setNewFovModifier(fov);
	}

	/**
	 * Draws the chunk loader debug boxes.
	 */
	private static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !ClientChunkManager.isShow()) {
			return;
		}

		final MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		final Vec3 camera = event.getCamera().getPosition();

		ClientChunkManager.render(event.getPoseStack(), bufferSource, camera.x, camera.y, camera.z);
		bufferSource.endBatch(RenderType.debugLineStrip(1.0));
	}
}
