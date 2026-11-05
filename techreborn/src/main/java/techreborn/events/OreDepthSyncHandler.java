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

package techreborn.events;

import net.minecraft.core.registries.BuiltInRegistries;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import techreborn.TechReborn;
import techreborn.world.OreDepth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tells the client at which height each ore generates, so the tooltip can show it.
 * <p>
 * Port notes: this is sent during the CONFIGURATION phase, as upstream does. Fabric's
 * {@code ServerConfigurationConnectionEvents.CONFIGURE} becomes a configuration TASK, registered
 * from {@link RegisterConfigurationTasksEvent}; the payload itself is registered in
 * {@link techreborn.packets.Packets} with {@code configurationToClient}.
 * <p>
 * Upstream also disconnected a client that could not receive the packet ("TechReborn must be
 * installed"). That check is gone on purpose: NeoForge negotiates channels before configuration and
 * already refuses a client without the mod, so there is nothing left to check by hand.
 */
public final class OreDepthSyncHandler {
	private static Map<Block, OreDepth> oreDepthMap = new HashMap<>();

	private OreDepthSyncHandler() {
	}

	public static void init(IEventBus modEventBus) {
		modEventBus.addListener(OreDepthSyncHandler::registerTasks);
	}

	private static void registerTasks(RegisterConfigurationTasksEvent event) {
		event.register(new SyncTask(event.getListener()));
	}

	public static void updateDepths(List<OreDepth> list) {
		synchronized (OreDepthSyncHandler.class) {
			oreDepthMap = list.stream()
				.collect(Collectors.toMap(oreDepth -> BuiltInRegistries.BLOCK.get(oreDepth.identifier()), Function.identity()));
		}
	}

	public static Map<Block, OreDepth> getOreDepthMap() {
		synchronized (OreDepthSyncHandler.class) {
			return oreDepthMap;
		}
	}

	/**
	 * ATTENZIONE, errore che blocca il gioco: un compito di configurazione NON si chiude da solo.
	 * Finche' non chiama {@code finishCurrentTask(type())} il server non passa al compito
	 * successivo e il client resta per sempre in "Joining world", poi il server lancia
	 * "Task techreborn:ore_depth has not finished yet". Vale anche quando non c'e' niente da
	 * mandare: la chiusura va fatta su OGNI percorso. Il compito gemello di NeoForge
	 * (network/configuration/SyncConfig) fa esattamente questo.
	 */
	private record SyncTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
		private static final ConfigurationTask.Type TYPE =
			new ConfigurationTask.Type(TechReborn.MOD_ID + ":ore_depth");

		@Override
		public void run(Consumer<CustomPacketPayload> sender) {
			try {
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
				if (server != null) {
					sender.accept(new OreDepthPayload(OreDepth.create(server)));
				}
			} finally {
				listener.finishCurrentTask(TYPE);
			}
		}

		@Override
		public ConfigurationTask.Type type() {
			return TYPE;
		}
	}

	public record OreDepthPayload(List<OreDepth> oreDepths) implements CustomPacketPayload {
		public static final Type<OreDepthPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, "ore_depth"));
		// Nota: in fase di CONFIGURAZIONE non esiste un RegistryFriendlyByteBuf (i registri non sono
		// ancora sincronizzati), quindi il codec e' su ByteBuf semplice. Qui basta: dentro ci sono
		// solo ResourceLocation e interi.
		public static final StreamCodec<ByteBuf, OreDepthPayload> PACKET_CODEC = StreamCodec.composite(
			OreDepth.STREAM_CODEC.apply(ByteBufCodecs.list()), OreDepthPayload::oreDepths,
			OreDepthPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}
}
