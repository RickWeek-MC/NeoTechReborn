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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import techreborn.config.TechRebornConfig;
import techreborn.init.TRContent.Ingots;
import techreborn.init.TRContent.Parts;

/**
 * Port note: Fabric's LootTableEvents.MODIFY becomes NeoForge's {@link LootTableLoadEvent}, on the game bus.
 * <p>
 * Adding a whole pool is easy ({@code table.addPool}). Upstream's {@code modifyPools} (add one entry to the
 * pools that already exist) is not: LootPool keeps its entries private. Instead of an extra pool (that would
 * be an extra drop, not a different drop) the table is round-tripped through its own codec, the entry is
 * appended to the JSON of every pool, and the table is replaced with {@link LootTableLoadEvent#setTable}.
 * <p>
 * Upstream quirk kept on purpose: the first check only lets {@code minecraft:gameplay*} and
 * {@code minecraft:chests*} through, so the {@code minecraft:archeology/trail_ruins_common} branch of the
 * original never ran, and it does not run here either.
 */
public class ModLoot {
	private static final Logger LOGGER = LoggerFactory.getLogger("techreborn");

	public static void init() {
		NeoForge.EVENT_BUS.addListener(ModLoot::onLootTableLoad);
	}

	private static void onLootTableLoad(LootTableLoadEvent event) {
		String stringId = event.getName().toString();
		if (!stringId.startsWith("minecraft:gameplay") && !stringId.startsWith("minecraft:chests")) {
			return;
		}
		LootTable table = event.getTable();

		if (TechRebornConfig.enableOverworldLoot) {
			switch (stringId) {
				case "minecraft:chests/abandoned_mineshaft",
					"minecraft:chests/desert_pyramid",
					"minecraft:chests/igloo_chest",
					"minecraft:chests/jungle_temple",
					"minecraft:chests/simple_dungeon",
					"minecraft:chests/shipwreck_treasure",
					"minecraft:chests/underwater_ruin_small",
					"minecraft:chests/village/village_weaponsmith",
					"minecraft:chests/village/village_armorer",
					"minecraft:chests/village/village_toolsmith"
					-> table.addPool(makeBasicPool());
				case "minecraft:chests/stronghold_corridor",
					"minecraft:chests/stronghold_crossing",
					"minecraft:chests/stronghold_library",
					"minecraft:chests/underwater_ruin_big",
					"minecraft:chests/pillager_outpost"
					-> table.addPool(makeAdvancedPool());
				case "minecraft:chests/woodland_mansion",
					"minecraft:chests/ancient_city"
					-> table.addPool(makeIndustrialPool());
				case "minecraft:gameplay/cat_morning_gift"
					-> addEntryToEveryPool(event, entry(Parts.SCRAP, 5, false));
				default -> {
				}
			}
		}

		if (TechRebornConfig.enableNetherLoot) {
			if (stringId.equals("minecraft:chests/nether_bridge") ||
					stringId.equals("minecraft:chests/bastion_bridge") ||
					stringId.equals("minecraft:chests/bastion_hoglin_stable") ||
					stringId.equals("minecraft:chests/bastion_treasure") ||
					stringId.equals("minecraft:chests/bastion_other")) {
				table.addPool(makeAdvancedPool());
			}
		}

		if (TechRebornConfig.enableEndLoot) {
			if (stringId.equals("minecraft:chests/end_city_treasure")) {
				table.addPool(makeIndustrialPool());
			}
		}

		if (TechRebornConfig.enableFishingJunkLoot) {
			if (stringId.equals("minecraft:gameplay/fishing/junk")) {
				addEntryToEveryPool(event,
					entry(Parts.RUBBER, 10, false),
					entry(TRContent.TREE_TAP, 10, true),
					entry(Parts.SCRAP, 10, false));
			}
		}
	}

	private static LootPool makeBasicPool() {
		return LootPool.lootPool()
			.add(makeEntry(Items.COPPER_INGOT)).add(makeEntry(Ingots.TIN))
			.add(makeEntry(Ingots.LEAD)).add(makeEntry(Ingots.SILVER))
			.add(makeEntry(Ingots.REFINED_IRON)).add(makeEntry(Ingots.ADVANCED_ALLOY))
			.add(makeEntry(TRContent.MachineBlocks.BASIC.frame.asItem())).add(makeEntry(Parts.ELECTRONIC_CIRCUIT))
			.add(makeEntry(TRContent.RUBBER_SAPLING, 25))
			.setRolls(UniformGenerator.between(1.0f, 2.0f))
			.build();
	}

