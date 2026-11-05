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

package reborncore.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Names of the GUI sprites stitched from assets/reborncore/atlases/gui.json.
 * <p>
 * Port note: upstream wrapped each name in a SpriteIdentifier holding the atlas id as well, but the
 * atlas was never read back: every draw went through the GUI sprite manager, which already knows the
 * atlas. Here a sprite is just its ResourceLocation.
 */
public final class GuiSprites {
	public static final ResourceLocation CHARGE_SLOT_ICON = create("charge_slot_icon");
	public static final ResourceLocation DISCHARGE_SLOT_ICON = create("discharge_slot_icon");
	public static final ResourceLocation ENERGY_BAR = create("energy_bar");
	public static final ResourceLocation ENERGY_BAR_BACKGROUND = create("energy_bar_background");
	public static final ResourceLocation TOP_ENERGY_BAR = create("top_energy_bar");
	public static final ResourceLocation TOP_ENERGY_BAR_BACKGROUND = create("top_energy_bar_background");
	public static final ResourceLocation LEFT_TAB = create("left_tab");
	public static final ResourceLocation LEFT_TAB_SELECTED = create("left_tab_selected");
	public static final ResourceLocation CONFIGURE_ICON = create("configure_icon");
	public static final ResourceLocation UPGRADE_ICON = create("upgrade_icon");
	public static final ResourceLocation ENERGY_ICON = create("energy_icon");
	public static final ResourceLocation ENERGY_ICON_EMPTY = create("energy_icon_empty");
	public static final ResourceLocation JEI_ICON = create("jei_icon");
	public static final ResourceLocation BUTTON_SLOT_NORMAL = create("button_slot_normal");
	public static final ResourceLocation FAKE_SLOT = create("fake_slot");
	public static final ResourceLocation BUTTON_HOVER_OVERLAY_SLOT_NORMAL = create("button_hover_overlay_slot_normal");
	public static final ResourceLocation SLOT_CONFIG_POPUP = create("slot_config_popup");
	public static final ResourceLocation FAST_FORWARD = create("fast_forward");
	public static final ResourceLocation FORWARD = create("forward");
	public static final ResourceLocation REWIND = create("rewind");
	public static final ResourceLocation FAST_REWIND = create("fast_rewind");
	public static final ResourceLocation SLOT = create("slot");
	public static final ResourceLocation OUTPUT_SLOT = create("output_slot");
	public static final ResourceLocation BUTTON_UNLOCKED = create("button_unlocked");
	public static final ResourceLocation BUTTON_LOCKED = create("button_locked");
	public static final ResourceLocation BUTTON_HOLOGRAM_ENABLED = create("button_hologram_enabled");
	public static final ResourceLocation BUTTON_HOLOGRAM_DISABLED = create("button_hologram_disabled");
	public static final ResourceLocation SLOT_TAB = create("slot_tab");
	public static final ResourceLocation UPGRADES = create("upgrades");
	public static final ResourceLocation TANK_BACKGROUND = create("tank_background");
	public static final ResourceLocation TANK_FOREGROUND = create("tank_foreground");
	public static final ResourceLocation SLOT_BAR_RIGHT = create("slot_bar_right");
	public static final ResourceLocation SLOT_BAR_CENTER = create("slot_bar_center");
	public static final ResourceLocation SLOT_BAR_LEFT = create("slot_bar_left");
	public static final ResourceLocation POWER_BAR_BASE = create("power_bar_base");
	public static final ResourceLocation POWER_BAR_OVERLAY = create("power_bar_overlay");

	public static final ResourceLocation BACKGROUND = create("background");

	public static final ResourceLocation EXIT_BUTTON_NORMAL = create("exit_button_normal");
	public static final ResourceLocation EXIT_BUTTON_HOVERED = create("exit_button_hovered");
	public static final Button EXIT_BUTTON = new Button(EXIT_BUTTON_NORMAL, EXIT_BUTTON_HOVERED);

	public static final ResourceLocation DARK_CHECK_BOX_NORMAL = create("dark_check_box_normal");
	public static final ResourceLocation DARK_CHECK_BOX_TICKED = create("dark_check_box_ticked");
	public static final CheckBox DARK_CHECK_BOX = new CheckBox(DARK_CHECK_BOX_NORMAL, DARK_CHECK_BOX_TICKED);

	public static final ResourceLocation LIGHT_CHECK_BOX_NORMAL = create("light_check_box_normal");
	public static final ResourceLocation LIGHT_CHECK_BOX_TICKED = create("light_check_box_ticked");
	public static final CheckBox LIGHT_CHECK_BOX = new CheckBox(LIGHT_CHECK_BOX_NORMAL, LIGHT_CHECK_BOX_TICKED);

	private GuiSprites() {
	}

	public static ResourceLocation create(String name) {
		return ResourceLocation.fromNamespaceAndPath("reborncore", name);
	}

	/**
	 * Draws the sprite scaled to the given size, ignoring any nine-slice metadata it carries.
	 */
	public static void drawSpriteStretched(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int textureWidth, int textureHeight) {
		guiGraphics.blit(x, y, 0, textureWidth, textureHeight, GuiBase.getSprite(sprite));
	}

	public static void drawSpriteStretched(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width, int height, int textureWidth, int textureHeight, GuiBase<?> gui) {
		drawSpriteStretched(guiGraphics, sprite, x, y, width, height, textureWidth, textureHeight, gui.getGuiLeft(), gui.getGuiTop());
	}

	public static void drawSpriteStretched(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width, int height, int textureWidth, int textureHeight) {
		drawSpriteStretched(guiGraphics, sprite, x, y, width, height, textureWidth, textureHeight, 0, 0);
	}

	/**
	 * Draws the sprite at its full size but clipped to a width x height window, which is how the
	 * partially filled bars are drawn. sx/sy shift the scissor box into screen space.
	 */
	public static void drawSpriteStretched(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width, int height, int textureWidth, int textureHeight, int sx, int sy) {
		guiGraphics.enableScissor(x + sx, y + sy, x + width + sx, y + height + sy);
		drawSpriteStretched(guiGraphics, sprite, x, y, textureWidth, textureHeight);
		guiGraphics.disableScissor();
	}

	@Deprecated
	public static void drawSprite(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y) {
		final TextureAtlasSprite atlasSprite = GuiBase.getSprite(sprite);

		guiGraphics.blit(
			x,
			y,
			0,
			atlasSprite.contents().width(),
			atlasSprite.contents().height(),
			atlasSprite
		);
	}

	@Deprecated
	public static void drawSprite(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width, int height) {
		drawSprite(guiGraphics, sprite, x, y, width, height, 0, 0);
	}

	@Deprecated
	public static void drawSprite(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width, int height, GuiBase<?> gui) {
		drawSprite(guiGraphics, sprite, x, y, width, height, gui.getGuiLeft(), gui.getGuiTop());
	}

	@Deprecated
	public static void drawSprite(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width, int height, int sx, int sy) {
		guiGraphics.enableScissor(x + sx, y + sy, x + width + sx, y + height + sy);
		drawSprite(guiGraphics, sprite, x, y);
		guiGraphics.disableScissor();
	}

	public record Button(ResourceLocation normal, ResourceLocation hovered) {
	}

	public record CheckBox(ResourceLocation normal, ResourceLocation ticked) {
	}
}
