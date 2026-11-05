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

package techreborn.packets;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import techreborn.client.ClientboundPacketHandlers;
import techreborn.events.OreDepthSyncHandler;
import techreborn.packets.clientbound.OpenManualPayload;
import techreborn.packets.serverbound.QuantumSuitSprintPayload;
import techreborn.packets.serverbound.AESUConfigPayload;
import techreborn.packets.serverbound.ChunkloaderPayload;
import techreborn.packets.serverbound.LaunchSpeedPayload;
import techreborn.packets.serverbound.PumpDepthPayload;
import techreborn.packets.serverbound.PumpRangePayload;
import techreborn.packets.serverbound.StorageUnitLockPayload;
import techreborn.packets.serverbound.FusionControlSizePayload;
import techreborn.packets.serverbound.ExperiencePayload;
import techreborn.packets.serverbound.RefundPayload;
import techreborn.packets.serverbound.SuitNightVisionPayload;
import techreborn.packets.serverbound.AutoCraftingLockPayload;
import techreborn.packets.serverbound.DetectorRadiusPayload;
import techreborn.packets.serverbound.JumpPayload;
import techreborn.packets.serverbound.RollingMachineLockPayload;

/**
 * Port note: same idea as RebornCore's Packets -- NeoForge registers the payload type and its handler
 * together, in RegisterPayloadHandlersEvent, so the Fabric register() with PayloadTypeRegistry becomes
 * an event listener hooked up from {@link #init(IEventBus)}.
 * <p>
 * Here: the manual's screen towards the client, and towards the server the two armour keys and the
 * manual's refund. The other twelve serverbound payloads of upstream (AESU, pump, rolling machine,
 * storage unit, chunk loader, elevator jump...) come with the machines.
 */
public class Packets {

	public static void init(IEventBus modEventBus) {
		modEventBus.addListener(Packets::register);
	}

	private static void register(RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");

		clientbound(registrar);
		serverbound(registrar);
		configuration(registrar);
	}

	/**
	 * Blocco M7: l'altezza dei minerali, mandata UNA volta in fase di configurazione, prima che il
	 * giocatore entri nel mondo (upstream fa lo stesso). Il compito che la manda e' registrato da
	 * OreDepthSyncHandler.
	 */
	private static void configuration(PayloadRegistrar registrar) {
		registrar.configurationToClient(OreDepthSyncHandler.OreDepthPayload.ID, OreDepthSyncHandler.OreDepthPayload.PACKET_CODEC,
			(payload, context) -> OreDepthSyncHandler.updateDepths(payload.oreDepths()));
	}

	private static void clientbound(PayloadRegistrar registrar) {
		// The handler is a lambda on purpose (see RebornCore Packets): nothing client-only may be
		// linked while registering, because a dedicated server runs this too.
		registrar.playToClient(OpenManualPayload.ID, OpenManualPayload.PACKET_CODEC,
			(payload, context) -> ClientboundPacketHandlers.handleOpenManual(payload, context));
	}

	private static void serverbound(PayloadRegistrar registrar) {
		registrar.playToServer(SuitNightVisionPayload.ID, SuitNightVisionPayload.PACKET_CODEC, ServerboundPackets::handleSuitNightVision);
		registrar.playToServer(QuantumSuitSprintPayload.ID, QuantumSuitSprintPayload.PACKET_CODEC, ServerboundPackets::handleQuantumSuitSprint);
		registrar.playToServer(RefundPayload.ID, RefundPayload.PACKET_CODEC, ServerboundPackets::handleRefund);
		registrar.playToServer(FusionControlSizePayload.ID, FusionControlSizePayload.PACKET_CODEC, ServerboundPackets::handleFusionControlSize);
		registrar.playToServer(AESUConfigPayload.ID, AESUConfigPayload.PACKET_CODEC, ServerboundPackets::handleAesuConfig);
		// blocco M6
		registrar.playToServer(StorageUnitLockPayload.ID, StorageUnitLockPayload.PACKET_CODEC, ServerboundPackets::handleStorageUnitLock);
		registrar.playToServer(ChunkloaderPayload.ID, ChunkloaderPayload.PACKET_CODEC, ServerboundPackets::handleChunkloader);
		registrar.playToServer(LaunchSpeedPayload.ID, LaunchSpeedPayload.PACKET_CODEC, ServerboundPackets::handleLaunchSpeed);
		registrar.playToServer(PumpDepthPayload.ID, PumpDepthPayload.PACKET_CODEC, ServerboundPackets::handlePumpDepth);
		registrar.playToServer(PumpRangePayload.ID, PumpRangePayload.PACKET_CODEC, ServerboundPackets::handlePumpRange);
		registrar.playToServer(ExperiencePayload.ID, ExperiencePayload.PACKET_CODEC, ServerboundPackets::handleExperience);
		// blocco M8
		registrar.playToServer(RollingMachineLockPayload.ID, RollingMachineLockPayload.PACKET_CODEC, ServerboundPackets::handleRollingMachineLock);
		registrar.playToServer(AutoCraftingLockPayload.ID, AutoCraftingLockPayload.PACKET_CODEC, ServerboundPackets::handleAutoCraftingLock);
		registrar.playToServer(DetectorRadiusPayload.ID, DetectorRadiusPayload.PACKET_CODEC, ServerboundPackets::handleDetectorRadius);
		registrar.playToServer(JumpPayload.ID, JumpPayload.PACKET_CODEC, ServerboundPackets::handleJump);
	}

}
