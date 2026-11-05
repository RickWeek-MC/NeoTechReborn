package techreborn.client.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import reborncore.client.gui.GuiBuilder;
import techreborn.recipe.recipes.FluidGeneratorRecipe;

import java.util.List;

/** One category per fluid generator: the fuel on the left, the total energy it gives on the right. */
public class FluidGeneratorCategory implements IRecipeCategory<RecipeHolder<FluidGeneratorRecipe>> {
	private static final int WIDTH = 150 - 2 * Layout.OFFSET;
	private static final int HEIGHT = 66 - 2 * Layout.OFFSET;
	private static final int FLUID_X = 16 - Layout.OFFSET;
	private static final int ENERGY_X = 108 - Layout.OFFSET;
	private static final int TOP = 8 - Layout.OFFSET;

	private final RecipeType<RecipeHolder<FluidGeneratorRecipe>> type;
	private final Component title;
	private final IDrawable icon;

	public FluidGeneratorCategory(RecipeType<RecipeHolder<FluidGeneratorRecipe>> type, Component title, IDrawable icon) {
		this.type = type;
		this.title = title;
		this.icon = icon;
	}

	@Override
	public RecipeType<RecipeHolder<FluidGeneratorRecipe>> getRecipeType() {
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
	public ResourceLocation getRegistryName(RecipeHolder<FluidGeneratorRecipe> recipe) {
		return recipe.id();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FluidGeneratorRecipe> holder, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, FLUID_X, TOP)
			.addFluidStack(holder.value().getFluid(), 1000)
			.setFluidRenderer(1000, false, 16, 50)
			.setBackground(JeiDraw.drawable(22, 56, (g, x, y) -> JeiDraw.tankBackground(g, x + 4, y + 4)), -4, -4)
			.setOverlay(JeiDraw.drawable(16, 50, (g, x, y) -> JeiDraw.tankForeground(g, x, y)), 0, 0);
	}

	@Override
	public void draw(RecipeHolder<FluidGeneratorRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		JeiDraw.energyBar(graphics, ENERGY_X, TOP, false);
		JeiDraw.progressBar(graphics, 76 - 16 - Layout.OFFSET, 48 - 19 - Layout.OFFSET, 5000, GuiBuilder.ProgressDirection.RIGHT);
	}

	@Override
	public List<Component> getTooltipStrings(RecipeHolder<FluidGeneratorRecipe> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (mouseX < ENERGY_X || mouseX > ENERGY_X + 14 || mouseY < TOP || mouseY > TOP + 50) {
			return List.of();
		}
		return List.of(
			Component.translatable("techreborn.jei.recipe.energy"),
			Component.translatable("techreborn.jei.recipe.generator.total", holder.value().power() * 1000).withStyle(ChatFormatting.GRAY)
		);
	}
}
