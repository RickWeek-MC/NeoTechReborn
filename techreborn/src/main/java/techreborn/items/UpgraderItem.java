/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 TechReborn
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

package techreborn.items;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import techreborn.blockentity.storage.fluid.TankUnitBaseBlockEntity;
import techreborn.blockentity.storage.item.StorageUnitBaseBlockEntity;
import techreborn.init.TRContent.StorageUnit;
import techreborn.init.TRContent.TankUnit;

public class UpgraderItem extends Item {

	public UpgraderItem() {
		super(new Item.Properties());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos blockPos = context.getClickedPos();
		BlockEntity oldBlockEntity = level.getBlockEntity(blockPos);
		if (oldBlockEntity == null){
			return InteractionResult.PASS;
		}
		if (!(oldBlockEntity instanceof StorageUnitBaseBlockEntity) && !(oldBlockEntity instanceof TankUnitBaseBlockEntity)){
			return InteractionResult.PASS;
		}
		BlockState oldBlockState = level.getBlockState(blockPos);
		BlockState newBlockState = null;
		String newType = "";
		// if no storage upgrader, the isOf compares with null and returns false
		if (oldBlockState.is(StorageUnit.getUpgradableFor(this).map(StorageUnit::asBlock).orElse(null))) {
			// upgradable is now guaranteed to be present, or something is seriously wrong
			// we want to get the next unit in the enum, hence ordinal()+1
			newBlockState = StorageUnit.values()[StorageUnit.getUpgradableFor(this).orElseThrow().ordinal()+1].asBlock().withPropertiesOf(oldBlockState);
			newType = StorageUnit.values()[StorageUnit.getUpgradableFor(this).orElseThrow().ordinal()+1].name();
		}
		// same for the tank
		else if (oldBlockState.is(TankUnit.getUpgradableFor(this).map(TankUnit::asBlock).orElse(null))) {
			newBlockState = TankUnit.values()[TankUnit.getUpgradableFor(this).orElseThrow().ordinal()+1].asBlock().defaultBlockState();
			newType = TankUnit.values()[TankUnit.getUpgradableFor(this).orElseThrow().ordinal()+1].name();
		}
		if (newBlockState == null) {
			return InteractionResult.PASS;
		}

		CompoundTag data = oldBlockEntity.saveWithoutMetadata(level.registryAccess());
		data.putString("unitType", newType);

		// empty storage to prevent item spill
		oldBlockEntity.loadWithComponents(new CompoundTag(), level.registryAccess());

		level.setBlockAndUpdate(blockPos, newBlockState);

		// restore content and set a new storage type
		BlockEntity newBlockEntity = level.getBlockEntity(blockPos);
		if (newBlockEntity != null){
			newBlockEntity.loadWithComponents(data, level.registryAccess());
			((MachineBaseBlockEntity) newBlockEntity).syncWithAll();
		}

		ItemStack stack = context.getItemInHand();
		stack.shrink(1);

		return InteractionResult.SUCCESS;
	}
}
