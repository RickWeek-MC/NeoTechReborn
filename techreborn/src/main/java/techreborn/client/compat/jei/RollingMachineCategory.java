package techreborn.client.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import reborncore.client.gui.GuiBuilder;
import techreborn.recipe.recipes.RollingMachineRecipe;

import java.util.ArrayList;
import java.util.List;

/** The rolling machine shows a 3x3 shaped crafting grid, so it gets its own category. */
public class RollingMachineCategory implements IRecipeCategory<RecipeHolder<RollingMachineRecipe>> {
	private static final int WIDTH = 150 - 2 * Layout.OFFSET;
	private static final int HEIGHT = 66 - 2 * Layout.OFFSET;
	// Same anchor the REI plugin used: the centre of the recipe area, minus a fixed offset.
	private static final int START_X = 75 - 58 - Layout.OFFSET;
	private static final int START_Y = 33 - 27 - Layout.OFFSET;

	private final RecipeType<RecipeHolder<RollingMachineRecipe>> type;
	private final Component title;
	private final IDrawable icon;
	private final ICraftingGridHelper gridHelper;

	public RollingMachineCategory(RecipeType<RecipeHolder<RollingMachineRecipe>> type, Component title, IDrawable icon, ICraftingGridHelper gridHelper) {
		this.type = type;
		this.title = title;
		this.icon = icon;
		this.gridHelper = gridHelper;
	}

	@Override
	public RecipeType<RecipeHolder<RollingMachineRecipe>> getRecipeType() {
		return type;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public ResourceLocation getRegistryName(RecipeHolder<RollingMachineRecipe> recipe) {
		return recipe.id();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RollingMachineRecipe> holder, IFocusGroup focuses) {
		ShapedRecipe shaped = holder.value().getShapedRecipe();
		List<IRecipeSlotBuilder> slots = new ArrayList<>();
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				slots.add(builder.addSlot(RecipeIngredientRole.INPUT, START_X + 10 + x * 18 + 1, START_Y + 1 + y * 18 + 1).setStandardSlotBackground());
			}
		}

		List<List<ItemStack>> inputs = new ArrayList<>();
		for (Ingredient ingredient : shaped.getIngredients()) {
			inputs.add(List.of(ingredient.getItems()));
		}
		gridHelper.setInputs(slots, VanillaTypes.ITEM_STACK, inputs, shaped.getWidth(), shaped.getHeight());

		IRecipeSlotBuilder output = builder.addSlot(RecipeIngredientRole.OUTPUT, START_X + 95 + 1, START_Y + 19 + 1).setOutputSlotBackground();
		for (ItemStack stack : holder.value().outputs()) {
			output.addItemStack(stack);
		}
	}

	@Override
	public void draw(RecipeHolder<RollingMachineRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		RollingMachineRecipe recipe = holder.value();
		JeiDraw.energyBar(graphics, 8 - Layout.OFFSET, 8 - Layout.OFFSET, true);
		JeiDraw.progressBar(graphics, START_X + 68, START_Y + 22, recipe.time() * 50.0, GuiBuilder.ProgressDirection.RIGHT);
		JeiDraw.label(graphics, JeiDraw.seconds(recipe.time()), 150 - 5 - Layout.OFFSET, 5 - Layout.OFFSET, true);
	}

	@Override
	public List<Component> getTooltipStrings(RecipeHolder<RollingMachineRecipe> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
		int x = 8 - Layout.OFFSET;
		int y = 8 - Layout.OFFSET;
		if (mouseX < x || mouseX > x + 14 || mouseY < y || mouseY > y + 50) {
			return List.of();
		}
		RollingMachineRecipe recipe = holder.value();
		return List.of(
			Component.translatable("techreborn.jei.recipe.energy"),
			Component.translatable("techreborn.jei.recipe.running.cost", "E", recipe.power()).withStyle(ChatFormatting.GRAY),
			Component.translatable("techreborn.jei.recipe.generator.total", recipe.power() * recipe.time()).withStyle(ChatFormatting.GRAY)
		);
	}
}
