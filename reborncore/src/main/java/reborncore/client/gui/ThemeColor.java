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

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * A colour in packed ARGB form, as used by GuiGraphics.
 * <p>
 * Port note: upstream used a Yarn-only class for this. In theme.json a colour is written as
 * {@code #RRGGBB} (opaque) or {@code #AARRGGBB}.
 */
public record ThemeColor(int argb) {
	public static final ThemeColor TRANSPARENT = new ThemeColor(0);

	public static final Codec<ThemeColor> CODEC = Codec.STRING.comapFlatMap(ThemeColor::parse, ThemeColor::toString);

	private static DataResult<ThemeColor> parse(String text) {
		if (!text.startsWith("#") || (text.length() != 7 && text.length() != 9)) {
			return DataResult.error(() -> "Colour must look like #RRGGBB or #AARRGGBB, got: " + text);
		}
		try {
			int value = (int) Long.parseLong(text.substring(1), 16);
			if (text.length() == 7) {
				value |= 0xFF000000;
			}
			return DataResult.success(new ThemeColor(value));
		} catch (NumberFormatException e) {
			return DataResult.error(() -> "Not a hex colour: " + text);
		}
	}

	@Override
	public String toString() {
		return String.format("#%08X", argb);
	}
}
