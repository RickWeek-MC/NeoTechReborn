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

package techreborn.blockentity.machine.tier1;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import reborncore.api.IToolDrop;
import reborncore.api.blockentity.InventoryProvider;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.powerSystem.PowerAcceptorBlockEntity;
import reborncore.common.screen.BuiltScreenHandler;
import reborncore.common.screen.BuiltScreenHandlerProvider;
import reborncore.common.screen.builder.ScreenHandlerBuilder;
import reborncore.common.util.ItemUtils;
import reborncore.common.util.RebornInventory;
import techreborn.config.TechRebornConfig;
import techreborn.init.ModSounds;
import techreborn.init.TRBlockEntities;
import techreborn.init.TRContent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by modmuss50 on 20/06/2017.
 */
public class AutoCraftingTableBlockEntity extends PowerAcceptorBlockEntity
	implements IToolDrop, InventoryProvider, BuiltScreenHandlerProvider {

	public static final int CRAFTING_HEIGHT = 3;
	public static final int CRAFTING_WIDTH = 3;
	public static final int CRAFTING_AREA = CRAFTING_HEIGHT * CRAFTING_WIDTH;
	public static final int RECIPE_TIME = 120;
	public static final int EU_TICK = 10;

	public final RebornInventory<AutoCraftingTableBlockEntity> inventory = new RebornInventory<>(CRAFTING_AREA + 2, "AutoCraftingTableBlockEntity", 64, this);
	private final int OUTPUT_SLOT = CRAFTING_AREA; // first slot is indexed by 0, so this is the last non crafting slot
	private final int EXTRA_OUTPUT_SLOT = CRAFTING_AREA + 1;

	public int progress = 0;
	public int maxProgress = RECIPE_TIME;
	public long euTick = EU_TICK;
	public long lastSoundTime = 0;
	public int balanceSlot = 0;

	TransientCraftingContainer inventoryCrafting = null;
	CraftingRecipe lastRecipe = null;
	ItemStack outputPreview = ItemStack.EMPTY;

	Item[] layoutInv = new Item[CRAFTING_AREA];

	public boolean locked = false;

	public AutoCraftingTableBlockEntity(BlockPos pos, BlockState state) {
		super(TRBlockEntities.AUTO_CRAFTING_TABLE, pos, state);
	}

	@Nullable
	public CraftingRecipe getCurrentRecipe(TransientCraftingContainer craftingInventory, CraftingInput input) {
		if (lastRecipe != null && lastRecipe.matches(input, level)) {
			if (outputPreview == ItemStack.EMPTY) {
				layoutInv = getCraftingLayout(craftingInventory);
				outputPreview = lastRecipe.assemble(input, level.registryAccess());
			} else if (lastRecipe instanceof CustomRecipe) {
				Item[] currentInvLayout = getCraftingLayout(craftingInventory);
				if (!Arrays.equals(layoutInv, currentInvLayout)) {
					layoutInv = currentInvLayout;
					outputPreview = lastRecipe.assemble(input, level.registryAccess());
				}
			}
			return lastRecipe;
		}

		Item[] currentInvLayout = getCraftingLayout(craftingInventory);
		if (Arrays.equals(layoutInv, currentInvLayout)) return null;

		layoutInv = currentInvLayout;

		Optional<CraftingRecipe> testRecipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).map(RecipeHolder::value);
		if (testRecipe.isPresent()) {
			lastRecipe = testRecipe.get();
			outputPreview = lastRecipe.assemble(input, level.registryAccess());
			return lastRecipe;
		} else {
			outputPreview = ItemStack.EMPTY;
		}

		return null;
	}

	private Item[] getCraftingLayout(TransientCraftingContainer craftingInventory) {
		Item[] layout = new Item[CRAFTING_AREA];
		for (int i = 0; i < CRAFTING_AREA; i++) {
			layout[i] = craftingInventory.getItem(i).getItem();
		}
		return layout;
	}

	private TransientCraftingContainer getTransientCraftingContainer() {
		if (inventoryCrafting == null) {
			inventoryCrafting = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
				@Override
				public ItemStack quickMoveStack(Player player, int index) {
					return ItemStack.EMPTY;
				}

				@Override
				public boolean stillValid(Player playerIn) {
					return false;
				}
			}, CRAFTING_WIDTH, CRAFTING_HEIGHT);
		}
		for (int i = 0; i < CRAFTING_AREA; i++) {
			inventoryCrafting.setItem(i, inventory.getItem(i));
		}
		return inventoryCrafting;
	}

	@Nullable
	private ItemStack getRecipeRemainder(CraftingRecipe recipe, CraftingInput input) {
		NonNullList<ItemStack> remainingStacks = recipe.getRemainingItems(input);
		ItemStack reminderStack, recipeReminder = ItemStack.EMPTY;
		for (int slot = 0, size = remainingStacks.size(); slot < size; slot++) {
			reminderStack = remainingStacks.get(slot);
			if (!reminderStack.isEmpty()) {
				recipeReminder = reminderStack.copy();
				for (slot = slot + 1; slot < size; slot++) {
					reminderStack = remainingStacks.get(slot);
					if (!reminderStack.isEmpty()) {
						if (ItemUtils.isItemEqual(recipeReminder, reminderStack, true, true)) {
							recipeReminder.grow(reminderStack.getCount());
						} else {
							return null;
						}
					}
				}
				break;
			}
		}
		return recipeReminder;
	}

	private boolean hasOutputSpace(ItemStack output, int slot) {
		ItemStack stack = inventory.getItem(slot);
		if (stack.isEmpty()) {
			return true;
		}
		if (ItemUtils.isItemEqual(stack, output, true, true)) {
			return stack.getMaxStackSize() >= stack.getCount() + output.getCount();
		}
		return false;
	}

	private boolean make(CraftingRecipe recipe, CraftingInput input, ItemStack recipeReminder) {
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		if (ingredients.isEmpty()) {
			input.items().forEach(stack -> stack.shrink(1));
			if (!recipeReminder.isEmpty()) {
				moveOutput(recipeReminder, EXTRA_OUTPUT_SLOT);
			}
		} else {
			// each slot can only be used once because in canMake we only checked if decrement by 1 still retains the recipe
			// otherwise recipes can break when an ingredient is used multiple times
			boolean[] slotUsed = new boolean[CRAFTING_AREA];
			for (int i = 0; i < ingredients.size(); i++) {
				Ingredient ingredient = ingredients.get(i);
				// Looks for the best slot to take it from
				ItemStack bestSlot = inventory.getItem(i);
				if (ingredient.test(bestSlot) && !slotUsed[i]) {
					slotUsed[i] = true;
					ItemStack remainderStack = getRemainderItem(bestSlot);
					bestSlot.shrink(1);
					if (!remainderStack.isEmpty()) {
						moveOutput(remainderStack, EXTRA_OUTPUT_SLOT);
					}

				} else {
					// check all slots in search of the ingredient
					for (int j = 0; j < CRAFTING_AREA; j++) {
						ItemStack stack = inventory.getItem(j);
						if (ingredient.test(stack) && !slotUsed[j]) {
							slotUsed[j] = true;
							ItemStack remainderStack = getRemainderItem(stack);
							stack.shrink(1);
							if (!remainderStack.isEmpty()) {
								moveOutput(remainderStack, EXTRA_OUTPUT_SLOT);
							}
						}
					}
				}
			}
		}
		moveOutput(outputPreview, OUTPUT_SLOT);
		return true;
	}

	private void moveOutput(ItemStack stack, int slot) {
		ItemStack currentOutput = inventory.getItem(slot);
		if (currentOutput.isEmpty()) {
			inventory.setItem(slot, stack.copy());
		} else {
			currentOutput.grow(stack.getCount());
		}
	}

	private ItemStack getRemainderItem(ItemStack stack) {
		return stack.getCraftingRemainingItem();
	}

	private Optional<TransientCraftingContainer> balanceRecipe(TransientCraftingContainer craftCache, CraftingRecipe currentRecipe) {
		balanceSlot++;
		if (balanceSlot > craftCache.getContainerSize()) {
			balanceSlot = 0;
		}
		// Find the best slot for each item in a recipe, and move it if needed
		ItemStack sourceStack = inventory.getItem(balanceSlot);
		if (sourceStack.isEmpty()) {
			return Optional.empty();
		}
		List<Integer> possibleSlots = new ArrayList<>();
		for (int s = 0; s < currentRecipe.getIngredients().size(); s++) {
			for (int i = 0; i < CRAFTING_AREA; i++) {
				if (possibleSlots.contains(i)) {
					continue;
				}
				ItemStack stackInSlot = inventory.getItem(i);
				Ingredient ingredient = currentRecipe.getIngredients().get(s);
				if (ingredient != Ingredient.EMPTY && ingredient.test(sourceStack)) {
					if (stackInSlot.getItem() == sourceStack.getItem()) {
						possibleSlots.add(i);
						break;
					}
				}
			}

		}

		if (!possibleSlots.isEmpty()) {
			int totalItems = possibleSlots.stream()
				.mapToInt(value -> inventory.getItem(value).getCount()).sum();
			int slots = possibleSlots.size();

			//This makes an array of ints with the best possible slot distribution
			int[] split = new int[slots];
			int remainder = totalItems % slots;
			Arrays.fill(split, totalItems / slots);
			while (remainder > 0) {
				for (int i = 0; i < split.length; i++) {
					if (remainder > 0) {
						split[i] += 1;
						remainder--;
					}
				}
			}

			List<Integer> slotDistribution = possibleSlots.stream()
				.mapToInt(value -> inventory.getItem(value).getCount())
				.boxed().collect(Collectors.toList());

			boolean needsBalance = false;
			for (int required : split) {
				if (slotDistribution.contains(required)) {
					//We need to remove the int, not at the int, this seems to work around that
					slotDistribution.remove(Integer.valueOf(required));
				} else {
					needsBalance = true;
				}
			}
			if (!needsBalance) {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}

		//Slot, count
		Pair<Integer, Integer> bestSlot = null;
		for (Integer slot : possibleSlots) {
			ItemStack slotStack = inventory.getItem(slot);
			if (slotStack.isEmpty()) {
				bestSlot = Pair.of(slot, 0);
			}
			if (bestSlot == null) {
				bestSlot = Pair.of(slot, slotStack.getCount());
			} else if (bestSlot.getRight() >= slotStack.getCount()) {
				bestSlot = Pair.of(slot, slotStack.getCount());
			}
		}
		if (bestSlot.getLeft() == balanceSlot
			|| bestSlot.getRight() == sourceStack.getCount()
			|| inventory.getItem(bestSlot.getLeft()).isEmpty()
			|| !ItemUtils.isItemEqual(sourceStack, inventory.getItem(bestSlot.getLeft()), true, true)) {
			return Optional.empty();
		}
		sourceStack.shrink(1);
		inventory.getItem(bestSlot.getLeft()).grow(1);
		inventory.setHashChanged();

		return Optional.of(getTransientCraftingContainer());
	}

	// PowerAcceptorBlockEntity
	@Override
	public void tick(Level level, BlockPos pos, BlockState state, MachineBaseBlockEntity blockEntity) {
		super.tick(level, pos, state, blockEntity);
		if (level == null || level.isClientSide || getStored() < euTick) {
			return;
		}
		TransientCraftingContainer inventory = getTransientCraftingContainer();
		if (inventory.isEmpty()) {
			progress = 0;
			outputPreview = ItemStack.EMPTY;
			return;
		}
		CraftingInput input = getRecipeInput(inventory);
		CraftingRecipe recipe = getCurrentRecipe(inventory, input);
		if (recipe == null) {
			progress = 0;
			return;
		}
		if (!hasOutputSpace(outputPreview, OUTPUT_SLOT)) {
			return;
		}

		Optional<TransientCraftingContainer> balanceResult = balanceRecipe(inventory, recipe);
		balanceResult.ifPresent(craftingInventory -> inventoryCrafting = craftingInventory);

		// Don't allow recipe to change (Keep at least one of each slot stocked, assuming it's actually a recipe)
		if (locked) {
			for (ItemStack stack : input.items()) {
				if (stack.getCount() == 1) {
					return;
				}
			}
		}

		ItemStack recipeReminder = getRecipeRemainder(recipe, input);
		if (recipeReminder == null || !hasOutputSpace(recipeReminder, EXTRA_OUTPUT_SLOT)) {
			return;
		}

		if (progress >= maxProgress) {
			progress = 0;
			make(recipe, input, recipeReminder);
			if (inventory.isEmpty()) {
				outputPreview = ItemStack.EMPTY;
			}
		} else {
			if (progress == 0) {
				maxProgress = Math.max((int) (RECIPE_TIME * (1.0 - getSpeedMultiplier())), 1);
				euTick = getEuPerTick(EU_TICK);
				if (getStored() < euTick) {
					return;
				}
			}
			progress++;
			if (!isMuffled()) {
				long time = level.getGameTime();
				if (time - lastSoundTime > RECIPE_TIME) {
					lastSoundTime = time;
					level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.AUTO_CRAFTING,
						SoundSource.BLOCKS, 0.3F, 0.8F);
				}
			}
			useEnergy(euTick);
		}
	}

	@Override
	public long getBaseMaxPower() {
		return TechRebornConfig.autoCraftingTableMaxEnergy;
	}

	@Override
	public long getBaseMaxOutput() {
		return 0;
	}

	@Override
	public long getBaseMaxInput() {
		return TechRebornConfig.autoCraftingTableMaxInput;
	}

	@Override
	public boolean canProvideEnergy(@Nullable Direction side) {
		return false;
	}

	@Override
	public void saveAdditional(CompoundTag tag, HolderLookup.Provider registryLookup) {
		tag.putBoolean("locked", locked);
		super.saveAdditional(tag, registryLookup);
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registryLookup) {
		if (tag.contains("locked")) {
			locked = tag.getBoolean("locked");
		}
		super.loadAdditional(tag, registryLookup);
	}

	// IToolDrop
	@Override
	public ItemStack getToolDrop(Player playerIn) {
		return TRContent.Machine.AUTO_CRAFTING_TABLE.getStack();
	}

	// InventoryProvider
	@Override
	public RebornInventory<AutoCraftingTableBlockEntity> getInventory() {
		return inventory;
	}

	// BuiltScreenHandlerProvider
	@Override
	public BuiltScreenHandler createScreenHandler(int syncID, Player player) {
		return new ScreenHandlerBuilder("autocraftingtable").player(player.getInventory()).inventory().hotbar().addInventory()
			.blockEntity(this)
			.slot(0, 28, 25).slot(1, 46, 25).slot(2, 64, 25)
			.slot(3, 28, 43).slot(4, 46, 43).slot(5, 64, 43)
			.slot(6, 28, 61).slot(7, 46, 61).slot(8, 64, 61)
			.outputSlot(OUTPUT_SLOT, 145, 42)
			.outputSlot(EXTRA_OUTPUT_SLOT, 145, 70)
			.syncEnergyValue().sync(ByteBufCodecs.INT, this::getProgress, this::setProgress)
			.sync(ByteBufCodecs.INT, this::getMaxProgress, this::setMaxProgress)
			.sync(ByteBufCodecs.INT, this::getLockedInt, this::setLockedInt)
			.sync(ItemStack.OPTIONAL_STREAM_CODEC, this::getOutputPreview, this::setOutputPreview)
			.addInventory().create(this, syncID);
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public int getMaxProgress() {
		if (maxProgress == 0) {
			maxProgress = 1;
		}
		return maxProgress;
	}

	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
	}

	public int getLockedInt() {
		return locked ? 1 : 0;
	}

	public void setLockedInt(int lockedInt) {
		locked = lockedInt == 1;
	}

	public ItemStack getOutputPreview() {
		return outputPreview;
	}

	public void setOutputPreview(ItemStack stack) {
		outputPreview = stack;
	}

	private CraftingInput getRecipeInput(TransientCraftingContainer craftingInventory) {
		List<ItemStack> stacks = new ArrayList<>(craftingInventory.getContainerSize());
		for (int i = 0; i < craftingInventory.getContainerSize(); i++) {
			stacks.add(craftingInventory.getItem(i));
		}
		return CraftingInput.of(craftingInventory.getWidth(), craftingInventory.getHeight(), stacks);
	}
}
