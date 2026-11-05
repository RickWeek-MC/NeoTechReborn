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

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import reborncore.common.fluid.FluidSettings;
import reborncore.common.fluid.RebornFluid;
import reborncore.common.fluid.RebornFluidManager;

/**
 * Gives every RebornCore fluid its textures on the client.
 * <p>
 * Port note: this file shrank a lot. Fabric needed a FluidRenderHandler per fluid plus a cache of
 * resolved sprites and a resource reload listener to invalidate it. NeoForge asks only for the two
 * texture names through IClientFluidTypeExtensions and resolves and reloads the sprites itself, so
 * the sprite map, the lazy holders and the reload listener are all gone.
 * <p>
 * The same extensions feed both the world rendering and the tank drawing in GuiBuilder, which
 * already reads them via IClientFluidTypeExtensions.of(fluid). Without this registration every
 * RebornCore fluid falls back to the default texture.
 */
public class RebornFluidRenderManager {

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		RebornFluidManager.getFluidStream().forEach(fluid -> registerFluid(event, fluid));
	}

	private static void registerFluid(RegisterClientExtensionsEvent event, RebornFluid fluid) {
		final FluidSettings fluidSettings = fluid.getFluidSettings();
		final ResourceLocation still = fluidSettings.getStillTexture();
		final ResourceLocation flowing = fluidSettings.getFlowingTexture();

		event.registerFluidType(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return still;
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return flowing;
			}
		}, fluid.getFluidType());
	}
}
