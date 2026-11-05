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

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techreborn.component.TRDataComponentTypes;
import techreborn.config.TechRebornConfig;
import techreborn.init.TRContent;
import techreborn.packets.serverbound.QuantumSuitSprintPayload;
import techreborn.blockentity.GuiType;
import techreborn.blockentity.machine.iron.IronFurnaceBlockEntity;
import techreborn.init.TRBlockEntities;
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
import techreborn.packets.serverbound.RollingMachineLockPayload;
import techreborn.packets.serverbound.AutoCraftingLockPayload;
import techreborn.packets.serverbound.DetectorRadiusPayload;
import techreborn.packets.serverbound.JumpPayload;
import techreborn.blockentity.machine.tier1.ElevatorBlockEntity;

/**
 * Server side of TechReborn's network: the handler bodies, registered from {@link Packets}.
 * <p>
 * Port note: like RebornCore, every handler goes through {@code context.enqueueWork}, because NeoForge
 * calls these on the network thread while Fabric called them on the server thread.
 * <p>
 * Only the three that belong to ported content are here. The other twelve of upstream (AESU, pump,
 * rolling machine, storage unit, chunk loader, elevator jump...) arrive with the machines.
 */
public class ServerboundPackets {

	/** The two keys of the armour: night vision on the helmet, sprint on the Quantum leggings. */
	public static void handleSuitNightVision(SuitNightVisionPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			for (ItemStack stack : context.player().getArmorSlots()) {
				if (stack.is(TRContent.NANO_HELMET) || stack.is(TRContent.QUANTUM_HELMET)) {
					stack.set(TRDataComponentTypes.IS_ACTIVE, !stack.getOrDefault(TRDataComponentTypes.IS_ACTIVE, false));
					break;
				}
			}
		});
	}

	public static void handleQuantumSuitSprint(QuantumSuitSprintPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			for (ItemStack stack : context.player().getArmorSlots()) {
				if (stack.is(TRContent.QUANTUM_LEGGINGS)) {
					stack.set(TRDataComponentTypes.IS_ACTIVE, !stack.getOrDefault(TRDataComponentTypes.IS_ACTIVE, false));
					break;
				}
			}
		});
	}

	/** The manual's refund button: gives back the book and the refined iron it was crafted from. */
	/** Bottone dell'esperienza nel forno di ferro: svuota l'esperienza accumulata nel giocatore. */
	public static void handleExperience(ExperiencePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			IronFurnaceBlockEntity ironFurnace = GuiType.IRON_FURNACE.getBlockEntity(context, payload, TRBlockEntities.IRON_FURNACE);
			ironFurnace.handleGuiInputFromClient(context.player());
		});
	}

	/** Bottoni +/- della dimensione dell'anello nel computer di controllo della fusione. */
	public static void handleFusionControlSize(FusionControlSizePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var fusionControlComputer = GuiType.FUSION_CONTROLLER.getBlockEntity(context, payload, TRBlockEntities.FUSION_CONTROL_COMPUTER);
			fusionControlComputer.changeSize(payload.sizeDelta());
		});
	}

	/** Bottoni +/- dell'uscita nella AESU. */
	public static void handleAesuConfig(AESUConfigPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var aesu = GuiType.AESU.getBlockEntity(context, payload, TRBlockEntities.ADJUSTABLE_SU);
			aesu.handleGuiInputFromClient(payload.buttonID(), payload.shift(), payload.ctrl());
		});
	}

	public static void handleRefund(RefundPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!TechRebornConfig.allowManualRefund) {
				return;
			}

			Inventory inventory = context.player().getInventory();
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				if (inventory.getItem(i).is(TRContent.MANUAL)) {
					inventory.removeItemNoUpdate(i);
					inventory.add(new ItemStack(Items.BOOK));
					inventory.add(TRContent.Ingots.REFINED_IRON.getStack());
					return;
				}
			}
		});
	}

	/** Lucchetto dell'unita' di stoccaggio (blocco M6). */
	public static void handleStorageUnitLock(StorageUnitLockPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var storageUnit = GuiType.STORAGE_UNIT.getBlockEntity(context, payload, TRBlockEntities.STORAGE_UNIT);
			storageUnit.setLocked(payload.locked());
		});
	}

	/** Bottoni del chunk loader (blocco M6). */
	public static void handleChunkloader(ChunkloaderPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var chunkLoader = GuiType.CHUNK_LOADER.getBlockEntity(context, payload, TRBlockEntities.CHUNK_LOADER);
			chunkLoader.handleGuiInputFromClient(payload.buttonID(), payload.sync() ? context.player() : null);
		});
	}

	/** Bottoni della velocita' della piattaforma di lancio (blocco M6). */
	public static void handleLaunchSpeed(LaunchSpeedPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var launchpad = GuiType.LAUNCHPAD.getBlockEntity(context, payload, TRBlockEntities.LAUNCHPAD);
			launchpad.handleGuiInputFromClient(payload.buttonAmount());
		});
	}

	/** Bottoni della profondita' della pompa (blocco M6). */
	public static void handlePumpDepth(PumpDepthPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var pump = GuiType.PUMP.getBlockEntity(context, payload, TRBlockEntities.PUMP);
			pump.handleDepthGuiInputFromClient(payload.buttonAmount());
		});
	}

	/** Bottoni del raggio della pompa (blocco M6). */
	public static void handlePumpRange(PumpRangePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var pump = GuiType.PUMP.getBlockEntity(context, payload, TRBlockEntities.PUMP);
			pump.handleRangeGuiInputFromClient(payload.buttonAmount());
		});
	}

	/** Lucchetto della rolling machine (blocco M8). */
	public static void handleRollingMachineLock(RollingMachineLockPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var rollingMachine = GuiType.ROLLING_MACHINE.getBlockEntity(context, payload, TRBlockEntities.ROLLING_MACHINE);
			rollingMachine.locked = payload.locked();
		});
	}

	/** Lucchetto del tavolo da crafting automatico (blocco M8). */
	public static void handleAutoCraftingLock(AutoCraftingLockPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var autoCraftingTable = GuiType.AUTO_CRAFTING_TABLE.getBlockEntity(context, payload, TRBlockEntities.AUTO_CRAFTING_TABLE);
			autoCraftingTable.locked = payload.locked();
		});
	}

	/** Bottoni del raggio del rilevatore di giocatori (blocco M8). */
	public static void handleDetectorRadius(DetectorRadiusPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			var playerDetector = GuiType.PLAYER_DETECTOR.getBlockEntity(context, payload, TRBlockEntities.PLAYER_DETECTOR);
			playerDetector.handleGuiInputFromClient(payload.buttonAmount());
		});
	}

	/** Il giocatore salta sopra un ascensore (blocco M8). */
	public static void handleJump(JumpPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(payload.pos().below()) instanceof ElevatorBlockEntity elevator) {
				elevator.teleportUp(context.player());
			}
		});
	}
}
