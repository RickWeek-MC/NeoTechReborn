/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 TeamReborn
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

package reborncore.common.powerSystem;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import reborncore.common.util.ItemUtils;

/**
 * Implement on simple energy-containing items and (on top of what {@link SimpleEnergyItem} does):
 * <ul>
 *     <li>A tooltip will be added for the item, indicating the stored power,
 *     the max power and the extraction rates.</li>
 *     <li>Any {@link RcEnergyItem} input in a crafting recipe input will automatically
 *     give its energy to the output if the output implements {@link RcEnergyItem}.</li>
 * </ul>
 * <p>
 * Port note: upstream also extended Fabric's FabricItem for the two "stack changed" hooks. On
 * NeoForge those live on IItemExtension, which Item already implements; this interface extends it
 * too so that these defaults win over the ones Item inherits (most specific interface wins).
 */
public interface RcEnergyItem extends SimpleEnergyItem, IItemExtension {
	@Override
	long getEnergyCapacity(ItemStack stack);

	/**
	 * @return {@link RcEnergyTier} the tier of this item, used to have standard I/O rates.
	 * <p>
	 * Port note: upstream called this {@code getTier()}. It had to be renamed, and this is the only
	 * change RebornCore needed after it was finished. On NeoForge every tool item extends
	 * {@code TieredItem}, which already has a {@code Tier getTier()}; a class cannot have two
	 * methods with the same name and different return types, so TechReborn's drills, chainsaws,
	 * jackhammers, nanosaber and omni tool -- all tools AND energy items -- would not compile.
	 * Yarn had no clash because its tool items expose {@code getMaterial()} instead.
	 * The new name also matches the other methods here (getEnergyCapacity, getEnergyMaxInput...).
	 */
	RcEnergyTier getEnergyTier();

	@Override
	default long getEnergyMaxInput(ItemStack stack) {
		return getEnergyTier().getMaxInput();
	}

	@Override
	default long getEnergyMaxOutput(ItemStack stack) {
		return getEnergyTier().getMaxOutput();
	}

	/**
	 * Port note: replaces FabricItem.allowComponentsUpdateAnimation. Returning false here keeps the
	 * item from replaying its equip animation every time its stored energy ticks down.
	 */
	@Override
	default boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		if (slotChanged) {
			return true;
		}
		return !ItemUtils.isEqualIgnoreEnergy(oldStack, newStack);
	}

	/**
	 * Port note: replaces FabricItem.allowContinuingBlockBreaking (note the inverted meaning:
	 * NeoForge asks whether breaking should RESET).
	 */
	@Override
	default boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
		return !ItemUtils.isEqualIgnoreEnergy(oldStack, newStack);
	}

	/**
	 * Tries to use energy with honor to Unbreaking enchantment
	 *
	 * @param stack ItemStack representing item with Energy
	 * @param amount Initial amount of energy to use. This will be further decreased according to level of Unbreaking
	 *               enchantment
	 * @return Returns true if was able to use that energy from that item
	 */
	@Override
	default boolean tryUseEnergy(ItemStack stack, long amount){
		RandomSource random = RandomSource.create();

		int unbreakingLevel = getUnbreakingLevel(stack);
		if (unbreakingLevel > 0) {
			amount = amount / (random.nextInt(unbreakingLevel) + 1);
		}
		return SimpleEnergyItem.super.tryUseEnergy(stack, amount);
	}

	// A hack to do this without context of the DRM
	private int getUnbreakingLevel(ItemStack stack) {
		ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		for (Holder<Enchantment> entry : enchantments.keySet()) {
			if (entry.is(Enchantments.UNBREAKING)) {
				return enchantments.getLevel(entry);
			}
		}
		return 0;
	}
}
