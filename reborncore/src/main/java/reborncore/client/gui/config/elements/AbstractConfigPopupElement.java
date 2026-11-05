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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import reborncore.client.gui.GuiBase;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.util.MachineFacing;

import java.util.Arrays;

public abstract class AbstractConfigPopupElement extends ElementBase {
	private final int height;
	private final String[] pencils;
	private int pencilWidth;
	private int fixIndex = Integer.MAX_VALUE;
	private boolean mouseDown = false;
	private final boolean[] mouseIn = { false, false, false, false, false, false };
	private final MachineFacing[] mouseBoxFacingMap = {
		MachineFacing.UP,
		MachineFacing.FRONT,
		MachineFacing.RIGHT,
		MachineFacing.LEFT,
		MachineFacing.DOWN,
		MachineFacing.BACK
	};
	private final int[][] mouseBoxMap = {
		{ 23, 4 },
		{ 23, 23 },
		{ 42, 23 },
		{ 4, 23 },
		{ 23, 42 },
		{ 42, 42 }
	};

	@Nullable
	public String pencil;

	public AbstractConfigPopupElement(int x, int y, int height, ResourceLocation sprite, int textureWidth, int textureHeight, String[] pencils) {
		super(x, y, sprite, textureWidth, textureHeight);
		this.height = height;
		this.pencils = pencils;
		int space = 75 - pencils.length;
		this.pencilWidth = space / pencils.length;
		if (space % pencilWidth != 0) {
			this.pencilWidth++;
			this.fixIndex = space % pencils.length;
		}
	}

	@Override
	public boolean isMouseWithinRect(GuiBase<?> gui, double mouseX, double mouseY) {
		// sprite size is 62x62, expands to 84x79
		return isInRect(gui, getX() - 8, getY(), 84, 79, mouseX, mouseY);
	}

	@Override
	public final void draw(GuiGraphics guiGraphics, GuiBase<?> gui, int mouseX, int mouseY) {
		guiGraphics.pose().pushPose();
		int x = adjustX(gui, getX() - 8);
		int y = adjustY(gui, getY() - 7);
		gui.builder.drawDefaultBackground(
			guiGraphics,
			x,
			y,
			84,
			height
		);
		guiGraphics.pose().popPose();

		super.draw(guiGraphics, gui, mouseX, mouseY);

		final MachineBaseBlockEntity machine = ((MachineBaseBlockEntity) gui.be);
		final BlockState state = machine.getBlockState();
		final BlockState defaultState = state.getBlock().defaultBlockState();
		final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		final BakedModel model = dispatcher.getBlockModelShaper().getBlockModel(defaultState);

		drawState(guiGraphics, gui, model, defaultState, dispatcher, 4, 23, Axis.YP.rotationDegrees(90F), 0, 0); //left
		drawState(guiGraphics, gui, model, defaultState, dispatcher, 23, 4, Axis.XN.rotationDegrees(90F), 0, 0); //top
		drawState(guiGraphics, gui, model, defaultState, dispatcher, 23, 23, null, 0, 0); //centre
		drawState(guiGraphics, gui, model, defaultState, dispatcher, 23, 26, Axis.XP.rotationDegrees(90F), 0, 16); //bottom
		drawState(guiGraphics, gui, model, defaultState, dispatcher, 42, 23, Axis.YP.rotationDegrees(90F), 0, 0); //right
		drawState(guiGraphics, gui, model, defaultState, dispatcher, 26, 42, Axis.YP.rotationDegrees(180F), 16, 0); //back

		if (mouseDown) {
			for (int i = 0; i < 6; i++) {
				if (isInBox(mouseBoxMap[i][0], mouseBoxMap[i][1], 16, 16, mouseX, mouseY, gui)) {
					if (!mouseIn[i]) {
						mouseIn[i] = true;
						cycleConfig(mouseBoxFacingMap[i].getFacing(gui.getMachine()), gui);
					}
					break;
				}
			}
		}

		drawSateColor(guiGraphics, gui, MachineFacing.UP.getFacing(machine), 22, -1);
		drawSateColor(guiGraphics, gui, MachineFacing.FRONT.getFacing(machine), 22, 18);
		drawSateColor(guiGraphics, gui, MachineFacing.DOWN.getFacing(machine), 22, 37);
		drawSateColor(guiGraphics, gui, MachineFacing.RIGHT.getFacing(machine), 41, 18);
		drawSateColor(guiGraphics, gui, MachineFacing.BACK.getFacing(machine), 41, 37);
		drawSateColor(guiGraphics, gui, MachineFacing.LEFT.getFacing(machine), 3, 18);

		drawPencil(guiGraphics, gui, mouseX, mouseY, x, y + 71);
	}

