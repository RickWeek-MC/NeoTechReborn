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

package techreborn.client.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import techreborn.packets.serverbound.QuantumSuitSprintPayload;
import techreborn.packets.serverbound.SuitNightVisionPayload;

/**
 * The two keys of the armour: N toggles the helmet's night vision, R the Quantum leggings' sprint.
 * <p>
 * Port note: Fabric's KeyBindingHelper becomes RegisterKeyMappingsEvent on the mod bus, and
 * "wasPressed" in the client tick becomes {@code consumeClick} (see TechRebornClient). The keys are the
 * same as upstream, and so are the translation keys, which are already in the lang file.
 */
public class KeyBindings {
	public static final String CATEGORY = "key.techreborn.category";

	public static final KeyMapping SUIT_NIGHT_VISION = new KeyMapping(
		"key.techreborn.suitNightVision", InputConstants.Type.KEYSYM, InputConstants.KEY_N, CATEGORY);

	public static final KeyMapping QUANTUM_SUIT_SPRINT = new KeyMapping(
		"key.techreborn.quantumSuitSprint", InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY);

	public static void registerKeys(RegisterKeyMappingsEvent event) {
		event.register(SUIT_NIGHT_VISION);
		event.register(QUANTUM_SUIT_SPRINT);
	}

	/** Called once per press from the client tick; the server does the toggling. */
	public static void handlePresses() {
		while (SUIT_NIGHT_VISION.consumeClick()) {
			PacketDistributor.sendToServer(new SuitNightVisionPayload());
		}

		while (QUANTUM_SUIT_SPRINT.consumeClick()) {
			PacketDistributor.sendToServer(new QuantumSuitSprintPayload());
		}
	}
}
