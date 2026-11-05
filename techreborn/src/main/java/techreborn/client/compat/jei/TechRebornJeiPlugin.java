package techreborn.client.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import reborncore.common.powerSystem.RcEnergyItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import reborncore.api.blockentity.IUpgradeable;
import reborncore.client.gui.GuiBase;
import reborncore.client.gui.config.GuiTab;
import reborncore.client.gui.config.SlotConfigGui;
import reborncore.client.gui.config.elements.ConfigSlotElement;
import reborncore.common.crafting.RebornRecipe;
import techreborn.TechReborn;
import techreborn.component.TRDataComponentTypes;
import techreborn.init.ModRecipes;
import techreborn.init.TRContent;
import techreborn.init.TRContent.Machine;
import techreborn.recipe.recipes.FluidGeneratorRecipe;
import techreborn.recipe.recipes.RollingMachineRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI integration, replacing the REI plugin upstream ships. JEI is optional: this class is only
 * loaded by JEI itself (through the {@link JeiPlugin} annotation), so the mod runs fine without it.
 * <p>
 * Port note: where REI had a category class per layout, there is one generic {@link MachineCategory}
 * driven by a {@link Layout}; the rolling machine (shaped grid) and the fluid generators have their own.
 */
@JeiPlugin
public class TechRebornJeiPlugin implements IModPlugin {
	private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, "jei");

	/** A registered machine category: its recipe type and the machines that craft it. */
	private record Entry<R extends RebornRecipe>(
		net.minecraft.world.item.crafting.RecipeType<R> vanillaType,
		RecipeType<RecipeHolder<R>> jeiType,
		ItemLike[] catalysts
	) {
	}

	private final List<Entry<?>> entries = new ArrayList<>();
	private final List<GeneratorEntry> generators = new ArrayList<>();

	private record GeneratorEntry(
		net.minecraft.world.item.crafting.RecipeType<FluidGeneratorRecipe> vanillaType,
		RecipeType<RecipeHolder<FluidGeneratorRecipe>> jeiType,
		Machine machine
	) {
	}

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		// A cell is the same item whatever it holds; tell JEI the fluid is part of its identity.
		registration.registerSubtypeInterpreter(TRContent.CELL, new ISubtypeInterpreter<ItemStack>() {
			@Override
			public Object getSubtypeData(ItemStack stack, UidContext context) {
				Holder<Fluid> fluid = stack.get(TRDataComponentTypes.FLUID);
				return fluid == null ? null : fluid.value();
			}

			@Override
			public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
				Holder<Fluid> fluid = stack.get(TRDataComponentTypes.FLUID);
				return fluid == null ? "" : String.valueOf(fluid.unwrapKey().map(k -> k.location().toString()).orElse(""));
			}
		});

		// Powered items show up empty and charged in the creative tab. They are the same item, so without
		// this JEI warns about duplicates; "has charge or not" is the only difference that matters.
		ISubtypeInterpreter<ItemStack> charge = new ISubtypeInterpreter<ItemStack>() {
			@Override
			public Object getSubtypeData(ItemStack stack, UidContext context) {
				return ((RcEnergyItem) stack.getItem()).getStoredEnergy(stack) > 0 ? "charged" : "empty";
			}

			@Override
			public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
				return ((RcEnergyItem) stack.getItem()).getStoredEnergy(stack) > 0 ? "charged" : "empty";
			}
		};
		for (var item : BuiltInRegistries.ITEM) {
			if (item instanceof RcEnergyItem && TechReborn.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
				registration.registerSubtypeInterpreter(item, charge);
			}
		}
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
		entries.clear();
		generators.clear();

		machine(registration, ModRecipes.ALLOY_SMELTER, Machine.ALLOY_SMELTER, Layout.twoInCenterOut(), Machine.ALLOY_SMELTER, Machine.IRON_ALLOY_FURNACE);
		machine(registration, ModRecipes.ASSEMBLING_MACHINE, Machine.ASSEMBLY_MACHINE, Layout.assembling(), Machine.ASSEMBLY_MACHINE);
		machine(registration, ModRecipes.BLAST_FURNACE, Machine.INDUSTRIAL_BLAST_FURNACE, Layout.blastFurnace(), Machine.INDUSTRIAL_BLAST_FURNACE);
		machine(registration, ModRecipes.CENTRIFUGE, Machine.INDUSTRIAL_CENTRIFUGE, Layout.centrifuge(), Machine.INDUSTRIAL_CENTRIFUGE);
		machine(registration, ModRecipes.CHEMICAL_REACTOR, Machine.CHEMICAL_REACTOR, Layout.twoInCenterOut(), Machine.CHEMICAL_REACTOR);
		machine(registration, ModRecipes.COMPRESSOR, Machine.COMPRESSOR, Layout.oneInOneOut(), Machine.COMPRESSOR);
		machine(registration, ModRecipes.DISTILLATION_TOWER, Machine.DISTILLATION_TOWER, Layout.distillation(), Machine.DISTILLATION_TOWER);
		machine(registration, ModRecipes.EXTRACTOR, Machine.EXTRACTOR, Layout.oneInOneOut(), Machine.EXTRACTOR);
		machine(registration, ModRecipes.FLUID_REPLICATOR, Machine.FLUID_REPLICATOR, Layout.fluidReplicator(), Machine.FLUID_REPLICATOR);
		machine(registration, ModRecipes.FUSION_REACTOR, Machine.FUSION_CONTROL_COMPUTER, Layout.twoInCenterOut(), Machine.FUSION_CONTROL_COMPUTER);
		machine(registration, ModRecipes.GRINDER, Machine.GRINDER, Layout.oneInOneOut(), Machine.GRINDER);
		machine(registration, ModRecipes.IMPLOSION_COMPRESSOR, Machine.IMPLOSION_COMPRESSOR, Layout.implosion(), Machine.IMPLOSION_COMPRESSOR);
		machine(registration, ModRecipes.INDUSTRIAL_ELECTROLYZER, Machine.INDUSTRIAL_ELECTROLYZER, Layout.electrolyzer(), Machine.INDUSTRIAL_ELECTROLYZER);
		machine(registration, ModRecipes.INDUSTRIAL_GRINDER, Machine.INDUSTRIAL_GRINDER, Layout.industrialGrinder(), Machine.INDUSTRIAL_GRINDER);
		machine(registration, ModRecipes.INDUSTRIAL_SAWMILL, Machine.INDUSTRIAL_SAWMILL, Layout.sawmill(), Machine.INDUSTRIAL_SAWMILL);
		machine(registration, ModRecipes.SOLID_CANNING_MACHINE, Machine.SOLID_CANNING_MACHINE, Layout.twoInCenterOut(), Machine.SOLID_CANNING_MACHINE);
		machine(registration, ModRecipes.VACUUM_FREEZER, Machine.VACUUM_FREEZER, Layout.oneInOneOut(), Machine.VACUUM_FREEZER);
		machine(registration, ModRecipes.WIRE_MILL, Machine.WIRE_MILL, Layout.oneInOneOut(), Machine.WIRE_MILL);
		// The scrapbox has no machine: its icon is the box itself, and nothing crafts it.
		machine(registration, ModRecipes.SCRAPBOX, TRContent.SCRAP_BOX, Layout.oneInOneOut());

		// Rolling machine: shaped grid.
		RecipeType<RecipeHolder<RollingMachineRecipe>> rolling = RecipeType.createFromVanilla(ModRecipes.ROLLING_MACHINE);
		registration.addRecipeCategories(new RollingMachineCategory(rolling, title(ModRecipes.ROLLING_MACHINE),
			gui.createDrawableItemLike(Machine.ROLLING_MACHINE), gui.createCraftingGridHelper()));

		generator(registration, ModRecipes.THERMAL_GENERATOR, Machine.THERMAL_GENERATOR);
		generator(registration, ModRecipes.GAS_GENERATOR, Machine.GAS_TURBINE);
		generator(registration, ModRecipes.DIESEL_GENERATOR, Machine.DIESEL_GENERATOR);
		generator(registration, ModRecipes.SEMI_FLUID_GENERATOR, Machine.SEMI_FLUID_GENERATOR);
		generator(registration, ModRecipes.PLASMA_GENERATOR, Machine.PLASMA_GENERATOR);
	}

	private static Component title(net.minecraft.world.item.crafting.RecipeType<?> type) {
		// The lang file keys these by the recipe type id, with the colon: "techreborn:alloy_smelter".
		return Component.translatable(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(type).toString());
	}

	private <R extends RebornRecipe> void machine(IRecipeCategoryRegistration registration,
												net.minecraft.world.item.crafting.RecipeType<R> vanillaType,
												ItemLike icon, Layout layout, ItemLike... catalysts) {
		IDrawable iconDrawable = registration.getJeiHelpers().getGuiHelper().createDrawableItemLike(icon);
		RecipeType<RecipeHolder<R>> jeiType = RecipeType.createFromVanilla(vanillaType);
		registration.addRecipeCategories(new MachineCategory<>(jeiType, title(vanillaType), iconDrawable, layout));
		entries.add(new Entry<>(vanillaType, jeiType, catalysts));
	}

	private void generator(IRecipeCategoryRegistration registration,
						   net.minecraft.world.item.crafting.RecipeType<FluidGeneratorRecipe> vanillaType, Machine machine) {
		IDrawable iconDrawable = registration.getJeiHelpers().getGuiHelper().createDrawableItemLike(machine);
		RecipeType<RecipeHolder<FluidGeneratorRecipe>> jeiType = RecipeType.createFromVanilla(vanillaType);
		// Lang key is the machine name ("techreborn:gas_turbine"), not the recipe type id (gas_generator).
		Component title = Component.translatable(TechReborn.MOD_ID + ":" + machine.name);
		registration.addRecipeCategories(new FluidGeneratorCategory(jeiType, title, iconDrawable));
		generators.add(new GeneratorEntry(vanillaType, jeiType, machine));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		var level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		var manager = level.getRecipeManager();
		for (Entry<?> entry : entries) {
			addAll(registration, manager, entry);
		}
		addAll(registration, manager, ModRecipes.ROLLING_MACHINE, RecipeType.<RollingMachineRecipe>createFromVanilla(ModRecipes.ROLLING_MACHINE));
		for (GeneratorEntry entry : generators) {
			addAll(registration, manager, entry.vanillaType(), entry.jeiType());
		}
	}

	private <R extends RebornRecipe> void addAll(IRecipeRegistration registration, net.minecraft.world.item.crafting.RecipeManager manager, Entry<R> entry) {
		addAll(registration, manager, entry.vanillaType(), entry.jeiType());
	}

	private <R extends RebornRecipe> void addAll(IRecipeRegistration registration, net.minecraft.world.item.crafting.RecipeManager manager,
												 net.minecraft.world.item.crafting.RecipeType<R> vanillaType, RecipeType<RecipeHolder<R>> jeiType) {
		registration.addRecipes(jeiType, manager.getAllRecipesFor(vanillaType));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		// Every catalyst machine also gets the recipe button in its own screen (GuiRecipeButton).
		GuiRecipeButton.clear();
		for (Entry<?> entry : entries) {
			for (ItemLike catalyst : entry.catalysts()) {
				registration.addRecipeCatalyst(catalyst, entry.jeiType());
				GuiRecipeButton.register(Block.byItem(catalyst.asItem()), entry.jeiType());
			}
		}
		RecipeType<RecipeHolder<RollingMachineRecipe>> rolling = RecipeType.createFromVanilla(ModRecipes.ROLLING_MACHINE);
		registration.addRecipeCatalyst(Machine.ROLLING_MACHINE, rolling);
		GuiRecipeButton.register(Block.byItem(Machine.ROLLING_MACHINE.asItem()), rolling);
		for (GeneratorEntry entry : generators) {
			registration.addRecipeCatalyst(entry.machine(), entry.jeiType());
			GuiRecipeButton.register(Block.byItem(entry.machine().asItem()), entry.jeiType());
		}
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		GuiRecipeButton.setRuntime(runtime);
	}

	@Override
	public void onRuntimeUnavailable() {
		GuiRecipeButton.unsetRuntime();
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		// Keep JEI's item list from covering the side tabs and the open slot configuration of RebornCore machines.
		registration.addGenericGuiContainerHandler(GuiBase.class, new IGuiContainerHandler<GuiBase<?>>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(GuiBase<?> gui) {
				List<Rect2i> areas = new ArrayList<>();

				int height = 0;
				if (gui.tryAddUpgrades() && gui.be instanceof IUpgradeable upgradeable && upgradeable.canBeUpgraded()) {
					height = 80;
				}
				for (GuiTab tab : gui.getTabs()) {
					if (tab.enabled()) {
						height += 24;
					}
				}
				if (height > 0) {
					int width = 20;
					areas.add(new Rect2i(gui.getGuiLeft() - width, gui.getGuiTop() + 8, width, height));
				}

				if (gui.getSelectedTab() instanceof SlotConfigGui slotConfigGui) {
					ConfigSlotElement element = slotConfigGui.getSelectedSlot();
					if (element != null) {
						int slotX = element.getX() + gui.getGuiLeft() - 50;
						if (element.getWidth() + slotX > gui.getScreenWidth()) {
							int slotY = element.getY() + gui.getGuiTop() + 25;
							int x = gui.getScreenWidth() + gui.getGuiLeft();
							int y = slotY + gui.getGuiTop() - (element.getHeight() / 2);
							int width = element.getWidth() + slotX - gui.getScreenWidth() + 15;
							areas.add(new Rect2i(x, y, width, element.getHeight()));
						}
					}
				}
				return areas;
			}
		});
	}
}
