/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2025 TechReborn
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

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static techreborn.TechReborn.MOD_ID;

/**
 * Port note: only the builder and {@link #equals} are ported. The static text helpers upstream had
 * here were tooltip formatting for the client, and they come back with the client side.
 */
public class AttributeModifierBuilder {
	private final ItemAttributeModifiers.Builder builder;
	private final EquipmentSlotGroup target;
	private boolean tooltip = true;

	public AttributeModifierBuilder() {
		this(null);
	}

	public AttributeModifierBuilder(@Nullable ArmorItem.Type slot) {
		builder = ItemAttributeModifiers.builder();
		target = slot == null ? null : EquipmentSlotGroup.bySlot(slot.getSlot());
	}

	private AttributeModifier modifier(String path, double value) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, (target == null ? path : path + "/" + target.getSerializedName()));
		return new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE);
	}

	public AttributeModifierBuilder armor(int i) {
		builder.add(Attributes.ARMOR, modifier("nano_suit_armor", i), target);
		return this;
	}

	public AttributeModifierBuilder toughness(int i) {
		builder.add(Attributes.ARMOR_TOUGHNESS, modifier("nano_suit_armor_toughness", i), target);
		return this;
	}

	public AttributeModifierBuilder knockback(double i) {
		builder.add(Attributes.KNOCKBACK_RESISTANCE, modifier("nano_suit_knockback_resistance", i / 10), target);
		return this;
	}

	public AttributeModifierBuilder tooltip(boolean show) {
		tooltip = show;
		return this;
	}

	public ItemAttributeModifiers build() {
		ItemAttributeModifiers component = builder.build();
		return tooltip ? component : component.withTooltip(false);
	}

	public static boolean equals(@Nullable ItemAttributeModifiers attributes, ItemAttributeModifiers target) {
		if (attributes == null) {
			return false;
		}
		List<ItemAttributeModifiers.Entry> m1 = attributes.modifiers();
		List<ItemAttributeModifiers.Entry> m2 = target.modifiers();
		if (m1.size() < m2.size()) {
			return false;
		}
		Map<ResourceLocation, Double> map = new HashMap<>();
		m1.forEach(entry -> map.put(entry.modifier().id(), entry.modifier().amount()));
		for (ItemAttributeModifiers.Entry entry : m2) {
			if (map.get(entry.modifier().id()) != entry.modifier().amount()) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Port note: the static tooltip text helpers upstream had here are not ported. They were
	 * formatting for the client tooltip and come back with the client side.
	 */
}
