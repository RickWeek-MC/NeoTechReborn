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

package reborncore;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by Gigabit101 on 16/08/2016.
 * <p>
 * Port note: NeoForge registries are frozen outside of {@link RegisterEvent}, so the upstream
 * "register right now" API is kept but the entries are queued and flushed during that event.
 * Call {@link #init(IEventBus)} once from the mod constructor BEFORE anything registers.
 * <p>
 * READ THIS BEFORE REGISTERING ANYTHING. Queueing the registration is not enough on its own:
 * {@code Block}, {@code Item} and {@code Fluid} each grab an intrusive holder from their registry
 * <i>in their own constructor</i>, so merely calling {@code new SomeBlock(...)} while the registries
 * are frozen throws "Registry is already frozen". Mod constructors run before NeoForge unfreezes
 * anything, so content cannot be built there -- which is exactly what upstream does, because Fabric
 * never freezes these registries.
 * <p>
 * The fix is {@link #addContentLoader(Runnable)}: hand over a callback that builds the content, and
 * it runs inside the registration window, where constructing blocks and items is legal. The mod
 * constructor then only registers the callback.
 */
public class RebornRegistry {
	//Yeah, this is horrible
	private static final HashMap<Object, ResourceLocation> objIdentMap = new HashMap<>();

	private record PendingBlock(ResourceLocation name, Block block, Function<Block, BlockItem> itemFactory) { }

	private record PendingItem(ResourceLocation name, Item item) { }

	private static final List<PendingBlock> PENDING_BLOCKS = new ArrayList<>();
	private static final List<PendingItem> PENDING_ITEMS = new ArrayList<>();
	private static final List<Runnable> CONTENT_LOADERS = new ArrayList<>();
	private static boolean flushed = false;
	private static boolean contentLoaded = false;

	/**
	 * NeoForge moves these three to the front of the registration order because items are built from
	 * them (see GameData.getRegistrationOrder). Content loaders must therefore run after these and
	 * before fluid, block and item, which in vanilla order come next -- fluid first, then block, then
	 * item. Skipping these three lands the callbacks on the first registry after them.
	 */
	private static final Set<ResourceKey<? extends Registry<?>>> BUILT_BEFORE_CONTENT = Set.of(
		Registries.ATTRIBUTE,
		Registries.DATA_COMPONENT_TYPE,
		Registries.ARMOR_MATERIAL
	);

	/**
	 * Hooks the queue into the NeoForge registry event. Must be called from the mod constructor.
	 */
	public static void init(IEventBus modEventBus) {
		modEventBus.addListener(RebornRegistry::onRegister);
	}

	private static void onRegister(RegisterEvent event) {
		if (!BUILT_BEFORE_CONTENT.contains(event.getRegistryKey())) {
			ensureContentLoaded();
		}

		event.register(Registries.BLOCK, helper -> {
			for (PendingBlock pending : PENDING_BLOCKS) {
				helper.register(pending.name(), pending.block());
			}
		});

		event.register(Registries.ITEM, helper -> {
			for (PendingBlock pending : PENDING_BLOCKS) {
				if (pending.itemFactory() != null) {
					helper.register(pending.name(), pending.itemFactory().apply(pending.block()));
				}
			}

			for (PendingItem pending : PENDING_ITEMS) {
				helper.register(pending.name(), pending.item());
			}

			flushed = true;
		});
	}

	/**
	 * Registers a callback that builds this mod's blocks, items and fluids.
	 * <p>
	 * Call it from the mod constructor; the callback itself runs later, once NeoForge has unfrozen
	 * the registries, which is the only point where constructing a Block, Item or Fluid is allowed.
	 * Everything the callback registers -- through this class or through RebornFluidManager -- is
	 * queued and flushed by the registry events that follow.
	 * <p>
	 * This is how TechReborn has to load TRContent: not from its mod constructor, but from here.
	 */
	public static void addContentLoader(Runnable loader) {
		checkOpen();
		CONTENT_LOADERS.add(loader);
	}

	private static void ensureContentLoaded() {
		if (contentLoaded) {
			return;
		}
		contentLoaded = true;

		for (Runnable loader : CONTENT_LOADERS) {
			loader.run();
		}
	}

	private static void checkOpen() {
		if (flushed) {
			throw new IllegalStateException("RebornRegistry is closed, registration must happen during mod construction");
		}
	}

	/**
	 * Registers {@link Block} and {@link BlockItem} in vanilla registries
	 *
	 * @param block   {@link Block} Block to register
	 * @param builder {@link Item.Properties} Settings builder for {@link BlockItem}
	 * @param name    {@link ResourceLocation} Registry name for block and item
	 */
	public static void registerBlock(Block block, Item.Properties builder, ResourceLocation name) {
		registerBlock(block, b -> new BlockItem(b, builder), name);
	}

	public static void registerBlock(Block block, Function<Block, BlockItem> blockItemFunction, ResourceLocation name) {
		checkOpen();
		PENDING_BLOCKS.add(new PendingBlock(name, block, blockItemFunction));
	}

	/**
	 * Registers Block and BlockItem in vanilla registries.
	 * Block should have registered identifier in RebornRegistry via {@link #registerIdent registerIdent} method
	 */
	public static void registerBlock(Block block, Item.Properties itemGroup) {
		Validate.isTrue(objIdentMap.containsKey(block));
		registerBlock(block, itemGroup, objIdentMap.get(block));
	}

	public static void registerBlock(Block block, Function<Block, BlockItem> blockItemFunction){
		Validate.isTrue(objIdentMap.containsKey(block));
		registerBlock(block, blockItemFunction, objIdentMap.get(block));
	}

	/**
	 * Register only {@link Block}, without {@link BlockItem} in vanilla registries
	 */
	public static void registerBlockNoItem(Block block) {
		Validate.isTrue(objIdentMap.containsKey(block));
		checkOpen();
		PENDING_BLOCKS.add(new PendingBlock(objIdentMap.get(block), block, null));
	}


	/**
	 * Register {@link Item} in vanilla registries
	 */
	public static void registerItem(Item item, ResourceLocation name) {
		checkOpen();
		PENDING_ITEMS.add(new PendingItem(name, item));
	}

	/**
	 * Register {@link Item} in vanilla registries, using the identifier registered via registerIdent.
	 */
	public static void registerItem(Item item){
		Validate.isTrue(objIdentMap.containsKey(item));
		registerItem(item, objIdentMap.get(item));
	}

	/**
	 * Registers {@link ResourceLocation} in internal RebornCore map
	 */
	public static void registerIdent(Object object, ResourceLocation identifier){
		objIdentMap.put(object, identifier);
	}
}
