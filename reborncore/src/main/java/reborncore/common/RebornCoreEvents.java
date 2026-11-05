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
 *
 * NeoForge port (c) 2026 RickWeek, based on the upstream Fabric sources.
 */

package reborncore.common;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import reborncore.api.blockentity.UnloadHandler;
import reborncore.api.items.ArmorBlockEntityTicker;
import reborncore.common.chunkloading.ChunkLoaderManager;
import reborncore.common.crafting.RecipeUtils;
import reborncore.common.multiblock.MultiblockRegistry;
import reborncore.common.misc.RebornCoreTags;
import reborncore.common.powerSystem.RcEnergyItem;

/**
 * Port note: upstream did all of this with mixins on ItemEntity and Player. NeoForge fires events
 * for both, so the mixins are gone -- which also means no mixin config, no access widener, and
 * nothing that can clash with another mod touching the same vanilla classes.
 * <p>
 * The two other upstream mixins went the same way: crafting is PlayerEvent.ItemCraftedEvent (see
 * RebornCore), and the armour damage reduction is LivingDamageEvent, which belongs to whichever mod
 * adds the armour rather than to RebornCore.
 */
public final class RebornCoreEvents {

	public static void register() {
		NeoForge.EVENT_BUS.addListener(RebornCoreEvents::onItemEntityTick);
		NeoForge.EVENT_BUS.addListener(RebornCoreEvents::onPlayerTick);
		NeoForge.EVENT_BUS.addListener(RebornCoreEvents::onLevelTick);
		NeoForge.EVENT_BUS.addListener(RebornCoreEvents::onLevelLoad);
		NeoForge.EVENT_BUS.addListener(RebornCoreEvents::onLevelUnload);
		NeoForge.EVENT_BUS.addListener(RebornCoreEvents::onChunkUnload);
		NeoForge.EVENT_BUS.addListener(RebornCoreEvents::onDatapackSync);
	}

	/**
	 * Fires after a /reload and when a player joins: recipes may have changed, so the cached recipe
	 * lists must go. See {@link RecipeUtils#clearCache()}.
	 */
	private static void onDatapackSync(OnDatapackSyncEvent event) {
		RecipeUtils.clearCache();
	}

	/**
	 * Replaces Fabric's ServerTickEvents.START_WORLD_TICK: drives the multiblock registry and keeps
	 * a world with loaded chunks awake.
	 */
	private static void onLevelTick(LevelTickEvent.Pre event) {
		if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}

		MultiblockRegistry.tickStart(serverLevel);
		ChunkLoaderManager.get(serverLevel).onServerWorldTick(serverLevel);
	}

	/**
	 * Replaces Fabric's ServerWorldEvents.LOAD: re-applies the chunk loader tickets on world load.
	 */
	private static void onLevelLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			ChunkLoaderManager.get(serverLevel).onServerWorldLoad(serverLevel);
		}
	}

	private static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			MultiblockRegistry.onWorldUnloaded(serverLevel);
		}
	}

	/**
	 * Replaces Fabric's ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD and ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.
	 * NeoForge has no per-block-entity unload event, so the chunk's block entities are walked when the
	 * chunk goes away. ChunkEvent.Unload fires on both logical sides, so one listener covers both.
	 */
	private static void onChunkUnload(ChunkEvent.Unload event) {
		// Only a fully loaded LevelChunk holds its block entities; ChunkAccess does not expose them.
		if (!(event.getChunk() instanceof LevelChunk levelChunk)) {
			return;
		}

		for (BlockEntity blockEntity : levelChunk.getBlockEntities().values()) {
			if (blockEntity instanceof UnloadHandler unloadHandler) {
				unloadHandler.onUnload();
			}
		}
	}

	/**
	 * Replaces MixinItemEntity: items tagged water_explosion blow up when they touch water.
	 */
	private static void onItemEntityTick(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
			return;
		}

		Level level = itemEntity.level();

		if (level.isClientSide || !itemEntity.isInWater() || itemEntity.getItem().isEmpty()) {
			return;
		}

		if (itemEntity.getItem().is(RebornCoreTags.WATER_EXPLOSION_ITEM)) {
			level.explode(itemEntity, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 2F, Level.ExplosionInteraction.NONE);
			itemEntity.discard();
		}
	}

	/**
	 * Replaces MixinPlayerEntity: ticks armour pieces that ask to be ticked, telling them whether
	 * the player is wearing a full matching powered suit.
	 */
	private static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();

		if (player.isSpectator()) {
			return;
		}

		if (player.level().isClientSide) {
			// Upstream ticked the stack held on the cursor client side so its model keeps animating.
			ItemStack carried = player.inventoryMenu.getCarried();
			if (carried.getItem() instanceof ArmorBlockEntityTicker ticker) {
				carried.remove(DataComponents.CUSTOM_DATA);
				ticker.tickArmor(carried, false, player);
			}
			return;
		}

		Class<?> suit = null;
		int count = 0;
		for (ItemStack stack : player.getArmorSlots()) {
			if (!(stack.getItem() instanceof RcEnergyItem item)) {
				break;
			}
			if (item.getStoredEnergy(stack) == 0) {
				break;
			}
			if (suit == null) {
				suit = item.getClass();
			} else if (suit != item.getClass()) {
				break;
			}
			count++;
		}

		for (ItemStack stack : player.getArmorSlots()) {
			if (!stack.isEmpty() && stack.getItem() instanceof ArmorBlockEntityTicker ticker) {
				// mark tick
				if (!stack.has(DataComponents.CUSTOM_DATA)) {
					stack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
				}
				ticker.tickArmor(stack, count == 4, player);
			}
		}
	}

	private RebornCoreEvents() {
	}
}
