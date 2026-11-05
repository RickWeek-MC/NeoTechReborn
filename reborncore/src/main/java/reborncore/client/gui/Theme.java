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
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Theme(
	ThemeColor titleColor,
	ThemeColor subtitleColor,
	ThemeColor warningTextColor,
	ThemeColor ioInputColor,
	ThemeColor ioOutputColor,
	ThemeColor ioBothColor
) {
	public static final Codec<Theme> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ThemeColor.CODEC.fieldOf("titleColor").forGetter(Theme::titleColor),
		ThemeColor.CODEC.fieldOf("subtitleColor").forGetter(Theme::subtitleColor),
		ThemeColor.CODEC.fieldOf("warningTextColor").forGetter(Theme::warningTextColor),
		ThemeColor.CODEC.fieldOf("ioInputColor").forGetter(Theme::ioInputColor),
		ThemeColor.CODEC.fieldOf("ioOutputColor").forGetter(Theme::ioOutputColor),
		ThemeColor.CODEC.fieldOf("ioBothColor").forGetter(Theme::ioBothColor)
	).apply(instance, Theme::new));
}
