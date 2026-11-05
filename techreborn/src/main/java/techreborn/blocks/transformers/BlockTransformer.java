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

package techreborn.blocks.transformers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import reborncore.api.ToolManager;
import reborncore.common.util.WrenchUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import reborncore.common.BaseBlockEntityProvider;
import reborncore.common.blocks.BlockWrenchEventHandler;
import techreborn.init.TRBlockSettings;

/**
 * Created by Rushmead
 * <p>
 * Port note: l'onUse di upstream serviva solo alla chiave inglese. Su NeoForge BlockWrenchEventHandler
 * chiama useWithoutItem sui blocchi in wrenchableBlocks quando si ha un attrezzo in mano.
 */
public abstract class BlockTransformer extends BaseBlockEntityProvider {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public final String name;

	public BlockTransformer(String name) {
		super(TRBlockSettings.transformer());
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
		this.name = name;
		BlockWrenchEventHandler.wrenchableBlocks.add(this);
	}

	public void setFacing(Direction facing, Level level, BlockPos pos) {
		level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(FACING, facing));
	}

	public Direction getFacing(BlockState state) {
		return state.getValue(FACING);
	}

	// BaseBlockEntityProvider
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		Direction facing = placer.getDirection().getOpposite();
		if (placer.getXRot() < -50) {
			facing = Direction.DOWN;
		} else if (placer.getXRot() > 50) {
			facing = Direction.UP;
		}
		setFacing(facing, level, pos);
	}

	// Block
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (level.getBlockEntity(pos) == null) {
			return InteractionResult.FAIL;
		}

		if (!stack.isEmpty() && ToolManager.INSTANCE.canHandleTool(stack)) {
			if (WrenchUtils.handleWrench(stack, level, pos, player, hitResult.getDirection())) {
				return InteractionResult.PASS;
			}
		}

		return super.useWithoutItem(state, level, pos, player, hitResult);
	}
}
