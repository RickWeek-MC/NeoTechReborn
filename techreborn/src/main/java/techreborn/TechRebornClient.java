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

package techreborn;

import reborncore.client.multiblock.MultiblockRenderer;
import techreborn.init.TRBlockEntities;
import techreborn.client.render.entitys.CableCoverRenderer;
import techreborn.client.render.entitys.NukeRenderer;
import techreborn.init.TREntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import reborncore.client.gui.GuiBase;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.SimpleEnergyItem;
import techreborn.client.ClientGuiType;
import techreborn.client.events.ClientJumpHandler;
import techreborn.client.events.StackToolTipHandler;
import techreborn.client.keybindings.KeyBindings;
import techreborn.client.render.CellItemColor;
import techreborn.client.render.DynamicCellModel;
import techreborn.component.TRDataComponentTypes;
import techreborn.init.ModFluids;
import techreborn.init.TRContent;
import techreborn.items.BatteryItem;
import techreborn.items.DynamicCellItem;
import techreborn.items.FrequencyTransmitterItem;
import techreborn.items.armor.BatpackItem;
import techreborn.items.tool.ChainsawItem;
import techreborn.items.tool.industrial.NanosaberItem;

/**
 * Client-only entry point, the NeoForge counterpart of upstream's ClientModInitializer, built like
 * RebornCoreClient: a second {@code @Mod} class bound to {@link Dist#CLIENT}, so a dedicated server
 * never loads any of this.
 * <p>
 * Reduced to the content that exists. NOT here yet, and arriving with the later machine blocks: the
 * machine casing block state resolver, the block entity renderers (multiblock holograms, storage unit,
 * cables, wind mill), the nuke entity renderer and the elevator jump handler.
 * Le schermate delle macchine presenti stanno in {@link ClientGuiType}.
 */
@Mod(value = TechReborn.MOD_ID, dist = Dist.CLIENT)
public class TechRebornClient {

