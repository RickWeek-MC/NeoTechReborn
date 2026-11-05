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

package techreborn.events;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import techreborn.init.TRContent;
import techreborn.items.CellFluidHandler;

/**
 * Where this mod's items and blocks say what capabilities they expose.
 * <p>
 * Port note: this replaces the {@code registerFluidApi()} calls upstream made from its registry
 * code, which on Fabric could register a storage the moment the item was built. On NeoForge
 * capabilities are collected by one event instead, and that event runs after the registries are
 * filled, which is why the item fields are read inside the method and not captured earlier.
 * <p>
 * BEWARE when adding to this: a missing registration here produces NO error of any kind. The item
 * works, looks right and simply exchanges nothing with anything. The cell is the clearest case --
 * if its line below is gone, it fills from no machine and no tank, in silence.
 */
public class TRCapabilities {

	public static void init(IEventBus modEventBus) {
		modEventBus.addListener(TRCapabilities::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		// The cell: one bucket of any fluid, kept in the stack's FLUID component.
		event.registerItem(
			Capabilities.FluidHandler.ITEM,
			(stack, context) -> new CellFluidHandler(stack),
			TRContent.CELL
		);
	}

	private TRCapabilities() {/* No instantiation. */}
}
