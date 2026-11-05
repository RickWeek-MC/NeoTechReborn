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

package reborncore.client.gui.config.elements;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.network.PacketDistributor;
import reborncore.RebornCore;
import reborncore.client.gui.GuiBase;
import reborncore.client.gui.GuiSprites;
import reborncore.client.gui.ThemeColor;
import reborncore.common.blockentity.FluidConfiguration;
import reborncore.common.network.serverbound.FluidConfigSavePayload;
import reborncore.common.network.serverbound.FluidIoSavePayload;

import java.util.Arrays;

public class FluidConfigPopupElement extends AbstractConfigPopupElement {
	ConfigFluidElement fluidElement;

	public FluidConfigPopupElement(int x, int y, int height, ConfigFluidElement fluidElement) {
		super(
			x, y, height, GuiSprites.SLOT_CONFIG_POPUP, 62, 62,
			Arrays.stream(FluidConfiguration.ExtractConfig.values()).map(Enum::name).toArray(String[]::new)
		);
		this.fluidElement = fluidElement;
	}

	@Override
	public int getPencilColor(String pencil) {
		return switch (pencil) {
			case "INPUT" -> theme.ioInputColor().argb();
			case "OUTPUT" -> theme.ioOutputColor().argb();
			case "ALL" -> theme.ioBothColor().argb();
			default -> 0x80000000 | theme.warningTextColor().argb();
		};
	}

	@Override
	protected void cycleConfig(Direction side, GuiBase<?> guiBase) {
		FluidConfiguration.ExtractConfig fluidIO;
		if (pencil != null) {
			fluidIO = switch (pencil) {
				case "INPUT" -> FluidConfiguration.ExtractConfig.INPUT;
				case "OUTPUT" -> FluidConfiguration.ExtractConfig.OUTPUT;
				case "ALL" -> FluidConfiguration.ExtractConfig.ALL;
				default -> FluidConfiguration.ExtractConfig.NONE;
			};
		} else {
			FluidConfiguration.FluidConfig config = guiBase.getMachine().fluidConfiguration.getSideDetail(side);

			fluidIO = config.getIoConfig().getNext();
		}
		FluidConfiguration.FluidConfig newConfig = new FluidConfiguration.FluidConfig(side, fluidIO);

		PacketDistributor.sendToServer(new FluidConfigSavePayload(guiBase.be.getBlockPos(), newConfig));
	}

	public void updateCheckBox(String type, GuiBase<?> guiBase) {
		FluidConfiguration configHolder = guiBase.getMachine().fluidConfiguration;
		boolean input = configHolder.autoInput();
		boolean output = configHolder.autoOutput();
		if (type.equalsIgnoreCase("input")) {
			input = !configHolder.autoInput();
		}
		if (type.equalsIgnoreCase("output")) {
			output = !configHolder.autoOutput();
		}

		PacketDistributor.sendToServer(new FluidIoSavePayload(guiBase.be.getBlockPos(), input, output));
	}

	@Override
	protected void drawSateColor(GuiGraphics guiGraphics, GuiBase<?> gui, Direction side, int inx, int iny) {
		iny += 4;
		int sx = inx + getX() + gui.getGuiLeft();
		int sy = iny + getY() + gui.getGuiTop();
		FluidConfiguration fluidConfiguration = gui.getMachine().fluidConfiguration;
		if (fluidConfiguration == null) {
			RebornCore.LOGGER.debug("Hmm, this isn't supposed to happen");
			return;
		}
		FluidConfiguration.FluidConfig fluidConfig = fluidConfiguration.getSideDetail(side);
		ThemeColor color = switch (fluidConfig.getIoConfig()) {
			case NONE -> ThemeColor.TRANSPARENT;
			case INPUT -> theme.ioInputColor();
			case OUTPUT -> theme.ioOutputColor();
			case ALL -> theme.ioBothColor();
		};
		guiGraphics.fill(sx, sy, sx + 18, sy + 18, color.argb());
	}
}