	private static LootPool makeAdvancedPool() {
		return LootPool.lootPool()
			.add(makeEntry(Ingots.ALUMINUM)).add(makeEntry(Ingots.ELECTRUM))
			.add(makeEntry(Ingots.INVAR)).add(makeEntry(Ingots.NICKEL))
			.add(makeEntry(Ingots.STEEL)).add(makeEntry(Ingots.ZINC))
			.add(makeEntry(TRContent.MachineBlocks.ADVANCED.frame.asItem())).add(makeEntry(Parts.ADVANCED_CIRCUIT))
			.add(makeEntry(Parts.DATA_STORAGE_CHIP))
			.setRolls(UniformGenerator.between(1.0f, 3.0f))
			.build();
	}

	private static LootPool makeIndustrialPool() {
		return LootPool.lootPool()
			.add(makeEntry(Ingots.CHROME)).add(makeEntry(Ingots.IRIDIUM))
			.add(makeEntry(Ingots.PLATINUM)).add(makeEntry(Ingots.TITANIUM))
			.add(makeEntry(Ingots.TUNGSTEN)).add(makeEntry(Ingots.TUNGSTENSTEEL))
			.add(makeEntry(TRContent.MachineBlocks.INDUSTRIAL.frame.asItem())).add(makeEntry(Parts.INDUSTRIAL_CIRCUIT))
			.add(makeEntry(Parts.ENERGY_FLOW_CHIP))
			.setRolls(UniformGenerator.between(1.0f, 3.0f))
			.build();
	}

	/**
	 * Makes loot entry from item provided
	 *
	 * @param item {@link ItemLike} Item to include into LootEntry
	 */
	private static LootPoolSingletonContainer.Builder<?> makeEntry(ItemLike item) {
		return makeEntry(item, 5);
	}

	/**
	 * Makes loot entry from item provided with weight provided
	 *
	 * @param item   {@link ItemLike} Item to include into LootEntry
	 * @param weight {@code int} Weight of that item
	 */
	private static LootPoolSingletonContainer.Builder<?> makeEntry(ItemLike item, int weight) {
		return LootItem.lootTableItem(item).setWeight(weight)
			.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f)));
	}

	/** One item entry, for {@link #addEntryToEveryPool}. */
	private static LootPoolEntryContainer entry(ItemLike item, int weight, boolean randomDamage) {
		LootPoolSingletonContainer.Builder<?> builder = LootItem.lootTableItem(item).setWeight(weight);
		if (randomDamage) {
			builder.apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.0f, 0.9f)));
		}
		return builder.build();
	}

	/**
	 * Upstream's {@code tableBuilder.modifyPools(pool -> pool.with(entry))}, see the class comment.
	 * <p>
	 * Done by swapping each pool's entry list through reflection. A first version round-tripped the table
	 * through its codec as JSON, but tables that name biomes (the fishing junk one does) cannot be
	 * re-encoded here: their holders belong to a registry lookup other than the one the event gives.
	 */
	private static void addEntryToEveryPool(LootTableLoadEvent event, LootPoolEntryContainer... entries) {
		try {
			Field poolsField = LootTable.class.getDeclaredField("pools");
			poolsField.setAccessible(true);
			Field entriesField = LootPool.class.getDeclaredField("entries");
			entriesField.setAccessible(true);

			@SuppressWarnings("unchecked")
			List<LootPool> pools = (List<LootPool>) poolsField.get(event.getTable());
			for (LootPool pool : pools) {
				@SuppressWarnings("unchecked")
				List<LootPoolEntryContainer> current = (List<LootPoolEntryContainer>) entriesField.get(pool);
				List<LootPoolEntryContainer> merged = new ArrayList<>(current);
				merged.addAll(List.of(entries));
				entriesField.set(pool, List.copyOf(merged));
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.error("Could not add TechReborn entries to loot table {}", event.getName(), e);
		}
	}
}
