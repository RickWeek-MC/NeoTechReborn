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

package techreborn.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import reborncore.common.recipes.ICrafterSoundHandler;
import techreborn.TechReborn;
import techreborn.config.TechRebornConfig;

/**
 * Created by Mark on 20/03/2016.
 * <p>
 * Port note: upstream assigned these fields from ModRegistry, with a Registry.register per sound.
 * Sounds are registered through a {@link DeferredRegister} here, exactly as RebornCore does, so the
 * fields are holders and users need {@code .get()}. {@link #register(IEventBus)} goes in the mod
 * constructor.
 */
public class ModSounds {
	private static final DeferredRegister<SoundEvent> SOUNDS =
		DeferredRegister.create(Registries.SOUND_EVENT, TechReborn.MOD_ID);

	public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_SHOCK = register("cable_shock");
	public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DISMANTLE = register("block_dismantle");
	public static final DeferredHolder<SoundEvent, SoundEvent> SAP_EXTRACT = register("sap_extract");
	public static final DeferredHolder<SoundEvent, SoundEvent> AUTO_CRAFTING = register("auto_crafting");
	public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_RUN = register("machine_run");
	public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_START = register("machine_start");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALARM = register("alarm");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_2 = register("alarm_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_3 = register("alarm_3");

	private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, name);
		return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
	}

	public static void register(IEventBus modEventBus) {
		SOUNDS.register(modEventBus);
	}

	public static class SoundHandler implements ICrafterSoundHandler {

		@Override
		public void playSound(boolean firstRun, BlockEntity blockEntity) {
			Level world = blockEntity.getLevel();
			if (world == null) {
				return;
			}

			world.playSound(null, blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getY(),
					blockEntity.getBlockPos().getZ(), ModSounds.MACHINE_RUN.get(), SoundSource.BLOCKS, TechRebornConfig.machineSoundVolume, 1F);
		}
	}
}
