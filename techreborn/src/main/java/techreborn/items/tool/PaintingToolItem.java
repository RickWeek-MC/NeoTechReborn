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

package techreborn.items.tool;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.TooltipFlag;
import techreborn.blockentity.cable.CableBlockEntity;
import techreborn.blocks.cable.CableBlock;
import techreborn.component.TRDataComponentTypes;
import techreborn.init.TRContent;

import java.util.List;

public class PaintingToolItem extends Item {

	public PaintingToolItem() {
		super(new Item.Properties().durability(64));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.FAIL;
		}

		BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
		if (player.isShiftKeyDown()) {
			boolean opaqueFullCube = blockState.isSolidRender(context.getLevel(), context.getClickedPos())
				&& blockState.getBlock().defaultBlockState().isSolidRender(context.getLevel(), context.getClickedPos());

			if (opaqueFullCube || blockState.is(TRContent.BlockTags.NONE_SOLID_COVERS)) {
				context.getItemInHand().set(TRDataComponentTypes.PAINTING_COVER, blockState);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.FAIL;
		} else {
			BlockState cover = getCover(context.getItemInHand());
			if (cover != null && blockState.getBlock() instanceof CableBlock && blockState.getValue(CableBlock.COVERED)) {
				BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
				if (blockEntity == null) {
					return InteractionResult.FAIL;
				}
				((CableBlockEntity) blockEntity).setCover(cover);

				context.getLevel().playSound(player, context.getClickedPos(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
				if (!context.getLevel().isClientSide) {
					context.getItemInHand().hurtAndBreak(1, player, context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
				}

				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.FAIL;
	}

	public static BlockState getCover(ItemStack stack) {
		return stack.getOrDefault(TRDataComponentTypes.PAINTING_COVER, null);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		BlockState blockState = getCover(stack);
		if (blockState != null) {
			tooltip.add((Component.translatable(blockState.getBlock().getDescriptionId())).withStyle(ChatFormatting.GRAY));
			tooltip.add((Component.translatable("techreborn.tooltip.painting_tool.apply")).withStyle(ChatFormatting.GOLD));
		} else {
			tooltip.add((Component.translatable("techreborn.tooltip.painting_tool.select")).withStyle(ChatFormatting.GOLD));
		}
	}

}
