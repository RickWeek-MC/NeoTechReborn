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

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import techreborn.TechReborn;

/**
 * We use custom materials to allow LevelZ to tweak the mining level of our tools.
 * <p>
 * Port note: Yarn ToolMaterial(s) is vanilla {@link Tier}/{@link Tiers} here. The anonymous class
 * keeps delegating everything, including the toString() that LevelZ reads to tell the tiers apart.
 */
public class TRToolMaterials {
	public static final Tier BASIC_CHAINSAW = copy(Tiers.IRON, "basic_chainsaw");
	public static final Tier BASIC_DRILL = copy(Tiers.IRON, "basic_drill");
	public static final Tier BASIC_JACKHAMMER = copy(Tiers.DIAMOND, "basic_jackhammer");

	public static final Tier ADVANCED_CHAINSAW = copy(Tiers.DIAMOND, "advanced_chainsaw");
	public static final Tier ADVANCED_DRILL = copy(Tiers.DIAMOND, "advanced_drill");
	public static final Tier ADVANCED_JACKHAMMER = copy(Tiers.NETHERITE, "advanced_jackhammer");

	public static final Tier INDUSTRIAL_CHAINSAW = copy(Tiers.NETHERITE, "industrial_chainsaw");
	public static final Tier INDUSTRIAL_DRILL = copy(Tiers.NETHERITE, "industrial_drill");
	public static final Tier INDUSTRIAL_JACKHAMMER = copy(Tiers.NETHERITE, "industrial_jackhammer");

	public static final Tier ROCK_CUTTER = copy(Tiers.DIAMOND, "rock_cutter");
	public static final Tier NANOSABER = copy(Tiers.NETHERITE, "nanosaber");
	public static final Tier OMNI_TOOL = copy(Tiers.NETHERITE, "omni_tool");

	public static Tier copy(Tier material, String id) {
		return new Tier() {
			@Override
			public int getUses() {
				return material.getUses();
			}

			@Override
			public float getSpeed() {
				return material.getSpeed();
			}

			@Override
			public float getAttackDamageBonus() {
				return material.getAttackDamageBonus();
			}

			@Override
			public TagKey<Block> getIncorrectBlocksForDrops() {
				return material.getIncorrectBlocksForDrops();
			}

			@Override
			public int getEnchantmentValue() {
				return material.getEnchantmentValue();
			}

			@Override
			public Ingredient getRepairIngredient() {
				return material.getRepairIngredient();
			}

			// This allows LevelZ to identify the material.
			@Override
			public String toString() {
				return TechReborn.MOD_ID + ":" + id;
			}
		};
	}
}
