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

package techreborn.blocks.storage.energy;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import reborncore.api.ToolManager;
import reborncore.common.util.WrenchUtils;
import techreborn.blockentity.GuiType;
import techreborn.blockentity.storage.energy.lesu.LapotronicSUBlockEntity;

public class LapotronicSUBlock extends EnergyStorageBlock {

	public LapotronicSUBlock() {
		super("LESU", GuiType.LESU);
	}

	@Override
	protected void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() == newState.getBlock()) {
			return;
		}
		if (worldIn.getBlockEntity(pos) instanceof LapotronicSUBlockEntity blockEntity) {
			blockEntity.disconnectNetwork();
		}
		super.onRemove(state, worldIn, pos, newState, isMoving);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LapotronicSUBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity player, ItemStack itemstack) {
		super.setPlacedBy(level, pos, state, player, itemstack);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof LapotronicSUBlockEntity blockEntity) {
			blockEntity.checkNeighbors();
		}
	}

	// Dopo la chiave inglese (anche se smonta il blocco) la rete LESU va ricalcolata.
	@Override
	protected void onWrenched(Level level, BlockEntity blockEntity) {
		if (!level.isClientSide && blockEntity instanceof LapotronicSUBlockEntity target) {
			target.disconnectNetwork();
		}
	}

}
