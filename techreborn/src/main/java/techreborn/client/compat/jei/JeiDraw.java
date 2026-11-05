package techreborn.client.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import reborncore.client.gui.GuiBuilder;
import reborncore.client.gui.GuiSprites;

import java.text.DecimalFormat;

/**
 * Drawing helpers for the JEI categories. They reuse RebornCore's own GUI sprites so the recipes look
 * like the machine screens (progress arrows, power bar, tank frame), as the REI plugin did upstream.
 */
final class JeiDraw {
	private static final DecimalFormat SECONDS = new DecimalFormat("###.##");

	private JeiDraw() {
	}

	/** Progress arrow: the empty sprite, then the filled one growing over {@code durationMs}. */
	static void progressBar(GuiGraphics graphics, int x, int y, double durationMs, GuiBuilder.ProgressDirection direction) {
		GuiSprites.drawSpriteStretched(graphics, direction.baseSprite, x, y, direction.width, direction.height);
		int j = (int) ((System.currentTimeMillis() / durationMs) % 1.0 * 16.0);
		if (j < 0) {
			j = 0;
		}
		int w = direction.width;
		int h = direction.height;
		switch (direction) {
			case RIGHT -> graphics.blitSprite(direction.overlaySprite, w, h, 0, 0, x, y, Math.min(j, w), h);
			case LEFT -> {
				int k = Math.min(j, w);
				graphics.blitSprite(direction.overlaySprite, w, h, w - k, 0, x + w - k, y, k, h);
			}
			case UP -> {
				int k = Math.min(j, h);
				graphics.blitSprite(direction.overlaySprite, w, h, 0, h - k, x, y + h - k, w, k);
			}
			case DOWN -> graphics.blitSprite(direction.overlaySprite, w, h, 0, 0, x, y, w, Math.min(j, h));
		}
	}

	/** Power bar, 14x50, with the red charge filling up (or down) in a loop. {@code x}/{@code y} is the inner top-left corner. */
	static void energyBar(GuiGraphics graphics, int x, int y, boolean downwards) {
		GuiSprites.drawSpriteStretched(graphics, GuiSprites.POWER_BAR_BASE, x - 1, y - 1, 14, 50);
		int inner = 48;
		int shown = animatedHeight(inner, 5000, downwards);
		if (shown > 0) {
			// Draw the bottom part of the 12x48 charge sprite; stretching it, as before, left the bar empty.
			graphics.blitSprite(GuiSprites.POWER_BAR_OVERLAY, 12, inner, 0, inner - shown, x, y + inner - shown, 12, shown);
		}
	}

	static void tankBackground(GuiGraphics graphics, int x, int y) {
		GuiSprites.drawSpriteStretched(graphics, GuiSprites.TANK_BACKGROUND, x - 4, y - 4, 22, 56);
	}

	static void tankForeground(GuiGraphics graphics, int x, int y) {
		GuiSprites.drawSpriteStretched(graphics, GuiSprites.TANK_FOREGROUND, x - 1, y - 1, 16, 50);
	}

	static int animatedHeight(int total, int durationMs, boolean downwards) {
		long step = Math.max(1, Math.round(durationMs * 1000.0 / total));
		int h = (int) Math.ceil(System.currentTimeMillis() / (step / 1000.0) % total);
		return downwards ? total - h : h;
	}

	static Component seconds(int ticks) {
		return Component.translatable("techreborn.jei.recipe.processing.time.3", SECONDS.format(ticks / 20.0));
	}

	static void label(GuiGraphics graphics, Component text, int x, int y, boolean rightAligned) {
		var font = Minecraft.getInstance().font;
		int drawX = rightAligned ? x - font.width(text) : x;
		graphics.drawString(font, text, drawX, y, 0xFF404040, false);
	}

	/** Wraps a sprite-drawing function as a JEI drawable (used for the tank frame over a fluid slot). */
	static IDrawable drawable(int width, int height, DrawFunction function) {
		return new IDrawable() {
			@Override
			public int getWidth() {
				return width;
			}

			@Override
			public int getHeight() {
				return height;
			}

			@Override
			public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
				function.draw(graphics, xOffset, yOffset);
			}
		};
	}

	@FunctionalInterface
	interface DrawFunction {
		void draw(GuiGraphics graphics, int x, int y);
	}
}
