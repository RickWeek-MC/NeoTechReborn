package techreborn.client.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import reborncore.client.gui.GuiBase;
import reborncore.client.gui.GuiSprites;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JEI icon in the corner of a machine's screen that opens the recipes of that machine.
 * Done with screen events so it works for every RebornCore screen without touching them.
 */
final class GuiRecipeButton {
	private static final int SIZE = 16;
	private static final Map<Block, List<RecipeType<?>>> TYPES = new HashMap<>();
	private static IJeiRuntime runtime;
	private static boolean listening;

	private GuiRecipeButton() {
	}

	static void register(Block machine, RecipeType<?> type) {
		TYPES.computeIfAbsent(machine, b -> new java.util.ArrayList<>());
		if (!TYPES.get(machine).contains(type)) {
			TYPES.get(machine).add(type);
		}
	}

	static void clear() {
		TYPES.clear();
	}

	static void setRuntime(IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
		if (!listening) {
			listening = true;
			NeoForge.EVENT_BUS.addListener(GuiRecipeButton::onRender);
			NeoForge.EVENT_BUS.addListener(GuiRecipeButton::onClick);
		}
	}

	static void unsetRuntime() {
		runtime = null;
	}

	private static List<RecipeType<?>> typesFor(Screen screen) {
		if (runtime == null || !(screen instanceof GuiBase<?> gui)) {
			return List.of();
		}
		BlockEntity be = gui.be;
		if (be == null) {
			return List.of();
		}
		return TYPES.getOrDefault(be.getBlockState().getBlock(), List.of());
	}

	private static int buttonX(GuiBase<?> gui) {
		return gui.getGuiLeft() + gui.getXSize() - SIZE - 4;
	}

	private static int buttonY(GuiBase<?> gui) {
		return gui.getGuiTop() + 4;
	}

	private static boolean over(GuiBase<?> gui, double mouseX, double mouseY) {
		int x = buttonX(gui);
		int y = buttonY(gui);
		return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
	}

	private static void onRender(ScreenEvent.Render.Post event) {
		Screen screen = event.getScreen();
		if (typesFor(screen).isEmpty()) {
			return;
		}
		GuiBase<?> gui = (GuiBase<?>) screen;
		var graphics = event.getGuiGraphics();
		GuiSprites.drawSprite(graphics, GuiSprites.JEI_ICON, buttonX(gui), buttonY(gui), SIZE, SIZE);
		if (over(gui, event.getMouseX(), event.getMouseY())) {
			graphics.renderTooltip(gui.getMinecraft().font, Component.translatable("jei.tooltip.show.recipes"), event.getMouseX(), event.getMouseY());
		}
	}

	private static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
		Screen screen = event.getScreen();
		List<RecipeType<?>> types = typesFor(screen);
		if (types.isEmpty() || event.getButton() != 0) {
			return;
		}
		if (over((GuiBase<?>) screen, event.getMouseX(), event.getMouseY())) {
			runtime.getRecipesGui().showTypes(types);
			event.setCanceled(true);
		}
	}
}
