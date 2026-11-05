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

package techreborn.items.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import reborncore.api.items.ArmorBlockEntityTicker;
import reborncore.api.items.ArmorRemoveHandler;
import reborncore.common.powerSystem.RcEnergyTier;
import techreborn.config.TechRebornConfig;
import techreborn.utils.TRItemUtils;

import java.util.List;

public class NanoSuitItem extends TREnergyArmourItem implements ArmorBlockEntityTicker, ArmorRemoveHandler {
	private static final ItemAttributeModifiers FULL_SUIT = new AttributeModifierBuilder().armor(10).toughness(4).build();
	private final ItemAttributeModifiers noPowerAttributes;
	private final ItemAttributeModifiers hasPowerAttributes;
	private final ItemAttributeModifiers fullSuitAttributes;

	public NanoSuitItem(Holder<ArmorMaterial> material, Type slot) {
		super(material, slot, TechRebornConfig.nanoSuitCapacity, RcEnergyTier.HIGH);
		switch (slot) {
			case HELMET, BOOTS:
				noPowerAttributes = new AttributeModifierBuilder(slot).armor(1).build();
				hasPowerAttributes = new AttributeModifierBuilder(slot).armor(3).toughness(2).knockback(1).build();
				fullSuitAttributes = new AttributeModifierBuilder(slot).armor(5).toughness(3).knockback(1).tooltip(false).build();
				break;
			case CHESTPLATE:
				noPowerAttributes = new AttributeModifierBuilder(slot).armor(2).build();
				hasPowerAttributes = new AttributeModifierBuilder(slot).armor(6).toughness(2).knockback(1).build();
				fullSuitAttributes = new AttributeModifierBuilder(slot).armor(10).toughness(3).knockback(1).tooltip(false).build();
				break;
			case LEGGINGS:
				noPowerAttributes = new AttributeModifierBuilder(slot).armor(3).build();
				hasPowerAttributes = new AttributeModifierBuilder(slot).armor(8).toughness(2).knockback(1).build();
				fullSuitAttributes = new AttributeModifierBuilder(slot).armor(10).toughness(3).knockback(1).tooltip(false).build();
				break;
			default:
				throw new IllegalArgumentException("Invalid slot type");
		}
	}

	// TREnergyArmourItem
	@Override
	public long getEnergyMaxOutput(ItemStack stack) { return 0; }

	// ArmorBlockEntityTicker
	@Override
	public void tickArmor(ItemStack stack, boolean hasFullSuit, Player playerEntity) {
		// Night Vision
		if (this.getEquipmentSlot() == EquipmentSlot.HEAD) {
			if (TRItemUtils.isActive(stack) && tryUseEnergy(stack, TechRebornConfig.suitNightVisionCost)) {
				playerEntity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 1, false, false));
			} else {
				playerEntity.removeEffect(MobEffects.NIGHT_VISION);
			}
		}
		applyModifier(stack, hasFullSuit);
	}

	private void applyModifier(ItemStack stack, ItemAttributeModifiers attributes, ItemAttributeModifiers target) {
		if (attributes != target) {
			stack.set(DataComponents.ATTRIBUTE_MODIFIERS, target);
		}
	}

	public void applyModifier(ItemStack stack, boolean hasFullSuit) {
		ItemAttributeModifiers attributes = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
		if (getStoredEnergy(stack) > 0) {
			if (hasFullSuit) {
				applyModifier(stack, attributes, fullSuitAttributes);
			} else {
				applyModifier(stack, attributes, hasPowerAttributes);
			}
		} else {
			applyModifier(stack, attributes, noPowerAttributes);
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack thisStack = user.getItemInHand(hand);
		EquipmentSlot slotType = this.getEquipmentSlot();
		if (user.isShiftKeyDown() && slotType == EquipmentSlot.HEAD) {
			TRItemUtils.switchActive(thisStack, 1, user);
			return InteractionResultHolder.success(thisStack);
		}
		return super.use(world, user, hand);
	}

	@Override
	public void onRemoved(Player playerEntity) {
		playerEntity.removeEffect(MobEffects.NIGHT_VISION);
		ItemStack stack = playerEntity.inventoryMenu.getCarried();
		if (stack.getItem() instanceof NanoSuitItem nanoSuitItem) {
			nanoSuitItem.applyModifier(stack, false);
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			playerEntity.getInventory().items.forEach(itemStack -> {
				if (itemStack.getItem() instanceof NanoSuitItem nanoSuitItem) {
					nanoSuitItem.applyModifier(itemStack, false);
					itemStack.remove(DataComponents.CUSTOM_DATA);
				}
			});
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag type) {
		if (this.type == Type.HELMET) {
			TRItemUtils.buildActiveTooltip(stack, tooltip);
		}
	}

	/*
	 * Port note: appendArmorTooltip is not ported. It only built the fancy armour tooltip and it was
	 * called from the client tooltip handler, which is not ported yet; it comes back with the client
	 * side. Nothing about how the armour actually works depends on it.
	 */
}
