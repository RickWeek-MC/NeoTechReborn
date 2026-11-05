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

package reborncore.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.util.Objects;
import java.util.Optional;

/**
 * Loads assets/reborncore/theme.json on every client resource reload, so resource packs
 * (like the bundled dark mode one) can restyle the GUIs.
 * <p>
 * Port note: SimplePreparableReloadListener reads off-thread in prepare() and swaps the theme
 * on the main thread in apply(), which is what the upstream async future pair did by hand.
 */
public class ThemeManager extends SimplePreparableReloadListener<Theme> {
	private static final ResourceLocation THEME_FILE = ResourceLocation.fromNamespaceAndPath("reborncore", "theme.json");

	private static Theme theme = null;

	@Override
	protected Theme prepare(ResourceManager manager, ProfilerFiller profiler) {
		Optional<Resource> resource = manager.getResource(THEME_FILE);

		if (resource.isEmpty()) {
			throw new IllegalStateException("Failed to find reborn core theme.json");
		}

		try (BufferedReader reader = resource.get().openAsReader()) {
			JsonElement element = JsonParser.parseReader(reader);
			return Theme.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow(JsonParseException::new);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to parse reborn core theme.json", e);
		}
	}

	@Override
	protected void apply(Theme loaded, ResourceManager manager, ProfilerFiller profiler) {
		theme = loaded;
	}

	public static Theme getTheme() {
		return Objects.requireNonNull(theme, "Theme not loaded");
	}
}
