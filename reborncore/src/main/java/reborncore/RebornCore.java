/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 TeamReborn
 *
 * NeoForge port (c) 2026 RickWeek, based on the upstream Fabric sources.
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

package reborncore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reborncore.api.ToolManager;
import reborncore.api.items.ArmorRemoveHandler;
import reborncore.common.RebornCoreCommands;
import reborncore.common.RebornCoreConfig;
import reborncore.common.RebornCoreEvents;
import reborncore.common.blocks.BlockWrenchEventHandler;
import reborncore.common.config.Configuration;
import reborncore.common.misc.ModSounds;
import reborncore.common.misc.RebornCoreTags;
import reborncore.common.powerSystem.RebornCapabilities;
import reborncore.common.powerSystem.RebornEnergyComponents;
import reborncore.common.crafting.RecipeManager;
import reborncore.common.fluid.RebornFluidManager;
import reborncore.common.network.Packets;
import reborncore.common.recipes.PaddedShapedRecipe;
import reborncore.common.util.CalenderUtils;
import reborncore.common.util.GenericWrenchHelper;

import java.util.Locale;

/**
 * Entry point of Neo Reborn Core (NeoForge port of Reborn Core).
 * <p>
 * Upstream {@code onInitialize()} is being ported step by step, see PORTING_NOTES.md.
 */
@Mod(RebornCore.MOD_ID)
public class RebornCore {

	public static final String MOD_ID = "reborncore";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Locale locale = Locale.ROOT;

	public RebornCore(IEventBus modEventBus) {
		new Configuration(RebornCoreConfig.class, MOD_ID);
		CalenderUtils.loadCalender(); // Done early as some features need this

		// Deferred registration has to be hooked up before anything registers into it
		RebornRegistry.init(modEventBus);
		ModSounds.register(modEventBus);
		PaddedShapedRecipe.register(modEventBus);
		RecipeManager.init(modEventBus);
		RebornFluidManager.init(modEventBus);
		modEventBus.addListener(Packets::register);
		RebornEnergyComponents.register(modEventBus);

		// Items carrying energy get their capability wired up automatically; machines cannot be
		// discovered this way, see RebornCapabilities.
		modEventBus.addListener(RebornCapabilities::registerItems);

		registerWrenchHandlers();

		BlockWrenchEventHandler.setup();

		NeoForge.EVENT_BUS.addListener(RebornCore::onEquipmentChange);
		RebornCoreEvents.register();
		RebornCoreCommands.setup();

		//noinspection ResultOfMethodCallIgnored
		RebornCoreTags.WATER_EXPLOSION_ITEM.toString();

		// Machine capabilities are per BlockEntityType, so the mod that owns them calls
		// RebornCapabilities.registerMachines from its own RegisterCapabilitiesEvent listener.

		LOGGER.info("Neo Reborn Core loading");
	}

	private void registerWrenchHandlers() {
		addWrench("intergrateddynamics:wrench");
		addWrench("thermal:wrench");
		addWrench("rftoolsbase:smartwrench");
		addWrench("redstone_arsenal:flux_wrench");

		addWrench("ad_astra:wrench");
		addWrench("ae2:certus_quartz_wrench");
		addWrench("ae2:nether_quartz_wrench");
		addWrench("bitsandchisels:wrench");
		addWrench("create:wrench");
		addWrench("indrev:wrench");
		addWrench("modern_industialization:wrench");
	}

	private void addWrench(String id) {
		ToolManager.INSTANCE.customToolHandlerList.add(new GenericWrenchHelper(ResourceLocation.parse(id), false));
	}

	/**
	 * Port note: replaces Fabric's ServerEntityEvents.EQUIPMENT_CHANGE.
	 */
	private static void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (event.getEntity() instanceof Player playerEntity
			&& event.getFrom().getItem() instanceof ArmorRemoveHandler armorRemoveHandler
			&& !ItemStack.isSameItem(event.getFrom(), event.getTo())) {
			armorRemoveHandler.onRemoved(playerEntity);
		}
	}

	/**
	 * Replacement for the upstream FabricLoader environment type: the physical side we run on.
	 */
	public static Dist getSide() {
		return FMLEnvironment.dist;
	}
}
