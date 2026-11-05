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


package techreborn.init;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import reborncore.common.crafting.RebornRecipe;
import reborncore.common.crafting.RecipeUtils;
import reborncore.common.fluid.RebornBucketItem;
import techreborn.TechReborn;
import techreborn.blockentity.machine.misc.DrainBlockEntity;
import techreborn.config.TechRebornConfig;
import techreborn.items.DynamicCellItem;

import java.util.List;

/**
 * Created by drcrazy on 10-Jan-20 for TechReborn-1.15.
 * <p>
 * Port note: has to run from FMLCommonSetupEvent.enqueueWork (items must exist). Differences from upstream:
 * <ul>
 *   <li>the bucket handed back by a fluid block is not an ItemFluidInfo any more, the fluid is read from its
 *   item fluid capability (same helper the drain uses);</li>
 *   <li>a stack of several cells/buckets that is used up goes through vanilla's
 *   {@code consumeWithRemainder}: upstream tested {@code getCount() < 0}, which is never true, so a full
 *   dispenser silently lost the returned empty container. Vanilla spits it out instead.</li>
 * </ul>
 */
public class TRDispenserBehavior {

	public static void init() {
		if (TechRebornConfig.dispenseScrapboxes) {
			DispenserBlock.registerBehavior(TRContent.SCRAP_BOX, new DefaultDispenseItemBehavior() {
				@Override
				public ItemStack execute(BlockSource pointer, ItemStack stack) {
					List<RebornRecipe> scrapboxRecipeList = RecipeUtils.getRecipes(pointer.level(), ModRecipes.SCRAPBOX);
					int random = RandomSource.create().nextInt(scrapboxRecipeList.size());
					ItemStack out = scrapboxRecipeList.get(random).outputs().getFirst().copy();
					stack.split(1);

					Direction facing = pointer.state().getValue(DispenserBlock.FACING);
					Position position = DispenserBlock.getDispensePosition(pointer);
					spawnItem(pointer.level(), out, 6, facing, position);
					return stack;
				}
			});
		}

		DispenserBlock.registerBehavior(TRContent.CELL, new DefaultDispenseItemBehavior() {
			@Override
			public ItemStack execute(BlockSource pointer, ItemStack stack) {
				DynamicCellItem cell = (DynamicCellItem) stack.getItem();
				LevelAccessor iLevel = pointer.level();
				BlockPos blockPos = pointer.pos().relative(pointer.state().getValue(DispenserBlock.FACING));
				BlockState blockState = iLevel.getBlockState(blockPos);
				Block block = blockState.getBlock();
				if (cell.getFluid(stack) == Fluids.EMPTY) {
					// fill cell
					if (block instanceof BucketPickup bucketPickup) {
						ItemStack fluidContainer = bucketPickup.pickupBlock(null, iLevel, blockPos, blockState);
						Fluid fluid = DrainBlockEntity.getFluidOf(fluidContainer);
						if (fluid == null) {
							TechReborn.LOGGER.debug("Could not get Fluid from ItemStack {}", fluidContainer.getItem());
						}
						if (!(fluid instanceof FlowingFluid)) {
							return super.execute(pointer, stack);
						} else {
							ItemStack filledCell = DynamicCellItem.getCellWithFluid(fluid, 1);
							return consumeWithRemainder(pointer, stack, filledCell);
						}
					} else {
						return super.execute(pointer, stack);
					}
				} else {
					// drain cell
					if (cell.placeFluid(null, pointer.level(), blockPos, null, stack)) {
						ItemStack emptyCell = cell.getEmpty();
						return consumeWithRemainder(pointer, stack, emptyCell);
					}
					return stack;
				}
			}
		});

		for (ModFluids fluid : ModFluids.values()) {
			DispenserBlock.registerBehavior(fluid, new DefaultDispenseItemBehavior() {
				@Override
				public ItemStack execute(BlockSource pointer, ItemStack stack) {
					RebornBucketItem bucket = (RebornBucketItem) stack.getItem();
					BlockPos blockPos = pointer.pos().relative(pointer.state().getValue(DispenserBlock.FACING));

					if (bucket.emptyContents(null, pointer.level(), blockPos, null)) {
						ItemStack emptyBucket = new ItemStack(Items.BUCKET);
						return consumeWithRemainder(pointer, stack, emptyBucket);
					}
					return stack;
				}
			});
		}
	}
}
