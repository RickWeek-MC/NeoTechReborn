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

package reborncore;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import reborncore.client.RebornCoreClientEvents;
import reborncore.client.RebornFluidRenderManager;
import reborncore.client.gui.ThemeManager;

import java.util.Locale;

/**
 * Client-only entry point, the NeoForge counterpart of upstream's ClientModInitializer.
 * <p>
 * Everything the common entry point must not touch (screens, rendering, resource reloads) is
 * wired up from here, so a dedicated server never loads these classes.
 */
@Mod(value = RebornCore.MOD_ID, dist = Dist.CLIENT)
public class RebornCoreClient {

	public RebornCoreClient(IEventBus modEventBus) {
		modEventBus.addListener(RebornCoreClient::registerReloadListeners);
		modEventBus.addListener(RebornCoreClient::addPackFinders);
		modEventBus.addListener(RebornFluidRenderManager::registerClientExtensions);

		RebornCoreClientEvents.register();
	}

	private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(new ThemeManager());

		// Port note: upstream read the language once at CLIENT_STARTED, so switching language in game
		// had no effect until a restart. Changing language triggers a resource reload, so doing it
		// here keeps RebornCore.locale correct (number formatting etc.) at no extra cost.
		event.registerReloadListener((ResourceManagerReloadListener) manager -> {
			String code = Minecraft.getInstance().getLanguageManager().getSelected();
			int underscore = code.indexOf('_');
			RebornCore.locale = Locale.forLanguageTag(underscore > 0 ? code.substring(0, underscore) : code);
		});
	}

	/**
	 * Registers the bundled dark mode resource pack (resourcepacks/reborncore_darkmode in the jar).
	 * Like upstream's ResourcePackActivationType.NORMAL it is available in the pack list but not enabled by default.
	 */
	private static void addPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() != PackType.CLIENT_RESOURCES) {
			return;
		}

		event.addPackFinders(
			ResourceLocation.fromNamespaceAndPath(RebornCore.MOD_ID, "resourcepacks/reborncore_darkmode"),
			PackType.CLIENT_RESOURCES,
			Component.literal("Neo Reborn Core Dark Mode"),
			PackSource.BUILT_IN,
			false,
			Pack.Position.TOP
		);
	}
}
