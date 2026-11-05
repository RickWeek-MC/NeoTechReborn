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

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import reborncore.RebornCore;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Developer tool: renders queued item stacks off-screen and writes them out as PNG files under
 * run/item_renderer/. Driven by the /reborncore render commands, which fill the queue through
 * QueueItemStacksPayload.
 * <p>
 * Initially taken from https://github.com/JamiesWhiteShirt/developer-mode/tree/experimental-item-render
 * and then ported to 1.15. Thanks 2xsaiko for fixing the lighting + odd issues above.
 * <p>
 * Port notes:
 * <ul>
 * <li>HudRenderCallback becomes RenderGuiEvent.Post; the DrawContext is the event's GuiGraphics.</li>
 * <li>SimpleFramebuffer is TextureTarget, beginWrite/endWrite are bindWrite/unbindWrite,
 *     loadFromTextureImage is downloadTexture, mirrorVertically is flipY, writeTo is writeToFile.</li>
 * <li>Two upstream bugs fixed. It built a local MatrixStack and then called applyModelViewMatrix,
 *     which applies RenderSystem's own stack -- so the translate never took effect; the model view
 *     is now pushed on RenderSystem's stack. And it replaced the projection matrix without putting
 *     the old one back, leaving the rest of the HUD rendering with an orthographic 16x16 projection;
 *     backupProjectionMatrix/restoreProjectionMatrix now bracket the work.</li>
 * </ul>
 */
public class ItemStackRenderer {

	private static final int SIZE = 512;

	public static void onRenderGui(RenderGuiEvent.Post event) {
		if (ItemStackRenderManager.RENDER_QUEUE.isEmpty()) {
			return;
		}

		final Minecraft minecraft = Minecraft.getInstance();
		final GuiGraphics guiGraphics = event.getGuiGraphics();

		ItemStack itemStack = ItemStackRenderManager.RENDER_QUEUE.remove();
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
		guiGraphics.drawString(minecraft.font, "Rendering " + id, 5, 5, -1, false);
		guiGraphics.drawString(minecraft.font, ItemStackRenderManager.RENDER_QUEUE.size() + " items left", 5, 15, -1, false);
		export(id, itemStack);
	}

	private static void export(ResourceLocation identifier, ItemStack item) {
		final Minecraft minecraft = Minecraft.getInstance();

		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 16, 16, 0, 1000, 3000), VertexSorting.ORTHOGRAPHIC_Z);

		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.identity();
		modelView.translate(0, 0, -2000);
		RenderSystem.applyModelViewMatrix();

		Lighting.setupFor3DItems();

		RenderTarget framebuffer = new TextureTarget(SIZE, SIZE, true, Minecraft.ON_OSX);

		try (NativeImage nativeImage = new NativeImage(SIZE, SIZE, true)) {
			framebuffer.setClearColor(0, 0, 0, 0);
			framebuffer.clear(Minecraft.ON_OSX);

			framebuffer.bindWrite(true);
			GuiGraphics offscreen = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
			offscreen.renderItem(item, 0, 0);
			offscreen.flush();
			framebuffer.unbindWrite();

			framebuffer.bindRead();
			nativeImage.downloadTexture(0, false);
			nativeImage.flipY();
			framebuffer.unbindRead();

			Path path = FMLPaths.GAMEDIR.get()
				.resolve("item_renderer")
				.resolve(identifier.getNamespace())
				.resolve(identifier.getPath() + ".png");
			Files.createDirectories(path.getParent());
			nativeImage.writeToFile(path);
		} catch (Exception e) {
			RebornCore.LOGGER.error("Failed to export item render for {}", identifier, e);
		} finally {
			framebuffer.destroyBuffers();

			modelView.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.restoreProjectionMatrix();

			// Back to the lighting the HUD expects for the rest of the frame.
			Lighting.setupForFlatItems();
		}
	}
}