	public TechRebornClient(IEventBus modEventBus) {
		modEventBus.addListener(TechRebornClient::registerGeometryLoaders);
		modEventBus.addListener(TechRebornClient::registerItemColors);
		modEventBus.addListener(KeyBindings::registerKeys);
		modEventBus.addListener(TechRebornClient::clientSetup);
		modEventBus.addListener(TechRebornClient::registerRenderers);

		// Le schermate delle macchine: su NeoForge si registrano qui, non al caricamento classe.
		modEventBus.addListener((RegisterMenuScreensEvent event) -> ClientGuiType.registerScreens(event));

		// The tooltip event and the client tick live on the game bus, not the mod bus.
		NeoForge.EVENT_BUS.addListener(StackToolTipHandler::onTooltip);
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> KeyBindings.handlePresses());
		// blocco M8: il salto sopra un ascensore (evento di RebornCore, vedi ClientJumpHandler)
		NeoForge.EVENT_BUS.addListener(ClientJumpHandler::onJump);
	}

	private static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
		event.register(DynamicCellModel.LOADER_ID, DynamicCellModel.Loader.INSTANCE);
	}

	/** The fluid inside the cell is drawn with tint index 1 and coloured by {@link CellItemColor}. */
	private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register(new CellItemColor(), TRContent.CELL);
	}

	/** I renderer dei block entity. Per ora solo il rivestimento dei cavi. */
	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(TRBlockEntities.CABLE, CableCoverRenderer::new);

		// Gli ologrammi dei multiblocchi: la struttura da costruire, disegnata in trasparenza.
		event.registerBlockEntityRenderer(TRBlockEntities.INDUSTRIAL_BLAST_FURNACE, MultiblockRenderer::new);
		event.registerBlockEntityRenderer(TRBlockEntities.INDUSTRIAL_GRINDER, MultiblockRenderer::new);
		event.registerBlockEntityRenderer(TRBlockEntities.INDUSTRIAL_SAWMILL, MultiblockRenderer::new);
		event.registerBlockEntityRenderer(TRBlockEntities.IMPLOSION_COMPRESSOR, MultiblockRenderer::new);
		event.registerBlockEntityRenderer(TRBlockEntities.VACUUM_FREEZER, MultiblockRenderer::new);
		event.registerBlockEntityRenderer(TRBlockEntities.DISTILLATION_TOWER, MultiblockRenderer::new);
		event.registerBlockEntityRenderer(TRBlockEntities.FLUID_REPLICATOR, MultiblockRenderer::new);
		event.registerBlockEntityRenderer(TRBlockEntities.FUSION_CONTROL_COMPUTER, MultiblockRenderer::new);
		event.registerBlockEntityRenderer(TRBlockEntities.GREENHOUSE_CONTROLLER, MultiblockRenderer::new);

		// blocco M7: la bomba atomica innescata (il blocco che lampeggia, come il TNT del carrello).
		event.registerEntityRenderer(TREntities.NUKE.get(), NukeRenderer::new);
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			registerItemProperties();

			// The two icons of the machine GUI tabs, left open since block 23 of RebornCore: it cannot
			// name TechReborn's items itself.
			GuiBase.wrenchStack = new ItemStack(TRContent.WRENCH);
			GuiBase.fluidCellProvider = DynamicCellItem::getCellWithFluid;

			// Upstream (BlockRenderLayerMap) disegna i cavi con i bordi trasparenti: cutout.
			for (TRContent.Cables cable : TRContent.Cables.values()) {
				ItemBlockRenderTypes.setRenderLayer(cable.block, RenderType.cutout());
			}

			// blocco M7: lampade, allarme, vetro rinforzato e il verde della gomma vogliono il
			// cutout, le foglie il cutout mipped (upstream: BlockRenderLayerMap).
			ItemBlockRenderTypes.setRenderLayer(TRContent.Machine.LAMP_INCANDESCENT.block, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(TRContent.Machine.LAMP_LED.block, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(TRContent.Machine.ALARM.block, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(TRContent.Machine.RESIN_BASIN.block, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(TRContent.Machine.FISHING_STATION.block, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(TRContent.RUBBER_SAPLING, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(TRContent.POTTED_RUBBER_SAPLING, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(TRContent.REINFORCED_GLASS, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(TRContent.RUBBER_LEAVES, RenderType.cutoutMipped());

			// Upstream asks for translucent fluids; without this they are drawn opaque.
			for (ModFluids fluid : ModFluids.values()) {
				ItemBlockRenderTypes.setRenderLayer(fluid.getFluid(), RenderType.translucent());
				ItemBlockRenderTypes.setRenderLayer(fluid.getFlowingFluid(), RenderType.translucent());
			}

			StackToolTipHandler.setup();
		});
	}

	/**
	 * The model overrides that are already in the item JSONs copied from upstream: an empty battery or
	 * batpack, a frequency transmitter holding coordinates, a running chainsaw, an active nanosaber.
	 * Without these the JSONs' "overrides" never fire and every one of them keeps its base texture.
	 * <p>
	 * Port note: Fabric's ModelPredicateProviderRegistry becomes ItemProperties, and the provider gets
	 * the stack but not the item, so where upstream read a value off the item (the chainsaw's cost) the
	 * item is taken from the stack.
	 */
	private static void registerItemProperties() {
		ClampedItemPropertyFunction empty = (stack, level, entity, seed) ->
			!stack.isEmpty() && SimpleEnergyItem.getStoredEnergyUnchecked(stack) == 0 ? 1.0F : 0.0F;

		registerProperty(BatteryItem.class, "empty", empty);
		registerProperty(BatpackItem.class, "empty", empty);

		registerProperty(FrequencyTransmitterItem.class, "coords", (stack, level, entity, seed) -> {
			GlobalPos globalPos = stack.get(TRDataComponentTypes.FREQUENCY_TRANSMITTER);
			return globalPos != null ? 1.0F : 0.0F;
		});

		registerProperty(ChainsawItem.class, "animated", (stack, level, entity, seed) -> {
			if (stack.isEmpty() || entity == null || !(stack.getItem() instanceof ChainsawItem chainsaw)) {
				return 0.0F;
			}

			boolean inMainHand = entity.getMainHandItem() == stack;
			return inMainHand && SimpleEnergyItem.getStoredEnergyUnchecked(stack) >= chainsaw.getCost() ? 1.0F : 0.0F;
		});

		registerProperty(NanosaberItem.class, "active", (stack, level, entity, seed) -> {
			if (stack.get(TRDataComponentTypes.IS_ACTIVE) != Boolean.TRUE) {
				return 0.0F;
			}

			RcEnergyItem energyItem = (RcEnergyItem) stack.getItem();
			// Nearly empty: upstream shows the "low" texture.
			if (energyItem.getEnergyCapacity(stack) - energyItem.getStoredEnergy(stack) >= 0.9 * energyItem.getEnergyCapacity(stack)) {
				return 0.5F;
			}

			return 1.0F;
		});
	}

	private static <T extends Item> void registerProperty(Class<T> itemClass, String name, ClampedItemPropertyFunction property) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, name);
		BuiltInRegistries.ITEM.stream()
			.filter(itemClass::isInstance)
			.forEach(item -> ItemProperties.register(item, id, property));
	}
}