	@Override
	public final boolean onClick(GuiBase<?> gui, double mouseX, double mouseY) {
		mouseDown = true;
		for (int i = 0; i < 6; i++) {
			if (isInBox(mouseBoxMap[i][0], mouseBoxMap[i][1], 16, 16, mouseX, mouseY, gui)) {
				mouseIn[i] = true;
				cycleConfig(mouseBoxFacingMap[i].getFacing(gui.getMachine()), gui);
				return true;
			}
		}
		int rectX = -2, rectWidth = pencilWidth - 2;
		for (String pencil : pencils) {
			if (isInBox(rectX, 65, rectWidth, 11, mouseX, mouseY, gui)) {
				this.pencil = pencil.equals(this.pencil) ? null : pencil;
				return true;
			}
			rectX += pencilWidth + 1;
		}

		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int state) {
		mouseDown = false;
		Arrays.fill(mouseIn, false);
		return true;
	}

	protected abstract void cycleConfig(Direction side, GuiBase<?> guiBase);

	protected abstract void drawSateColor(GuiGraphics guiGraphics, GuiBase<?> gui, Direction side, int inx, int iny);

	protected boolean isInBox(int rectX, int rectY, int rectWidth, int rectHeight, double pointX, double pointY, GuiBase<?> guiBase) {
		rectX += getX();
		rectY += getY();
		return isInRect(guiBase, rectX, rectY, rectWidth, rectHeight, pointX, pointY);
	}

	/**
	 * Renders one face of the machine's block model into the configuration popup.
	 * <p>
	 * Port note: straight rename of the Yarn render path. BlockRenderManager -> BlockRenderDispatcher,
	 * RotationAxis -> Axis, MatrixStack -> PoseStack, VertexConsumerProvider.Immediate ->
	 * MultiBufferSource.BufferSource, draw() -> endBatch(), OverlayTexture.getU -> u and DEFAULT_UV ->
	 * NO_OVERLAY. The odd pairing of an overlay U value as the packed light argument is upstream's.
	 */
	protected void drawState(GuiGraphics guiGraphics,
						GuiBase<?> gui,
						BakedModel model,
						BlockState actualState,
						BlockRenderDispatcher dispatcher,
						int x,
						int y,
						Quaternionf quaternion,
						int paddingLeft,
						int paddingTop) {
		PoseStack poseStack = guiGraphics.pose();
		Matrix4f positionMatrix = poseStack.last().pose();
		int left = gui.getGuiLeft() + getX() + x;
		int top = gui.getGuiTop() + getY() + y;
		Vector3f vector3f = positionMatrix.transformPosition(left + paddingLeft, top + paddingTop, 0.0F, new Vector3f());
		Vector3f vector3f2 = positionMatrix.transformPosition(left + paddingLeft + 16, top + paddingTop + 16, 0.0F, new Vector3f());
		guiGraphics.enableScissor(Mth.floor(vector3f.x), Mth.floor(vector3f.y), Mth.floor(vector3f2.x), Mth.floor(vector3f2.y));

		poseStack.pushPose();
		poseStack.translate(left + 8, top + 8, 0);
		poseStack.scale(16F, 16F, 16F);
		poseStack.translate(0.5F, 0.5F, 0);
		poseStack.scale(-1, -1, 0);

		if (quaternion != null) {
			poseStack.mulPose(quaternion);
		}

		MultiBufferSource.BufferSource immediate = guiGraphics.bufferSource();
		dispatcher.getModelRenderer().renderModel(poseStack.last(), immediate.getBuffer(RenderType.solid()), actualState, model, 1F, 1F, 1F, OverlayTexture.u(15F), OverlayTexture.NO_OVERLAY);
		immediate.endBatch();
		poseStack.popPose();
		guiGraphics.disableScissor();
	}

	protected abstract int getPencilColor(String pencil);

	protected void drawPencil(GuiGraphics guiGraphics, GuiBase<?> gui, int mouseX, int mouseY, int x, int y) {
		int mx = mouseX - gui.getGuiLeft();
		int my = mouseY - gui.getGuiTop();
		x += 5;
		int color, x2, y2 = y + 13, x3, y3 = y + 3;
		Font textRenderer = gui.getTextRenderer();
		String pencil;
		Component letter;
		for (int i = 0, len = pencils.length; i < len; i++) {
			pencil = pencils[i];
			x2 = x + (i >= fixIndex ? pencilWidth - 1 : pencilWidth);
			if (pencil.equals(this.pencil)) {
				color = getPencilColor(pencil);
			} else if ((mx >= x && mx <= x2) && (my >= y && my < y2)) {
				guiGraphics.renderTooltip(textRenderer, Component.translatable("reborncore.gui.slotconfig." + pencil), mx, my);
				color = mx != x2 ? 0xff8b8b8b : 0x668b8b8b;
			} else {
				color = 0x668b8b8b;
			}
			guiGraphics.fill(x, y, x2, y2, color);
			letter = Component.literal(pencil.substring(0, 1));
			x3 = x + (pencilWidth - textRenderer.width(letter)) / 2;
			guiGraphics.drawString(textRenderer, letter, x3, y3, -1, false);
			x = x2 + 1;
		}
	}
}
