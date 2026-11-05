/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 TeamReborn
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
 *
 * NeoForge port (c) 2026 RickWeek, based on the upstream Fabric sources.
 */

package reborncore.common.powerSystem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.RebornCore;

/**
 * Capability wiring for RebornCore.
 * <p>
 * IMPORTANT PORT NOTE. Upstream registered energy and fluid with
 * {@code EnergyStorage.SIDED.registerFallback(...)} and {@code FluidStorage.SIDED.registerFallback(...)},
 * a fallback that applied to EVERY block entity in the game. NeoForge has no global fallback for
 * block capabilities: they are registered per BlockEntityType.
 * <p>
 * Items are handled automatically here, by walking the item registry for {@link RcEnergyItem}.
 * Block entities cannot be discovered that way, so the mod that owns them has to hand over its
 * types: Tech Reborn calls {@link #registerMachines} with everything in TRBlockEntities. Forget
 * that call and the machines simply will not exchange energy, fluid or items, with no error to
 * show for it.
 */
public class RebornCapabilities {

	/**
	 * Registers the Forge Energy capability for every registered item implementing
	 * {@link RcEnergyItem}. Safe to call once from a RegisterCapabilitiesEvent listener.
	 */
	public static void registerItems(RegisterCapabilitiesEvent event) {
		int count = 0;

		for (Item item : BuiltInRegistries.ITEM) {
			if (item instanceof RcEnergyItem energyItem) {
				event.registerItem(
					Capabilities.EnergyStorage.ITEM,
					(stack, ctx) -> new RcItemEnergyStorage(stack, energyItem),
					item
				);
				count++;
			}
		}

		RebornCore.LOGGER.debug("Registered the energy capability for {} items", count);
	}

	/**
	 * Registers the energy and fluid capabilities for the given machine block entity types.
	 * <p>
	 * The mod that owns the block entities calls this once from a RegisterCapabilitiesEvent listener,
	 * passing every type whose block entity extends MachineBaseBlockEntity.
	 */
	public static void registerMachines(RegisterCapabilitiesEvent event, BlockEntityType<?>... types) {
		for (BlockEntityType<?> type : types) {
			event.registerBlockEntity(
				Capabilities.EnergyStorage.BLOCK,
				type,
				(blockEntity, side) -> blockEntity instanceof PowerAcceptorBlockEntity acceptor
					? acceptor.getSideEnergyStorage(side)
					: null
			);

			event.registerBlockEntity(
				Capabilities.FluidHandler.BLOCK,
				type,
				(blockEntity, side) -> blockEntity instanceof MachineBaseBlockEntity machine
					? machine.getTank()
					: null
			);

			event.registerBlockEntity(
				Capabilities.ItemHandler.BLOCK,
				type,
				(blockEntity, side) -> blockEntity instanceof MachineBaseBlockEntity machine
					? (side == null ? new InvWrapper(machine) : new SidedInvWrapper(machine, side))
					: null
			);
		}

		RebornCore.LOGGER.debug("Registered machine capabilities for {} block entity types", types.length);
	}

	private RebornCapabilities() {
	}
}
