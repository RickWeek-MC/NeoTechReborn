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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import techreborn.config.TechRebornConfig;
import techreborn.items.armor.NanoSuitItem;
import techreborn.items.armor.QuantumSuitItem;

/**
 * Nano and Quantum armour burning energy to soak up damage.
 * <p>
 * Port note: upstream went through RebornCore's ApplyArmorToDamageCallback, which a mixin on
 * {@code getDamageAfterArmorAbsorb} fired. Block 13 of this port removed every mixin, and that
 * callback with them, so this now listens to NeoForge's {@link LivingDamageEvent.Pre} instead.
 * That event fires after armour and enchantments have already been taken off the damage, which is
 * the same point upstream's hook ran at, and setNewDamage replaces the returned value.
 * <p>
 * Watch out: this is the piece the notes warned about. The callback existed for a while with nobody
 * firing it, and the armour silently absorbed nothing.
 */
public class ApplyArmorToDamageHandler {

	public static void init() {
		NeoForge.EVENT_BUS.addListener(ApplyArmorToDamageHandler::onLivingDamage);
	}

	private static void onLivingDamage(LivingDamageEvent.Pre event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}

		float amount = event.getNewDamage();
		if (amount <= 0) {
			return;
		}

		double damageAbsorbed = 0.0d;
		for (ItemStack stack : player.getArmorSlots()) {
			Item stackItem = stack.getItem();
			if (stackItem instanceof NanoSuitItem item) {
				long energy = item.getStoredEnergy(stack);
				if (energy > TechRebornConfig.nanoArmorEnergyCost) {
					item.setStoredEnergy(stack, energy - TechRebornConfig.nanoArmorEnergyCost);
				} else if (energy != 0) {
					item.setStoredEnergy(stack, 0);
				}
				continue;
			}
			if (!(stackItem instanceof QuantumSuitItem item)) {
				continue;
			}

			double stackEnergy = item.getStoredEnergy(stack);
			if (stackEnergy == 0.0d) {
				continue;
			}

			double damageToAbsorb = Math.min(stackEnergy, amount * 0.2d);
			if (item.tryUseEnergy(stack, (long) (damageToAbsorb * TechRebornConfig.damageAbsorbCost))) {
				damageAbsorbed += damageToAbsorb;
			}
		}

		if (damageAbsorbed > 0) {
			event.setNewDamage(Math.max(0f, amount - (float) damageAbsorbed));
		}
	}
}
