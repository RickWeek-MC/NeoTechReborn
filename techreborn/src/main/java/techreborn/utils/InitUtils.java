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

package techreborn.utils;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import reborncore.RebornRegistry;
import techreborn.TechReborn;

public class InitUtils {
	public static <I extends Item> I setup(I item, String name) {
		RebornRegistry.registerIdent(item, ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, name));
		return item;
	}

	public static <B extends Block> B setup(B block, String name) {
		RebornRegistry.registerIdent(block, ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, name));
		return block;
	}

	public static boolean isDatagenRunning() {
		// Datagen is not ported, so this is always false. Kept so existing call sites keep working.
		return false;
	}

	private InitUtils() {/* No instantiation. */}
}
