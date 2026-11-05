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

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import reborncore.common.util.TradeUtils;
import techreborn.TechReborn;
import techreborn.config.TechRebornConfig;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Port note, three Fabric APIs replaced:
 * <ul>
 *   <li>{@code PointOfInterestHelper} / {@code VillagerProfessionBuilder}: plain DeferredRegisters of PoiType and
 *   VillagerProfession (so the professions only exist after registration, hence the DeferredHolders);</li>
 *   <li>{@code TradeOfferHelper}: RebornCore's TradeUtils already listens to NeoForge's VillagerTradesEvent, the
 *   wandering trader has its own event ({@link WandererTradesEvent});</li>
 *   <li>{@code DynamicRegistrySetupCallback.registerEntryAdded} on the template pools: NeoForge has no event for
 *   that, so the houses are added to the pools once the server has loaded its registries
 *   ({@link ServerAboutToStartEvent}). The list lives in a private field of StructureTemplatePool; it is
 *   reached by reflection, and if that ever fails the houses are skipped with an error in the log.</li>
 * </ul>
 * {@link #register(IEventBus)} goes in the mod constructor; {@link #registerVillagerTrades()} and the two event
 * hooks in FMLCommonSetupEvent (they need the registered professions).
 */
public class TRVillager {

	public static final ResourceLocation METALLURGIST_ID = ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, "metallurgist");
	public static final ResourceLocation ELECTRICIAN_ID = ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, "electrician");

	private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, TechReborn.MOD_ID);
	private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, TechReborn.MOD_ID);

	private static final ResourceKey<PoiType> METALLURGIST_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, METALLURGIST_ID);
	private static final ResourceKey<PoiType> ELECTRICIAN_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, ELECTRICIAN_ID);

	public static final DeferredHolder<PoiType, PoiType> METALLURGIST_POI = POI_TYPES.register("metallurgist",
		() -> new PoiType(allStates(TRContent.Machine.IRON_ALLOY_FURNACE.block), 1, 1));
	public static final DeferredHolder<PoiType, PoiType> ELECTRICIAN_POI = POI_TYPES.register("electrician",
		() -> new PoiType(allStates(TRContent.Machine.SOLID_FUEL_GENERATOR.block), 1, 1));

	public static final DeferredHolder<VillagerProfession, VillagerProfession> METALLURGIST_PROFESSION = PROFESSIONS.register("metallurgist",
		() -> new VillagerProfession(METALLURGIST_ID.toString(),
			poi -> poi.is(METALLURGIST_POI_KEY), poi -> poi.is(METALLURGIST_POI_KEY),
			ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_TOOLSMITH));

	public static final DeferredHolder<VillagerProfession, VillagerProfession> ELECTRICIAN_PROFESSION = PROFESSIONS.register("electrician",
		() -> new VillagerProfession(ELECTRICIAN_ID.toString(),
			poi -> poi.is(ELECTRICIAN_POI_KEY), poi -> poi.is(ELECTRICIAN_POI_KEY),
			ImmutableSet.of(), ImmutableSet.of(), ModSounds.CABLE_SHOCK.get()));

	private TRVillager() {/* No instantiation. */}

	/** Same as Fabric's PointOfInterestHelper: every state of the block counts as the workstation. */
	private static java.util.Set<net.minecraft.world.level.block.state.BlockState> allStates(Block block) {
		return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
	}

	public static void register(IEventBus modEventBus) {
		POI_TYPES.register(modEventBus);
		PROFESSIONS.register(modEventBus);
	}

	/** Game-bus hooks and the trades; call from FMLCommonSetupEvent. */
	public static void setup() {
		registerVillagerTrades();
		NeoForge.EVENT_BUS.addListener(TRVillager::onWandererTrades);
		NeoForge.EVENT_BUS.addListener(TRVillager::onServerAboutToStart);
	}

	public static void registerVillagerTrades() {
		VillagerProfession metallurgist = METALLURGIST_PROFESSION.get();
		VillagerProfession electrician = ELECTRICIAN_PROFESSION.get();
		// metallurgist
		TradeUtils.registerTradesForLevel(metallurgist, TradeUtils.Level.NOVICE, false,
			TradeUtils.createBuy(TRContent.RawMetals.TIN, 1, 6, 12, 2),
			TradeUtils.createBuy(TRContent.RawMetals.LEAD, 1, 4, 12, 2),
			TradeUtils.createSell(TRContent.Parts.RUBBER, 2, 3, 16, 2)
		);
		TradeUtils.registerTradesForLevel(metallurgist, TradeUtils.Level.APPRENTICE, false,
			TradeUtils.createSell(TRContent.Ingots.BRONZE, 2, 1, 12, 10),
			TradeUtils.createSell(TRContent.Ingots.BRASS, 5, 1, 12, 10),
			TradeUtils.createSell(TRContent.Parts.ELECTRONIC_CIRCUIT, 3, 2, 12, 10)
		);
		TradeUtils.registerTradesForLevel(metallurgist, TradeUtils.Level.JOURNEYMAN, false,
			TradeUtils.createSell(TRContent.Ingots.ELECTRUM, 7, 3, 12, 20),
			TradeUtils.createBuy(TRContent.Parts.CARBON_FIBER, 1, 3, 12, 20)
		);
		TradeUtils.registerTradesForLevel(metallurgist, TradeUtils.Level.EXPERT, false,
			TradeUtils.createSell(TRContent.Ingots.ADVANCED_ALLOY, 7, 4, 12, 20),
			TradeUtils.createBuy(TRContent.Ingots.NICKEL, 1, 1, 12, 30)
		);
		TradeUtils.registerTradesForLevel(metallurgist, TradeUtils.Level.MASTER, false,
			TradeUtils.createSell(TRContent.Parts.ADVANCED_CIRCUIT, 7, 3, 12, 30)
		);
		// electrician
		TradeUtils.registerTradesForLevel(electrician, TradeUtils.Level.NOVICE, false,
			TradeUtils.createBuy(TRContent.Parts.RUBBER, 1, 6, 12, 2),
			TradeUtils.createBuy(Items.COPPER_INGOT, 1, 3, 12, 2),
			TradeUtils.createSell(TRContent.Cables.INSULATED_COPPER, 1, 3, 12, 2)
		);
		TradeUtils.registerTradesForLevel(electrician, TradeUtils.Level.APPRENTICE, false,
			TradeUtils.createBuy(Items.GOLD_INGOT, 1, 4, 12, 10),
			TradeUtils.createSell(TRContent.Cables.INSULATED_GOLD, 5, 3, 12, 10),
			TradeUtils.createSell(TRContent.Parts.ELECTRONIC_CIRCUIT, 3, 2, 12, 10)
		);
		TradeUtils.registerTradesForLevel(electrician, TradeUtils.Level.JOURNEYMAN, false,
			TradeUtils.createBuy(TRContent.RED_CELL_BATTERY, 1, 1, 12, 20),
			TradeUtils.createSell(TRContent.Machine.LOW_VOLTAGE_SU, 8, 1, 12, 20),
			TradeUtils.createSell(TRContent.Machine.SOLID_FUEL_GENERATOR, 8, 1, 12, 20)
		);
		TradeUtils.registerTradesForLevel(electrician, TradeUtils.Level.EXPERT, false,
			TradeUtils.createSell(TRContent.Parts.ADVANCED_CIRCUIT, 7, 3, 12, 20),
			TradeUtils.createBuy(TRContent.Gems.RUBY, 1, 6, 12, 20),
			TradeUtils.createSell(TRContent.Cables.GLASSFIBER, 4, 1, 8, 30)
		);
		TradeUtils.registerTradesForLevel(electrician, TradeUtils.Level.MASTER, false,
			TradeUtils.createSell(TRContent.Machine.LAMP_LED, 8, 1, 12, 20),
			TradeUtils.createSell(TRContent.LITHIUM_ION_BATTERY, 30, 1, 8, 30)
		);
	}

	/** Fabric's level 1 list is the common one ({@code getGenericTrades}), level 2 the rare one. */
	private static void onWandererTrades(WandererTradesEvent event) {
		event.getGenericTrades().add(TradeUtils.createSell(TRContent.RUBBER_SAPLING, 5, 1, 8, 1));
	}

	private static void onServerAboutToStart(ServerAboutToStartEvent event) {
		var pools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
		final String[] types = new String[] {"desert", "plains", "savanna", "snowy", "taiga"};
		for (String type : types) {
			StructureTemplatePool pool = pools.get(ResourceLocation.withDefaultNamespace("village/" + type + "/houses"));
			if (pool == null) {
				continue;
			}
			if (TechRebornConfig.enableMetallurgistGeneration) {
				addElement(pool, TechReborn.MOD_ID + ":village/" + type + "/houses/" + type + "_metallurgist");
			}
			if (TechRebornConfig.enableElectricianGeneration) {
				addElement(pool, TechReborn.MOD_ID + ":village/" + type + "/houses/" + type + "_electrician");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void addElement(StructureTemplatePool pool, String templateId) {
		try {
			// "templates" is the list the pool draws from (upstream's Yarn name was "elements").
			Field field = StructureTemplatePool.class.getDeclaredField("templates");
			field.setAccessible(true);
			List<StructurePoolElement> templates = (List<StructurePoolElement>) field.get(pool);
			templates.add(StructurePoolElement.single(templateId).apply(StructureTemplatePool.Projection.RIGID));
		} catch (ReflectiveOperationException | RuntimeException e) {
			TechReborn.LOGGER.error("Could not add {} to a village house pool", templateId, e);
		}
	}
}
