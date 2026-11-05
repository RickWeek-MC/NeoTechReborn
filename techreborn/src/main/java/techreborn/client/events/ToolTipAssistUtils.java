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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import reborncore.common.blockentity.MachineBaseBlockEntity;
import techreborn.config.TechRebornConfig;
import techreborn.init.TRContent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The extra lines TechReborn puts in tooltips.
 * <p>
 * Port note: {@code getUpgradeStats} arrived with block M7 (the upgrades themselves came with M4, and
 * the note here saying otherwise was stale). Shift shows the figures for the whole stack, capped at
 * four upgrades, exactly as upstream.
 */
public class ToolTipAssistUtils {

	private static final ChatFormatting INSTRUCT_COLOUR = ChatFormatting.BLUE;
	private static final ChatFormatting INFO_COLOUR = ChatFormatting.GOLD;
	private static final ChatFormatting STAT_COLOUR = ChatFormatting.GOLD;
	private static final ChatFormatting POS_COLOUR = ChatFormatting.GREEN;
	private static final ChatFormatting NEG_COLOUR = ChatFormatting.RED;

	/** What one upgrade (or the whole stack, with shift) does to the machine. */
	public static List<Component> getUpgradeStats(TRContent.Upgrades upgradeType, int count, boolean shiftHeld) {
		List<Component> tips = new ArrayList<>();
		boolean shouldStackCalculate = count > 1;

		switch (upgradeType) {
			case OVERCLOCKER -> {
				tips.add(getStatStringUnit(I18n.get("techreborn.tooltip.upgrade.speed_increase"), calculateSpeed(TechRebornConfig.overclockerSpeed * 100, count, shiftHeld), "%", true));
				tips.add(getStatStringUnit(I18n.get("techreborn.tooltip.upgrade.energy_increase"), calculateEnergyIncrease(TechRebornConfig.overclockerPower + 1, count, shiftHeld), "x", false));
			}
			case TRANSFORMER -> shouldStackCalculate = false;
			case ENERGY_STORAGE -> tips.add(getStatStringUnit(I18n.get("techreborn.tooltip.upgrade.storage_increase"), calculateValue(TechRebornConfig.energyStoragePower, count, shiftHeld), " E", true));
			case SUPERCONDUCTOR -> tips.add(getStatStringUnit(I18n.get("techreborn.tooltip.upgrade.flow_increase"), calculateValue(Math.pow(2, (TechRebornConfig.superConductorCount + 2)) * 100, count, shiftHeld), "%", true));
		}

		// Add reminder that they can use shift to calculate the entire stack
		if (shouldStackCalculate && !shiftHeld) {
			tips.add(Component.literal(INSTRUCT_COLOUR + I18n.get("techreborn.tooltip.stack_info")));
		}

		return tips;
	}

	public static void addInfo(String inKey, List<Component> list) {
		addInfo(inKey, list, true);
	}

	/**
	 * Adds the "techreborn.message.info.&lt;key&gt;" text, if the language file has it. When hidden it only
	 * shows with shift held, and otherwise says so.
	 */
	public static void addInfo(String inKey, List<Component> list, boolean hidden) {
		String key = "techreborn.message.info." + inKey;

		if (!I18n.exists(key)) {
			return;
		}

		if (!hidden || Screen.hasShiftDown()) {
			List<MutableComponent> infoLines = Arrays.stream(I18n.get(key).split("\\r?\\n"))
				.map(infoLine -> Component.literal(INFO_COLOUR + infoLine))
				.toList();
			list.addAll(1, infoLines);
		} else {
			list.add(Component.literal(INSTRUCT_COLOUR + I18n.get("techreborn.tooltip.more_info")));
		}
	}

	private static int calculateValue(double value, int count, boolean shiftHeld) {
		if (shiftHeld) {
			return (int) value * Math.min(count, 4);
		}
		return (int) value;
	}

	private static double calculateEnergyIncrease(double value, int count, boolean shiftHeld) {
		if (shiftHeld) {
			return Math.pow(value, Math.min(count, 4));
		}
		return value;
	}

	private static double calculateSpeed(double value, int count, boolean shiftHeld) {
		if (shiftHeld) {
			return Math.min(value * Math.min(count, 4), MachineBaseBlockEntity.SPEED_CAP * 100);
		}
		return value;
	}

	private static Component getStatStringUnit(String text, double value, String unit, boolean isPositive) {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US); // Always use dot
		NumberFormat formatter = new DecimalFormat("##.##", symbols); // Round to 2 decimal places
		return Component.literal(STAT_COLOUR + text + ": " + (isPositive ? POS_COLOUR : NEG_COLOUR) + formatter.format(value) + unit);
	}
}
