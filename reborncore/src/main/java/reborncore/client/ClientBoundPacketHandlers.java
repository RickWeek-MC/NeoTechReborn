/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 TeamReborn
 *
 * NeoForge port (c) 2026 RickWeek, based on the upstream Fabric sources.
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

package reborncore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import reborncore.RebornCore;
import reborncore.common.blockentity.FluidConfiguration;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.blockentity.SlotConfiguration;
import reborncore.common.network.clientbound.ChunkSyncPayload;
import reborncore.common.network.clientbound.CustomDescriptionPayload;
import reborncore.common.network.clientbound.FluidConfigSyncPayload;
import reborncore.common.network.clientbound.QueueItemStacksPayload;
import reborncore.common.network.clientbound.ScreenHandlerUpdatePayload;
import reborncore.common.network.clientbound.SlotSyncPayload;
import reborncore.common.screen.BuiltScreenHandler;

/**
 * Client side of the RebornCore network, the counterpart of
 * {@link reborncore.common.network.ServerBoundPackets}.
 * <p>
 * Port notes:
 * <ul>
 * <li>Upstream registered these with ClientPlayNetworking.registerGlobalReceiver from
 *     ClientModInitializer. On NeoForge a payload type has to be registered the same way on both
 *     sides, so the registration lives in the common {@link reborncore.common.network.Packets} and
 *     only the handler bodies are here.</li>
 * <li>Every handler goes through {@code context.enqueueWork}: NeoForge calls handlers on the
 *     network thread, Fabric called them on the client thread. Upstream only did this for the slot
 *     sync (MinecraftClient.execute), the other five were already racy there.</li>
 * <li>This class is loaded lazily, on the first packet. Nothing on a dedicated server ever reaches
 *     it, which is why {@code Packets} wraps the calls in explicit lambdas instead of using method
 *     references -- see the comment there.</li>
 * </ul>
 */
public class ClientBoundPacketHandlers {

	/**
	 * Full block entity NBT pushed by MachineBaseBlockEntity.syncWithAll.
	 */
	public static void handleCustomDescription(CustomDescriptionPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = Minecraft.getInstance().level;
			// Port note: upstream dereferenced the world without checking it, which crashed when a
			// packet arrived while leaving a world.
			if (level == null || !level.hasChunkAt(payload.pos())) {
				return;
			}

			BlockEntity blockEntity = level.getBlockEntity(payload.pos());
			if (blockEntity != null && payload.nbt() != null) {
				blockEntity.loadWithComponents(payload.nbt(), level.registryAccess());
			}
		});
	}

	/**
	 * Per-side fluid IO configuration of a machine.
	 */
	public static void handleFluidConfigSync(FluidConfigSyncPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = Minecraft.getInstance().level;
			if (level == null || !level.hasChunkAt(payload.pos())) {
				return;
			}

			FluidConfiguration incoming = payload.fluidConfiguration();
			// Port note: upstream cast the block entity blindly, so a packet landing one tick after the
			// machine was replaced threw a ClassCastException.
			if (!(level.getBlockEntity(payload.pos()) instanceof MachineBaseBlockEntity machine)
				|| machine.fluidConfiguration == null || incoming == null) {
				RebornCore.LOGGER.error("Failed to sync fluid config data to {}", payload.pos());
				return;
			}

			incoming.getAllSides().forEach(machine.fluidConfiguration::updateFluidConfig);
			machine.fluidConfiguration.setInput(incoming.autoInput());
			machine.fluidConfiguration.setOutput(incoming.autoOutput());
		});
	}

	/**
	 * Per-side slot IO configuration of a machine.
	 */
	public static void handleSlotSync(SlotSyncPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = Minecraft.getInstance().level;
			if (level == null || !level.hasChunkAt(payload.pos())) {
				return;
			}

			SlotConfiguration incoming = payload.slotConfig();
			if (!(level.getBlockEntity(payload.pos()) instanceof MachineBaseBlockEntity machine)
				|| machine.getSlotConfiguration() == null || incoming == null || incoming.getSlotDetails() == null) {
				RebornCore.LOGGER.error("Failed to sync slot data to {}", payload.pos());
				return;
			}

			incoming.getSlotDetails().forEach(machine.getSlotConfiguration()::updateSlotDetails);
		});
	}

	/**
	 * Values of the open screen, sent every tick by BuiltScreenHandler.sendContentUpdates.
	 * This is what keeps energy bars, progress bars and tanks moving.
	 */
	public static void handleScreenHandlerUpdate(ScreenHandlerUpdatePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> containerScreen
				&& containerScreen.getMenu() instanceof BuiltScreenHandler builtScreenHandler) {
				builtScreenHandler.applyScreenHandlerData(payload.data());
			}
		});
	}

	/**
	 * Chunks held by a chunk loader, for the debug overlay.
	 */
	public static void handleChunkSync(ChunkSyncPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientChunkManager.setLoadedChunks(payload.chunks()));
	}

	/**
	 * Stacks the server wants rendered off-screen (used for dynamic cell and armor textures).
	 */
	public static void handleQueueItemStacks(QueueItemStacksPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> ItemStackRenderManager.RENDER_QUEUE.addAll(payload.stacks()));
	}
}
