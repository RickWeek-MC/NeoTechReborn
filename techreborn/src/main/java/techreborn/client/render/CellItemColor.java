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

package techreborn.client.render;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import reborncore.common.fluid.container.ItemFluidInfo;
import techreborn.TechReborn;

/**
 * Colours the fluid inside the cell, the layer {@link DynamicCellModel} draws with tint index 1.
 * <p>
 * Port note: upstream read the colour from Fabric's FluidRenderHandler. The NeoForge counterpart is the
 * fluid's client extensions, the same ones RebornCore registers for this mod's fluids. Most fluids ask
 * for no tint (their texture is already coloured) and get white; water is the one that matters, and
 * NeoForge gives it its blue.
 * <p>
 * The fluid is read off the item, not through the fluid capability: fewer things in the way, and it is
 * what upstream did.
 */
public class CellItemColor implements ItemColor {

	private static final int WHITE = 0xFFFFFFFF;

	@Override
	public int getColor(ItemStack stack, int tintIndex) {
		if (tintIndex != DynamicCellModel.FLUID_TINT_INDEX) {
			return WHITE;
		}

		if (!(stack.getItem() instanceof ItemFluidInfo fluidInfo)) {
			return WHITE;
		}

		Fluid fluid = fluidInfo.getFluid(stack);
		if (fluid == Fluids.EMPTY) {
			return WHITE;
		}

		int color = IClientFluidTypeExtensions.of(fluid).getTintColor();

		// TEMPORARY diagnostics, see DynamicCellModel.debug
		if (DynamicCellModel.debug("log") && loggedTints < 8) {
			loggedTints++;
			TechReborn.LOGGER.info("Cell model: debug tint of {} is 0x{}", BuiltInRegistries.FLUID.getKey(fluid), Integer.toHexString(color));
		}
		if (DynamicCellModel.debug("whitetint")) {
			return WHITE;
		}

		return color;
	}

	private static int loggedTints = 0;
}
