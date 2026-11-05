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
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import reborncore.api.items.ArmorBlockEntityTicker;
import reborncore.api.items.ArmorRemoveHandler;
import reborncore.common.powerSystem.RcEnergyTier;
import techreborn.config.TechRebornConfig;
import techreborn.utils.TRItemUtils;

import java.util.List;

public class QuantumSuitItem extends TREnergyArmourItem implements ArmorBlockEntityTicker, ArmorRemoveHandler {
	public static QuantumSuitFlightHandler HANDLER = new VanillaQuantumSuitFlightHandler();

	private static final ItemAttributeModifiers FULL_SUIT = new AttributeModifierBuilder().armor(10).toughness(8).knockback(6).build();
	private final ItemAttributeModifiers noPowerAttributes;
	private final ItemAttributeModifiers hasPowerAttributes;
	private final ItemAttributeModifiers fullSuitAttributes;
	private final ItemAttributeModifiers hasPowerSprintAttributes;
	private final ItemAttributeModifiers fullSuitSprintAttributes;

	public QuantumSuitItem(Holder<ArmorMaterial> material, Type slot) {
		super(material, slot, TechRebornConfig.quantumSuitCapacity, RcEnergyTier.INSANE);
		switch (slot) {
			case HELMET, BOOTS:
				noPowerAttributes = new AttributeModifierBuilder(slot).armor(3).toughness(2).build();
				hasPowerAttributes = new AttributeModifierBuilder(slot).armor(3).toughness(3).knockback(1).build();
				fullSuitAttributes = new AttributeModifierBuilder(slot).armor(5).toughness(5).knockback(2).tooltip(false).build();
				hasPowerSprintAttributes = fullSuitSprintAttributes = null;
				break;
			case CHESTPLATE:
				noPowerAttributes = new AttributeModifierBuilder(slot).armor(6).toughness(2).build();
				hasPowerAttributes = new AttributeModifierBuilder(slot).armor(6).toughness(3).knockback(1).build();
				fullSuitAttributes = new AttributeModifierBuilder(slot).armor(10).toughness(5).knockback(3).tooltip(false).build();
				hasPowerSprintAttributes = fullSuitSprintAttributes = null;
				break;
			case LEGGINGS: {
				AttributeModifier modifier = new AttributeModifier(
					ResourceLocation.fromNamespaceAndPath("techreborn", "quantum_movement_speed"),
					0.15,
					AttributeModifier.Operation.ADD_VALUE
				);
				noPowerAttributes = new AttributeModifierBuilder(slot).armor(8).toughness(2).build();
				hasPowerAttributes = new AttributeModifierBuilder(slot).armor(8).toughness(3).knockback(1).build();
				hasPowerSprintAttributes = hasPowerAttributes.withModifierAdded(Attributes.MOVEMENT_SPEED, modifier, EquipmentSlotGroup.LEGS);
				fullSuitAttributes = new AttributeModifierBuilder(slot).armor(10).toughness(5).knockback(3).tooltip(false).build();
				fullSuitSprintAttributes = fullSuitAttributes.withModifierAdded(Attributes.MOVEMENT_SPEED, modifier, EquipmentSlotGroup.LEGS);
				break;
			}
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
		final EquipmentSlot slotType = this.getEquipmentSlot();
		switch (slotType) {
			case HEAD -> {
				// Water Breathing
				if (playerEntity.isUnderWater() && tryUseEnergy(stack, TechRebornConfig.quantumSuitBreathingCost)) {
					playerEntity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 5, 1));
				}

				// Night Vision
				if (TRItemUtils.isActive(stack) && tryUseEnergy(stack, TechRebornConfig.suitNightVisionCost)) {
					playerEntity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 1, false, false));
				} else {
					playerEntity.removeEffect(MobEffects.NIGHT_VISION);
				}
			}
			case CHEST -> {
				if (TechRebornConfig.quantumSuitEnableFlight && playerEntity instanceof ServerPlayer && !playerEntity.isCreative()) {
					if (getStoredEnergy(stack) > TechRebornConfig.quantumSuitFlyingCost) {
						HANDLER.setAllowFlight(playerEntity, true);

						if (HANDLER.isFlying(playerEntity)) {
							tryUseEnergy(stack, TechRebornConfig.quantumSuitFlyingCost);
						}
						playerEntity.setOnGround(true);
					} else {
						HANDLER.setAllowFlight(playerEntity, false);
					}
				}
				if (playerEntity.isOnFire() && tryUseEnergy(stack, TechRebornConfig.fireExtinguishCost)) {
					playerEntity.clearFire();
				}
			}
			case LEGS -> {
				boolean sprint = TechRebornConfig.quantumSuitEnableSprint && TRItemUtils.isActive(stack);
				if (sprint && playerEntity.isSprinting()) {
					tryUseEnergy(stack, TechRebornConfig.quantumSuitSprintingCost);
				}
				applyModifier(stack, hasFullSuit, sprint);
				return;
			}
			case FEET -> {
				if (playerEntity.isSwimming() && tryUseEnergy(stack, TechRebornConfig.quantumSuitSwimmingCost)) {
					playerEntity.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 5, 1, true, false));
				}
			}
		}
		applyModifier(stack, hasFullSuit, false);
	}

	private void applyModifier(ItemStack stack, ItemAttributeModifiers attributes, ItemAttributeModifiers target) {
		if (attributes != target) {
			stack.set(DataComponents.ATTRIBUTE_MODIFIERS, target);
		}
	}

	public void applyModifier(ItemStack stack, boolean hasFullSuit, boolean sprintEnable) {
		ItemAttributeModifiers attributes = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
		long energy = getStoredEnergy(stack);
		if (energy > 0) {
			if (sprintEnable && energy >= TechRebornConfig.quantumSuitSprintingCost) {
				if (hasFullSuit) {
					applyModifier(stack, attributes, fullSuitSprintAttributes);
				} else {
					applyModifier(stack, attributes, hasPowerSprintAttributes);
				}
			} else if (hasFullSuit) {
				applyModifier(stack, attributes, fullSuitAttributes);
			} else {
				applyModifier(stack, attributes, hasPowerAttributes);
			}
		} else {
			applyModifier(stack, attributes, noPowerAttributes);
		}
	}

	// ArmorRemoveHandler
	@Override
	public void onRemoved(Player playerEntity) {
		if (this.getEquipmentSlot() == EquipmentSlot.CHEST && TechRebornConfig.quantumSuitEnableFlight) {
			if (!playerEntity.isCreative() && !playerEntity.isSpectator()) {
				HANDLER.setAllowFlight(playerEntity, false);
			}
		} else if (this.getEquipmentSlot() == EquipmentSlot.HEAD) {
			playerEntity.removeEffect(MobEffects.NIGHT_VISION);
		}
		ItemStack stack = playerEntity.inventoryMenu.getCarried();
		if (stack.getItem() instanceof QuantumSuitItem quantumSuitItem) {
			quantumSuitItem.applyModifier(stack, false, false);
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			playerEntity.getInventory().items.forEach(itemStack -> {
				if (itemStack.getItem() instanceof QuantumSuitItem quantumSuitItem) {
					quantumSuitItem.applyModifier(itemStack, false, false);
					itemStack.remove(DataComponents.CUSTOM_DATA);
				}
			});
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack thisStack = user.getItemInHand(hand);
		EquipmentSlot slotType = this.getEquipmentSlot();
		if (user.isShiftKeyDown() && (slotType == EquipmentSlot.HEAD || slotType == EquipmentSlot.LEGS)) {
			TRItemUtils.switchActive(thisStack, 1, user);
			return InteractionResultHolder.success(thisStack);
		}
		return super.use(world, user, hand);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag type) {
		if (this.getEquipmentSlot() == EquipmentSlot.HEAD) {
			TRItemUtils.buildActiveTooltip(stack, tooltip);
		}

		// Will only add Inactive/Active tooltip if sprint is enabled
		if (this.getEquipmentSlot() == EquipmentSlot.LEGS && TechRebornConfig.quantumSuitEnableSprint) {
			TRItemUtils.buildActiveTooltip(stack, tooltip);
		}
	}

	/*
	 * Port note: appendArmorTooltip is not ported. It only built the fancy armour tooltip and it was
	 * called from the client tooltip handler, which is not ported yet; it comes back with the client
	 * side. Nothing about how the armour actually works depends on it.
	 */
}
