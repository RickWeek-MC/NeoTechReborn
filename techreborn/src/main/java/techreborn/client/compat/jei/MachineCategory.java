package techreborn.client.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.ChatFormatting;
import reborncore.common.crafting.RebornFluidRecipe;
import reborncore.common.crafting.RebornRecipe;
import reborncore.common.crafting.SizedIngredient;
import reborncore.common.fluid.container.FluidInstance;
import techreborn.recipe.recipes.BlastFurnaceRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * One JEI category for every machine recipe type. What differs between machines is only the
 * {@link Layout}; upstream had a class per layout, which is what this replaces.
 */
public class MachineCategory<R extends RebornRecipe> implements IRecipeCategory<RecipeHolder<R>> {
	private final RecipeType<RecipeHolder<R>> type;
	private final Component title;
	private final IDrawable icon;
	private final Layout layout;

	public MachineCategory(RecipeType<RecipeHolder<R>> type, Component title, IDrawable icon, Layout layout) {
		this.type = type;
		this.title = title;
		this.icon = icon;
		this.layout = layout;
	}

	@Override
	public RecipeType<RecipeHolder<R>> getRecipeType() {
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
		return layout.width;
	}

	@Override
	public int getHeight() {
		return layout.height;
	}

	@Override
	public ResourceLocation getRegistryName(RecipeHolder<R> recipe) {
		return recipe.id();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<R> holder, IFocusGroup focuses) {
		R recipe = holder.value();
		List<SizedIngredient> ingredients = recipe.ingredients();
		List<ItemStack> outputs = recipe.outputs();

		for (Layout.ItemSlot slot : layout.slots) {
			int x = slot.x() + 1 - Layout.OFFSET;
			int y = slot.y() + 1 - Layout.OFFSET;
			if (slot.output()) {
				IRecipeSlotBuilder s = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y);
				if (slot.bigResult()) {
					s.setOutputSlotBackground();
				} else {
					s.setStandardSlotBackground();
				}
				if (slot.index() < outputs.size()) {
					s.addItemStack(outputs.get(slot.index()));
				}
			} else {
				IRecipeSlotBuilder s = builder.addSlot(RecipeIngredientRole.INPUT, x, y).setStandardSlotBackground();
				if (slot.index() < ingredients.size()) {
					s.addItemStacks(ingredients.get(slot.index()).getPreviewStacks());
				}
			}
		}

		if (layout.tank != null && recipe instanceof RebornFluidRecipe fluidRecipe) {
			FluidInstance fluid = fluidRecipe.fluid();
			if (fluid != null && !fluid.isEmpty()) {
				long amount = fluid.toFluidStack().getAmount();
				int x = layout.tank.x() - Layout.OFFSET;
				int y = layout.tank.y() - Layout.OFFSET;
				builder.addSlot(layout.tank.output() ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT, x, y)
					.addFluidStack(fluid.fluid(), amount)
					.setFluidRenderer(Math.max(amount, 1), false, 16, 50)
					.setBackground(JeiDraw.drawable(22, 56, (g, bx, by) -> JeiDraw.tankBackground(g, bx + 4, by + 4)), -4, -4)
					.setOverlay(JeiDraw.drawable(16, 50, (g, ox, oy) -> JeiDraw.tankForeground(g, ox, oy)), 0, 0);
			}
		}
	}

	@Override
	public void draw(RecipeHolder<R> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		R recipe = holder.value();
		if (layout.energy) {
			JeiDraw.energyBar(graphics, 8 - Layout.OFFSET, layout.energyY - Layout.OFFSET, true);
		}
		for (Layout.Bar bar : layout.bars) {
			JeiDraw.progressBar(graphics, bar.x() - Layout.OFFSET, bar.y() - Layout.OFFSET, recipe.time() * 50.0, bar.direction());
		}
		JeiDraw.label(graphics, JeiDraw.seconds(recipe.time()), layout.labelX - Layout.OFFSET, layout.labelY - Layout.OFFSET, layout.labelRight);
		if (layout.showHeat && recipe instanceof BlastFurnaceRecipe blast) {
			Component heat = Component.literal(String.valueOf(blast.getHeat())).append(" ").append(Component.translatable("techreborn.jei.recipe.heat"));
			JeiDraw.label(graphics, heat, 150 - 5 - Layout.OFFSET, 5 - Layout.OFFSET, true);
		}
	}

	@Override
	public List<Component> getTooltipStrings(RecipeHolder<R> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (!layout.energy) {
			return List.of();
		}
		int x = 8 - Layout.OFFSET;
		int y = layout.energyY - Layout.OFFSET;
		if (mouseX < x || mouseX > x + 14 || mouseY < y || mouseY > y + 50) {
			return List.of();
		}
		R recipe = holder.value();
		List<Component> list = new ArrayList<>();
		list.add(Component.translatable("techreborn.jei.recipe.energy"));
		list.add(Component.translatable("techreborn.jei.recipe.running.cost", "E", recipe.power()).withStyle(ChatFormatting.GRAY));
		list.add(Component.translatable("techreborn.jei.recipe.generator.total", recipe.power() * recipe.time()).withStyle(ChatFormatting.GRAY));
		return list;
	}
}
