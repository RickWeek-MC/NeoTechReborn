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

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import reborncore.common.screen.BuiltScreenHandler;

/**
 * Nota di porting: su Fabric era HandledScreens.Provider, qui e' MenuScreens.ScreenConstructor.
 * Serve a passare dalla firma di vanilla (menu, inventario, titolo) a quella delle schermate di
 * TechReborn (syncId, giocatore, block entity), che prendono i dati dal block entity.
 */
public interface GuiFactory<T extends BlockEntity> extends MenuScreens.ScreenConstructor<BuiltScreenHandler, AbstractContainerScreen<BuiltScreenHandler>> {

	AbstractContainerScreen<BuiltScreenHandler> create(int syncId, Player player, T blockEntity);

	@Override
	default AbstractContainerScreen<BuiltScreenHandler> create(BuiltScreenHandler screenHandler, Inventory inventory, Component title) {
		Player player = inventory.player;
		//noinspection unchecked
		T blockEntity = (T) screenHandler.getBlockEntity();
		return create(screenHandler.containerId, player, blockEntity);
	}
}
