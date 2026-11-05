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

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import techreborn.TechReborn;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * Port note: upstream registered every armor material eagerly with
 * {@code Registry.registerReference} while the class was initializing. That cannot work here: the
 * armor material registry is frozen outside the registry event, so the materials go through a
 * {@link DeferredRegister} and {@link #register(IEventBus)} has to be called from the mod
 * constructor.
 * <p>
 * The fields are {@link Holder}s either way, which is what {@link ArmorItem} wants, so no call site
 * changes. The repair ingredient stays a {@link Supplier}: it names a TRContent item, which does not
 * exist yet when the material is built.
 * <p>
 * Order matters and is already handled: RebornCore's content loaders deliberately run after the
 * armor material registry has been built, so the materials exist by the time the armor items are
 * created.
 */
public class TRArmorMaterials {
	private static final DeferredRegister<ArmorMaterial> MATERIALS =
		DeferredRegister.create(Registries.ARMOR_MATERIAL, TechReborn.MOD_ID);

	public static final Holder<ArmorMaterial> BRONZE = register("bronze", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 2);
		map.put(ArmorItem.Type.LEGGINGS, 5);
		map.put(ArmorItem.Type.CHESTPLATE, 6);
		map.put(ArmorItem.Type.HELMET, 3);
		map.put(ArmorItem.Type.BODY, 7);
	}), 8, SoundEvents.ARMOR_EQUIP_IRON, 0.0f, 0.1f, () -> Ingredient.of(TRContent.Ingots.BRONZE.asItem()));

	public static final Holder<ArmorMaterial> SILVER = register("silver", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 2);
		map.put(ArmorItem.Type.LEGGINGS, 5);
		map.put(ArmorItem.Type.CHESTPLATE, 3);
		map.put(ArmorItem.Type.HELMET, 1);
		map.put(ArmorItem.Type.BODY, 5);
	}), 15, SoundEvents.ARMOR_EQUIP_GOLD, 0.0f, 0.0f, () -> Ingredient.of(TRContent.Ingots.SILVER.asItem()));

	public static final Holder<ArmorMaterial> STEEL = register("steel", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 2);
		map.put(ArmorItem.Type.LEGGINGS, 6);
		map.put(ArmorItem.Type.CHESTPLATE, 5);
		map.put(ArmorItem.Type.HELMET, 3);
		map.put(ArmorItem.Type.BODY, 11);
	}), 5, SoundEvents.ARMOR_EQUIP_IRON, 1.75f, 0.1f, () -> Ingredient.of(TRContent.Ingots.STEEL.asItem()));

	public static final Holder<ArmorMaterial> RUBY = register("ruby", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 2);
		map.put(ArmorItem.Type.LEGGINGS, 5);
		map.put(ArmorItem.Type.CHESTPLATE, 7);
		map.put(ArmorItem.Type.HELMET, 2);
		map.put(ArmorItem.Type.BODY, 7);
	}), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 0.0f, 0.0f, () -> Ingredient.of(TRContent.Gems.RUBY.asItem()));

	public static final Holder<ArmorMaterial> SAPPHIRE = register("sapphire", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 4);
		map.put(ArmorItem.Type.LEGGINGS, 4);
		map.put(ArmorItem.Type.CHESTPLATE, 4);
		map.put(ArmorItem.Type.HELMET, 4);
		map.put(ArmorItem.Type.BODY, 7);
	}), 8, SoundEvents.ARMOR_EQUIP_DIAMOND, 0.0f, 0.0f, () -> Ingredient.of(TRContent.Gems.SAPPHIRE.asItem()));

	public static final Holder<ArmorMaterial> PERIDOT = register("peridot", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 2);
		map.put(ArmorItem.Type.LEGGINGS, 3);
		map.put(ArmorItem.Type.CHESTPLATE, 8);
		map.put(ArmorItem.Type.HELMET, 3);
		map.put(ArmorItem.Type.BODY, 7);
	}), 16, SoundEvents.ARMOR_EQUIP_DIAMOND, 0.0f, 0.0f, () -> Ingredient.of(TRContent.Gems.PERIDOT.asItem()));

	public static final Holder<ArmorMaterial> QUANTUM = register("quantum", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 3);
		map.put(ArmorItem.Type.LEGGINGS, 8);
		map.put(ArmorItem.Type.CHESTPLATE, 6);
		map.put(ArmorItem.Type.HELMET, 3);
		map.put(ArmorItem.Type.BODY, 11);
	}), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0f, 0, () -> Ingredient.EMPTY);

	public static final Holder<ArmorMaterial> NANO = register("nano", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 1);
		map.put(ArmorItem.Type.LEGGINGS, 3);
		map.put(ArmorItem.Type.CHESTPLATE, 2);
		map.put(ArmorItem.Type.HELMET, 1);
		map.put(ArmorItem.Type.BODY, 3);
	}), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 0, 0, () -> Ingredient.EMPTY);

	public static final Holder<ArmorMaterial> CLOAKING_DEVICE = register("cloaking_device", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 0);
		map.put(ArmorItem.Type.LEGGINGS, 0);
		map.put(ArmorItem.Type.CHESTPLATE, 2);
		map.put(ArmorItem.Type.HELMET, 0);
		map.put(ArmorItem.Type.BODY, 0);
	}), 10, SoundEvents.ARMOR_EQUIP_GOLD, 0.0f, 0.0f, () -> Ingredient.EMPTY);

	public static final Holder<ArmorMaterial> LITHIUM_BATPACK = register("lithium_batpack", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 0);
		map.put(ArmorItem.Type.LEGGINGS, 0);
		map.put(ArmorItem.Type.CHESTPLATE, 5);
		map.put(ArmorItem.Type.HELMET, 0);
		map.put(ArmorItem.Type.BODY, 0);
	}), 10, SoundEvents.ARMOR_EQUIP_TURTLE, 0.0f, 0.0f, () -> Ingredient.EMPTY);

	public static final Holder<ArmorMaterial> LAPOTRONIC_ORBPACK = register("lapotronic_orbpack", Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
		map.put(ArmorItem.Type.BOOTS, 0);
		map.put(ArmorItem.Type.LEGGINGS, 0);
		map.put(ArmorItem.Type.CHESTPLATE, 6);
		map.put(ArmorItem.Type.HELMET, 0);
		map.put(ArmorItem.Type.BODY, 0);
	}), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0f, 0.0f, () -> Ingredient.EMPTY);

	/**
	 * Hooks the materials into the registry event. Call once from the mod constructor.
	 */
	public static void register(IEventBus modEventBus) {
		MATERIALS.register(modEventBus);
	}

	private static Holder<ArmorMaterial> register(String id, EnumMap<ArmorItem.Type, Integer> defense, int enchantability, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
		List<ArmorMaterial.Layer> list = List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, id)));
		return register(id, defense, enchantability, equipSound, toughness, knockbackResistance, repairIngredient, list);
	}

	private static Holder<ArmorMaterial> register(String id, EnumMap<ArmorItem.Type, Integer> defense, int enchantability, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient, List<ArmorMaterial.Layer> layers) {
		EnumMap<ArmorItem.Type, Integer> enumMap = new EnumMap<>(ArmorItem.Type.class);
		for (ArmorItem.Type type : ArmorItem.Type.values()) {
			enumMap.put(type, defense.get(type));
		}
		return MATERIALS.register(id, () -> new ArmorMaterial(enumMap, enchantability, equipSound, repairIngredient, layers, toughness, knockbackResistance));
	}
}
