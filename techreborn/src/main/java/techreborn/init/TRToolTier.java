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

package techreborn.init;

import com.google.common.base.Suppliers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * Port note: Yarn's {@code ToolMaterial} is vanilla's {@link Tier} here. Method names differ
 * (getDurability -> getUses, getMiningSpeedMultiplier -> getSpeed, getAttackDamage ->
 * getAttackDamageBonus, getInverseTag -> getIncorrectBlocksForDrops, getEnchantability ->
 * getEnchantmentValue), the values are untouched.
 */
public enum TRToolTier implements Tier {
	BRONZE(BlockTags.INCORRECT_FOR_IRON_TOOL, 375, 7.0F, 6, 6, () -> Ingredient.of(TRContent.Ingots.BRONZE.asItem())),
	RUBY(BlockTags.INCORRECT_FOR_IRON_TOOL, 750, 6.0F, 5, 10, () -> Ingredient.of(TRContent.Gems.RUBY.asItem())),
	SAPPHIRE(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1000, 7.0F, 5, 12, () -> Ingredient.of(TRContent.Gems.SAPPHIRE.asItem())),
	PERIDOT(BlockTags.INCORRECT_FOR_IRON_TOOL, 750, 7.0F, 5, 12, () -> Ingredient.of(TRContent.Gems.PERIDOT.asItem()));

	/**
	 * BlockTags for blocks which shouldn't be mined with this material.
	 */
	private final TagKey<Block> inverseTag;

	/**
	 * The number of uses this material allows. (wood = 59, stone = 131, iron = 250,
	 * diamond = 1561, gold = 32)
	 */
	private final int itemDurability;
	/**
	 * The strength of this tool material against blocks which it is effective
	 * against.
	 */
	private final float miningSpeed;
	/**
	 * Damage versus entities.
	 */
	private final float attackDamage;
	/**
	 * Defines the natural enchantability factor of the material.
	 */
	private final int enchantability;
	/**
	 * Ingredient which can repair this material.
	 * <p>
	 * Port note: memoized, like upstream, and that memoization is load bearing here -- the
	 * ingredient names a TRContent item, and the tiers are built while the registries are still
	 * being filled.
	 */
	private final Supplier<Ingredient> repairMaterial;

	TRToolTier(TagKey<Block> inverseTag, int itemDurability, float miningSpeed, float attackDamage, int enchantability, Supplier<Ingredient> repairIngredient) {
		this.inverseTag = inverseTag;
		this.itemDurability = itemDurability;
		this.miningSpeed = miningSpeed;
		this.attackDamage = attackDamage;
		this.enchantability = enchantability;
		this.repairMaterial = Suppliers.memoize(repairIngredient::get);
	}

	@Override
	public int getUses() {
		return itemDurability;
	}

	@Override
	public float getSpeed() {
		return miningSpeed;
	}

	@Override
	public float getAttackDamageBonus() {
		return attackDamage;
	}

	@Override
	public TagKey<Block> getIncorrectBlocksForDrops() {
		return this.inverseTag;
	}

	@Override
	public int getEnchantmentValue() {
		return enchantability;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return repairMaterial.get();
	}

	public ItemAttributeModifiers createAttributeModifiers(ToolType toolType) {
		return ItemAttributeModifiers.builder()
			.add(
				Attributes.ATTACK_DAMAGE,
				new AttributeModifier(
					Item.BASE_ATTACK_DAMAGE_ID, toolType.baseAttackDamage + getAttackDamageBonus(), AttributeModifier.Operation.ADD_VALUE
				),
				EquipmentSlotGroup.MAINHAND
			)
			.add(
				Attributes.ATTACK_SPEED,
				new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, toolType.attackSpeed, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND
			)
			.build();
	}

	public record ToolType(float baseAttackDamage, float attackSpeed) {
		public static final ToolType SWORD = new ToolType(0, -2F);
		public static final ToolType PICKAXE = new ToolType(-2, -2.8F);
		public static final ToolType AXE = new ToolType(3, -2.9F);
		public static final ToolType SHOVEL = new ToolType(-2, -3.0F);
		public static final ToolType HOE = new ToolType(-4, 0.0F);
	}
}
