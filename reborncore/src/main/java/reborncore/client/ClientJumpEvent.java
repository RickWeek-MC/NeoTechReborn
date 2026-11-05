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

import net.neoforged.bus.api.Event;

/**
 * Fired on the client every tick the jump key is held down.
 * <p>
 * Port note: upstream was an interface backed by Fabric's EventFactory. Block 13 removed the clone
 * of that event system, so this is a plain NeoForge event now: other mods can see it and it shows
 * up on the game bus like anything else.
 * <p>
 * Subscribing changed shape for callers. Instead of
 * {@code ClientJumpEvent.EVENT.register(() -> ...)} it is now
 * {@code NeoForge.EVENT_BUS.addListener(ClientJumpEvent.class, event -> ...)} or a
 * {@code @SubscribeEvent} method. TechReborn's jetpack and quantum boots are the callers to adapt.
 */
public class ClientJumpEvent extends Event {
}
