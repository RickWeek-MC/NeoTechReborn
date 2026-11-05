/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 TeamReborn
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

package reborncore.common.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import reborncore.common.fluid.container.FluidInstance;
import reborncore.common.util.Tank;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Port note: upstream drove all of this through Fabric's transfer API and its transactions, where a
 * whole multi-step move could be rolled back at the end. NeoForge has no transactions, so the
 * container moves below work on a COPY of the input stack: the copy is mutated for real, and the
 * result is only written back into the inventory once we know the output slot can take the emptied
 * or filled container. If it cannot, the copy is dropped and nothing changed.
 */
public class FluidUtils {

	@NotNull
	public static Fluid fluidFromBlock(Block block) {
		// Upstream read LiquidBlock's private fluid field through an access widener; the default
		// fluid state is public and gives the same answer.
		Fluid fluid = block.defaultBlockState().getFluidState().getType();
		return fluid == null ? Fluids.EMPTY : fluid;
	}

	public static List<Fluid> getAllFluids() {
		return BuiltInRegistries.FLUID.stream().collect(Collectors.toList());
	}

	public static boolean drainContainers(Tank tank, Container inventory, int inputSlot, int outputSlot) {
		return drainContainers(tank, inventory, inputSlot, outputSlot, false);
	}

	/**
	 * Moves fluid out of the container in {@code inputSlot} and into {@code tank}, putting the
	 * emptied container in {@code outputSlot}.
	 *
	 * @param voidFluid when true the fluid is thrown away instead of going into the tank
	 */
	public static boolean drainContainers(Tank tank, Container inventory, int inputSlot, int outputSlot, boolean voidFluid) {
		ItemStack inputStack = inventory.getItem(inputSlot);

		if (inputStack.isEmpty()) {
			return false;
		}

		IFluidHandlerItem handler = FluidUtil.getFluidHandler(inputStack.copyWithCount(1)).orElse(null);

		if (handler == null) {
			return false;
		}

		FluidStack available = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);

		if (available.isEmpty()) {
			return false;
		}

		int toMove = voidFluid ? available.getAmount() : tank.fill(available, IFluidHandler.FluidAction.SIMULATE);

		if (toMove <= 0) {
			return false;
		}

		FluidStack drained = handler.drain(toMove, IFluidHandler.FluidAction.EXECUTE);

		if (drained.isEmpty()) {
			return false;
		}

		ItemStack emptied = handler.getContainer();

		if (!canStow(inventory, outputSlot, emptied)) {
			// Nothing has been written back yet, so dropping the copy undoes everything.
			return false;
		}

		if (!voidFluid) {
			tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
		}

		consumeInputAndStow(inventory, inputSlot, outputSlot, emptied);
		return true;
	}

	/**
	 * Moves fluid out of {@code source} and into the container in {@code inputSlot}, putting the
	 * filled container in {@code outputSlot}.
	 */
	public static boolean fillContainers(Tank source, Container inventory, int inputSlot, int outputSlot) {
		ItemStack inputStack = inventory.getItem(inputSlot);

		if (inputStack.isEmpty() || source.isEmpty()) {
			return false;
		}

		IFluidHandlerItem handler = FluidUtil.getFluidHandler(inputStack.copyWithCount(1)).orElse(null);

		if (handler == null) {
			return false;
		}

		FluidStack available = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);

		if (available.isEmpty()) {
			return false;
		}

		int accepted = handler.fill(available, IFluidHandler.FluidAction.SIMULATE);

		if (accepted <= 0) {
			return false;
		}

		FluidStack drained = source.drain(accepted, IFluidHandler.FluidAction.SIMULATE);

		if (drained.isEmpty()) {
			return false;
		}

		int filled = handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);

		if (filled <= 0) {
			return false;
		}

		ItemStack result = handler.getContainer();

		if (!canStow(inventory, outputSlot, result)) {
			return false;
		}

		source.drain(filled, IFluidHandler.FluidAction.EXECUTE);
		consumeInputAndStow(inventory, inputSlot, outputSlot, result);
		return true;
	}

	/**
	 * @return whether {@code stack} can be placed into {@code slot}, either because the slot is
	 * empty or because it already holds a matching stack with room left.
	 */
	private static boolean canStow(Container inventory, int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return true;
		}

		ItemStack existing = inventory.getItem(slot);

		if (existing.isEmpty()) {
			return true;
		}

		return ItemStack.isSameItemSameComponents(existing, stack)
			&& existing.getCount() + stack.getCount() <= Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize());
	}

	private static void consumeInputAndStow(Container inventory, int inputSlot, int outputSlot, ItemStack stack) {
		inventory.getItem(inputSlot).shrink(1);

		if (stack.isEmpty()) {
			return;
		}

		ItemStack existing = inventory.getItem(outputSlot);

		if (existing.isEmpty()) {
			inventory.setItem(outputSlot, stack);
		} else {
			existing.grow(stack.getCount());
		}
	}

	public static boolean fluidEquals(@NotNull Fluid fluid, @NotNull Fluid fluid1) {
		return fluid == fluid1;
	}

	public static boolean isContainer(ItemStack stack) {
		return FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
	}

	public static boolean isContainerEmpty(ItemStack stack) {
		IFluidHandlerItem handler = FluidUtil.getFluidHandler(stack.copyWithCount(1)).orElse(null);

		if (handler == null) {
			return false;
		}

		return handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).isEmpty();
	}

	public static boolean containsMatchingFluid(ItemStack stack, Predicate<Fluid> predicate) {
		IFluidHandlerItem handler = FluidUtil.getFluidHandler(stack.copyWithCount(1)).orElse(null);

		if (handler == null) {
			return false;
		}

		for (int tank = 0; tank < handler.getTanks(); tank++) {
			FluidStack contents = handler.getFluidInTank(tank);

			if (!contents.isEmpty() && predicate.test(contents.getFluid())) {
				return true;
			}
		}

		return false;
	}

	@Deprecated
	public static boolean interactWithFluidHandler(Player playerIn, Tank tank) {
		// TODO
		return false;
	}

	public static String getFluidName(@NotNull FluidInstance fluidInstance) {
		return getFluidName(fluidInstance.fluid());
	}

	public static String getFluidName(@NotNull Fluid fluid) {
		return Component.translatable(fluid.defaultFluidState().createLegacyBlock().getBlock().getDescriptionId()).getString();
	}
}
