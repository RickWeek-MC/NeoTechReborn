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

package techreborn.blockentity.storage.energy.idsu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import reborncore.common.util.NBTSerializable;
import techreborn.config.TechRebornConfig;

import java.util.HashMap;

public class IDSUManager extends SavedData {
	private static final SavedData.Factory<IDSUManager> TYPE = new SavedData.Factory<>(IDSUManager::new, IDSUManager::createFromTag, null);
	private static final String KEY = "techreborn_idsu";

	private IDSUManager() {
	}

	@NotNull
	public static IDSUPlayer getPlayer(MinecraftServer server, String uuid) {
		return get(server).getPlayer(uuid);
	}

	private static IDSUManager get(MinecraftServer server) {
		ServerLevel serverWorld = server.getLevel(Level.OVERWORLD);
		return serverWorld.getDataStorage().computeIfAbsent(TYPE, KEY);
	}

	private final HashMap<String, IDSUPlayer> playerHashMap = new HashMap<>();

	@NotNull
	public IDSUPlayer getPlayer(String uuid) {
		return playerHashMap.computeIfAbsent(uuid, s -> new IDSUPlayer());
	}

	public static IDSUManager createFromTag(CompoundTag tag, HolderLookup.Provider registryLookup) {
		IDSUManager	idsuManager = new IDSUManager();
		idsuManager.fromTag(tag);
		return idsuManager;
	}

	public void fromTag(CompoundTag tag) {
		for (String uuid : tag.getAllKeys()) {
			playerHashMap.put(uuid, new IDSUPlayer(tag.getCompound(uuid)));
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registryLookup) {
		playerHashMap.forEach((uuid, player) -> tag.put(uuid, player.write()));
		return tag;
	}

	public class IDSUPlayer implements NBTSerializable {
		// Quanta energia c'e' nel conto del giocatore. Mai esposta direttamente: passa sempre dai
		// limiti di ingresso/uscita della singola IDSU (vedi InterdimensionalSUBlockEntity).
		private long amount = 0;

		private IDSUPlayer() {
		}

		private IDSUPlayer(CompoundTag compoundTag) {
			read(compoundTag);
		}

		@NotNull
		@Override
		public CompoundTag write() {
			CompoundTag tag = new CompoundTag();
			tag.putLong("energy", amount);
			return tag;
		}

		@Override
		public void read(@NotNull CompoundTag tag) {
			amount = tag.getLong("energy");
		}

		/** Mette dentro al massimo {@code max} unita', ritorna quante ne ha accettate. */
		public long insert(long max) {
			long accepted = Math.max(0, Math.min(max, TechRebornConfig.idsuMaxEnergy - amount));
			if (accepted > 0) {
				amount += accepted;
				setDirty();
			}
			return accepted;
		}

		/** Toglie al massimo {@code max} unita', ritorna quante ne ha date. */
		public long extract(long max) {
			long given = Math.max(0, Math.min(max, amount));
			if (given > 0) {
				amount -= given;
				setDirty();
			}
			return given;
		}

		public long getEnergy() {
			return amount;
		}

		public void setEnergy(long energy) {
			amount = energy;
			setDirty();
		}
	}

}
