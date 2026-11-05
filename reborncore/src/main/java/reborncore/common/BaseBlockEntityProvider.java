/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 TeamReborn
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

package reborncore.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class BaseBlockEntityProvider extends Block implements EntityBlock {

	protected BaseBlockEntityProvider(Properties builder) {
		super(builder);
	}

	public Optional<ItemStack> getDropWithContents(Level world, BlockPos pos, ItemStack stack) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null) {
			return Optional.empty();
		}

		ItemStack newStack = stack.copy();
		newStack.applyComponents(blockEntity.collectComponents());
		CompoundTag blockEntityData = blockEntity.saveWithId(world.registryAccess());
		newStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityData));
		return Optional.of(newStack);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.setPlacedBy(world, pos, state, placer, itemStack);

		CustomData customData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (customData == null) {
			return;
		}

		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null) {
			customData.loadInto(blockEntity, world.registryAccess());
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		return (world1, pos, state1, blockEntity) -> {
			if (blockEntity instanceof BlockEntityTicker) {
				((BlockEntityTicker) blockEntity).tick(world1, pos, state1, blockEntity);
			}
		};
	}

	private void stripLocationData(CompoundTag compound) {
		compound.remove("x");
		compound.remove("y");
		compound.remove("z");
	}

	private void injectLocationData(CompoundTag compound, BlockPos pos) {
		compound.putInt("x", pos.getX());
		compound.putInt("y", pos.getY());
		compound.putInt("z", pos.getZ());
	}

	public void getDrops(BlockState state, NonNullList<ItemStack> drops, Level world, BlockPos pos, int fortune){

	}
}
