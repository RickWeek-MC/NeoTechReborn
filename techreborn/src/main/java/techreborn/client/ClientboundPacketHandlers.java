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

package techreborn.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techreborn.client.gui.GuiManual;
import techreborn.packets.clientbound.OpenManualPayload;

/**
 * Client side of TechReborn's network, the counterpart of {@link techreborn.packets.ServerboundPackets}.
 * <p>
 * Port note: the registration lives in the common {@link techreborn.packets.Packets} (NeoForge wants a
 * payload registered the same way on both sides), only the handler bodies are here. This class is
 * loaded on the first packet, never on a dedicated server, which is why Packets calls it from inside a
 * lambda and not with a method reference.
 * <p>
 * Upstream also handled the ore-depth sync here; that one is a configuration-phase payload and arrives
 * with OreDepthSyncHandler, which is not ported yet.
 */
public class ClientboundPacketHandlers {

	public static void handleOpenManual(OpenManualPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> Minecraft.getInstance().setScreen(new GuiManual()));
	}
}
