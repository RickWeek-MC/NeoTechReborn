/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 TeamReborn
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

package reborncore.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import reborncore.client.ClientBoundPacketHandlers;
import reborncore.common.network.clientbound.ChunkSyncPayload;
import reborncore.common.network.clientbound.CustomDescriptionPayload;
import reborncore.common.network.clientbound.FluidConfigSyncPayload;
import reborncore.common.network.clientbound.QueueItemStacksPayload;
import reborncore.common.network.clientbound.ScreenHandlerUpdatePayload;
import reborncore.common.network.clientbound.SlotSyncPayload;
import reborncore.common.network.serverbound.ChunkLoaderRequestPayload;
import reborncore.common.network.serverbound.FluidConfigSavePayload;
import reborncore.common.network.serverbound.FluidIoSavePayload;
import reborncore.common.network.serverbound.IoSavePayload;
import reborncore.common.network.serverbound.SetRedstoneStatePayload;
import reborncore.common.network.serverbound.SlotConfigSavePayload;
import reborncore.common.network.serverbound.SlotSavePayload;

/**
 * Port note: Fabric registered the payload type and its handler separately (PayloadTypeRegistry
 * plus ServerPlayNetworking). NeoForge wants both at once, during RegisterPayloadHandlersEvent, so
 * upstream's Packets and ServerBoundPackets are wired together here.
 * <p>
 * Both directions have to be registered on both sides, otherwise the sender cannot encode the
 * payload at all. The handler bodies live apart: {@link ServerBoundPackets} for the server,
 * {@link ClientBoundPacketHandlers} for the client.
 */
public class Packets {

	public static void register(RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");

		clientbound(registrar);
		serverbound(registrar);
	}

	/**
	 * Port note: these are explicit lambdas on purpose, not method references like the serverbound
	 * ones below. A method reference makes the JVM link ClientBoundPacketHandlers when the call site
	 * is first executed, which is during registration -- on a dedicated server as well, where the
	 * client classes that handler touches are not present. Inside a lambda body the call is resolved
	 * only when the lambda actually runs, and a playToClient handler never runs on a server.
	 */
	private static void clientbound(PayloadRegistrar registrar) {
		registrar.playToClient(ChunkSyncPayload.ID, ChunkSyncPayload.PACKET_CODEC,
			(payload, context) -> ClientBoundPacketHandlers.handleChunkSync(payload, context));
		registrar.playToClient(CustomDescriptionPayload.ID, CustomDescriptionPayload.PACKET_CODEC,
			(payload, context) -> ClientBoundPacketHandlers.handleCustomDescription(payload, context));
		registrar.playToClient(FluidConfigSyncPayload.ID, FluidConfigSyncPayload.PACKET_CODEC,
			(payload, context) -> ClientBoundPacketHandlers.handleFluidConfigSync(payload, context));
		registrar.playToClient(QueueItemStacksPayload.ID, QueueItemStacksPayload.PACKET_CODEC,
			(payload, context) -> ClientBoundPacketHandlers.handleQueueItemStacks(payload, context));
		registrar.playToClient(ScreenHandlerUpdatePayload.ID, ScreenHandlerUpdatePayload.PACKET_CODEC,
			(payload, context) -> ClientBoundPacketHandlers.handleScreenHandlerUpdate(payload, context));
		registrar.playToClient(SlotSyncPayload.ID, SlotSyncPayload.PACKET_CODEC,
			(payload, context) -> ClientBoundPacketHandlers.handleSlotSync(payload, context));
	}

	private static void serverbound(PayloadRegistrar registrar) {
		registrar.playToServer(ChunkLoaderRequestPayload.ID, ChunkLoaderRequestPayload.PACKET_CODEC, ServerBoundPackets::handleChunkLoaderRequest);
		registrar.playToServer(FluidConfigSavePayload.ID, FluidConfigSavePayload.PACKET_CODEC, ServerBoundPackets::handleFluidConfigSave);
		registrar.playToServer(FluidIoSavePayload.ID, FluidIoSavePayload.PACKET_CODEC, ServerBoundPackets::handleFluidIoSave);
		registrar.playToServer(IoSavePayload.ID, IoSavePayload.PACKET_CODEC, ServerBoundPackets::handleIoSave);
		registrar.playToServer(SetRedstoneStatePayload.ID, SetRedstoneStatePayload.CODEC, ServerBoundPackets::handleSetRedstoneState);
		registrar.playToServer(SlotConfigSavePayload.ID, SlotConfigSavePayload.PACKET_CODEC, ServerBoundPackets::handleSlotConfigSave);
		registrar.playToServer(SlotSavePayload.ID, SlotSavePayload.PACKET_CODEC, ServerBoundPackets::handleSlotSave);
	}
}
