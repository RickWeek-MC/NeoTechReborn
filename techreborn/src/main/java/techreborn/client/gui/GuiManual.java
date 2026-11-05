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

package techreborn.client.gui;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import reborncore.client.gui.Theme;
import reborncore.client.gui.ThemeManager;
import techreborn.config.TechRebornConfig;
import techreborn.packets.serverbound.RefundPayload;

/**
 * The manual: a page with a link to the wiki, one to the Discord and, if the config allows it, a button
 * that refunds what the manual was crafted from.
 * <p>
 * Port note: DrawContext -> GuiGraphics, ButtonWidget -> Button, ClientPlayNetworking.send ->
 * PacketDistributor.sendToServer, Util.getOperatingSystem().open -> Util.getPlatform().openUri, and
 * the theme colour is argb() here (ThemeColor is a record of one int).
 */
public class GuiManual extends Screen {

	private static final ResourceLocation MANUAL_TEXTURE = ResourceLocation.fromNamespaceAndPath("techreborn", "textures/gui/manual.png");
	private static final Component TEXT_WIKI = Component.translatable("techreborn.manual.wiki");
	private static final Component TEXT_DISCORD = Component.translatable("techreborn.manual.discord");
	private static final Component TEXT_REFUND = Component.translatable("techreborn.manual.refund");

	private final int guiWidth = 207;
	private final int guiHeight = 195;
	private final Theme theme;

	public GuiManual() {
		super(Component.literal("gui.manual"));
		this.theme = ThemeManager.getTheme();
	}

	@Override
	public void init() {
		super.init();
		int y = (height / 2) - guiHeight / 2;

		addRenderableWidget(Button.builder(Component.translatable("techreborn.manual.wikibtn"),
				button -> openLink("https://wiki.techreborn.ovh"))
			.bounds(width / 2 - 30, y + 60, 60, 20).build());

		addRenderableWidget(Button.builder(Component.translatable("techreborn.manual.discordbtn"),
				button -> openLink("https://discord.gg/teamreborn"))
			.bounds(width / 2 - 30, y + 110, 60, 20).build());

		if (TechRebornConfig.allowManualRefund) {
			addRenderableWidget(Button.builder(Component.translatable("techreborn.manual.refundbtn"), button -> {
					PacketDistributor.sendToServer(new RefundPayload());
					onClose();
				})
				.bounds(width / 2 - 30, y + 160, 60, 20).build());
		}
	}

	private void openLink(String url) {
		if (minecraft == null) {
			return;
		}

		minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
			if (confirmed) {
				Util.getPlatform().openUri(url);
			}
			minecraft.setScreen(this);
		}, url, false));
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		int centerX = (width / 2) - guiWidth / 2;
		int centerY = (height / 2) - guiHeight / 2;
		guiGraphics.blit(MANUAL_TEXTURE, centerX, centerY, 0, 0, guiWidth, guiHeight);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		int centerY = (height / 2) - guiHeight / 2;
		int colour = theme.titleColor().argb();

		guiGraphics.drawString(font, TEXT_WIKI, (width / 2) - font.width(TEXT_WIKI) / 2, centerY + 40, colour, false);
		guiGraphics.drawString(font, TEXT_DISCORD, (width / 2) - font.width(TEXT_DISCORD) / 2, centerY + 90, colour, false);
		if (TechRebornConfig.allowManualRefund) {
			guiGraphics.drawString(font, TEXT_REFUND, (width / 2) - font.width(TEXT_REFUND) / 2, centerY + 140, colour, false);
		}
	}
}
