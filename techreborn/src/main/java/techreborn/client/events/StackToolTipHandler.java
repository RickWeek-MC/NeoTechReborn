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

package techreborn.client.events;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import reborncore.common.BaseBlockEntityProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import reborncore.api.IListInfoProvider;
import techreborn.blocks.cable.CableBlock;
import techreborn.init.TRContent;
import techreborn.items.DynamicCellItem;
import techreborn.items.UpgradeItem;
import techreborn.events.OreDepthSyncHandler;
import techreborn.world.OreDepth;
import techreborn.world.TargetDimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The extra tooltip lines on TechReborn's own items.
 * <p>
 * Port note: Fabric's ItemTooltipCallback becomes NeoForge's ItemTooltipEvent on the game bus.
 * <p>
 * The machine info lines work for any block with a block entity, so they show up by themselves as the
 * machines arrive. Still NOT ported: the armour tooltips
 * (NanoSuitItem/QuantumSuitItem.appendArmorTooltip, left out since block T5).
 */
public class StackToolTipHandler {

	/** Whether an item belongs to this mod; worked out once per item, as upstream did. */
	private static final Map<Item, Boolean> IS_TR_ITEM = new HashMap<>();

	/** Deepslate ores of a dimension where no deepslate generates: they cannot be mined anywhere. */
	private static final List<Block> UNOBTAINABLE_ORES = new ArrayList<>();

	public static void setup() {
		for (TRContent.Ores ore : TRContent.Ores.values()) {
			if (!ore.isDeepslate()) {
				continue;
			}

			TRContent.Ores normal = ore.getUnDeepslate();
			if (normal.distribution != null && normal.distribution.dimension != TargetDimension.OVERWORLD) {
				UNOBTAINABLE_ORES.add(ore.block);
			}
		}
	}

	public static void onTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		Item item = stack.getItem();
		List<Component> lines = event.getToolTip();

		// Upstream's note, still true: a recipe viewer may search on another thread.
		if (!Minecraft.getInstance().isSameThread()) {
			return;
		}

		if (!IS_TR_ITEM.computeIfAbsent(item, StackToolTipHandler::isTRItem)) {
			return;
		}

		Block block = Block.byItem(item);

		if (block instanceof BaseBlockEntityProvider) {
			ToolTipAssistUtils.addInfo(item.getDescriptionId(), lines);
		}

		if (block instanceof CableBlock cable) {
			BlockEntity blockEntity = cable.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
			if (blockEntity != null) {
				((IListInfoProvider) blockEntity).addInfo(lines, false, false);
			}
		}

		// blocco M7: le statistiche dei potenziamenti (erano un rimando di M4).
		if (item instanceof UpgradeItem upgrade) {
			ToolTipAssistUtils.addInfo(item.getDescriptionId(), lines, false);
			lines.addAll(ToolTipAssistUtils.getUpgradeStats(TRContent.Upgrades.valueOf(upgrade.name.toUpperCase(Locale.ROOT)), stack.getCount(), Screen.hasShiftDown()));
		}

		if (item == TRContent.Upgrades.SUPERCONDUCTOR.item && Screen.hasControlDown()) {
			lines.add(Component.literal(ChatFormatting.GOLD + "Blame obstinate_3 for this"));
		}

		if (item instanceof DynamicCellItem cell) {
			// A fluid that cannot be placed as a block: worth saying, you cannot empty the cell into the world.
			Fluid fluid = cell.getFluid(stack);
			if (!(fluid instanceof FlowingFluid) && fluid != Fluids.EMPTY) {
				ToolTipAssistUtils.addInfo("unplaceable_fluid", lines, false);
			}
		}

		if (item == TRContent.OMNI_TOOL) {
			lines.add(Component.literal(ChatFormatting.YELLOW + I18n.get("techreborn.tooltip.omnitool_motto")));
		}

		if (block == TRContent.Machine.INDUSTRIAL_CENTRIFUGE.block && Screen.hasControlDown()) {
			lines.add(Component.literal("Round and round it goes"));
		}

		if (UNOBTAINABLE_ORES.contains(block)) {
			lines.add(Component.translatable("techreborn.tooltip.unobtainable").withStyle(ChatFormatting.AQUA));
		} else if (OreDepthSyncHandler.getOreDepthMap().containsKey(block)) {
			// blocco M7: a che altezza si trova questo minerale. La mappa arriva dal server in
			// fase di configurazione (OreDepthSyncHandler), quindi in singolo e' sempre pronta.
			OreDepth oreDepth = OreDepthSyncHandler.getOreDepthMap().get(block);
			lines.add(getOreDepthText(oreDepth).copy().withStyle(ChatFormatting.AQUA));
		}
	}

	private static Component getOreDepthText(OreDepth depth) {
		return Component.translatable("techreborn.tooltip.ores.%s".formatted(depth.dimension().name().toLowerCase(Locale.ROOT)),
				Component.literal(String.valueOf(depth.minY())).withStyle(ChatFormatting.YELLOW),
				Component.literal(String.valueOf(depth.maxY())).withStyle(ChatFormatting.YELLOW)
		);
	}

	private static boolean isTRItem(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("techreborn");
	}
}
