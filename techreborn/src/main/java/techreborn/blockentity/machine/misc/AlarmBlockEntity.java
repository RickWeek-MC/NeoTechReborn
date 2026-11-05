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

package techreborn.blockentity.machine.misc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import reborncore.api.IToolDrop;
import techreborn.blocks.misc.BlockAlarm;
import techreborn.init.ModSounds;
import techreborn.init.TRBlockEntities;
import techreborn.init.TRContent;

/**
 * Port notes: {@code sendMessage(text, true)} on a player is {@code displayClientMessage}, and
 * {@code isReceivingRedstonePower} is {@code hasNeighborSignal}. The sounds are holders now, but
 * playSound takes a Holder directly, so there is no {@code .get()} here.
 */
public class AlarmBlockEntity extends BlockEntity
		implements BlockEntityTicker<AlarmBlockEntity>, IToolDrop {
	private int selectedSound = 1;

	public AlarmBlockEntity(BlockPos pos, BlockState state) {
		super(TRBlockEntities.ALARM, pos, state);
	}

	public void rightClick(Entity entity) {
		if (level == null) return;

		if (selectedSound < 3) {
			selectedSound++;
		} else {
			selectedSound = 1;
		}

		if (entity instanceof ServerPlayer serverPlayerEntity) {
			serverPlayerEntity.displayClientMessage(Component.translatable("techreborn.message.alarm")
											.withStyle(ChatFormatting.GRAY)
											.append(" Alarm ")
											.append(String.valueOf(selectedSound)), true);
		}
	}

	// BlockEntity
	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registryLookup) {
		super.saveAdditional(compound, registryLookup);
		compound.putInt("selectedSound", this.selectedSound);
	}

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider registryLookup) {
		super.loadAdditional(compound, registryLookup);
		selectedSound = compound.getInt("selectedSound");
	}

	// Tickable
	@Override
	public void tick(Level level, BlockPos pos, BlockState state, AlarmBlockEntity blockEntity) {
		if (level == null || level.isClientSide()) return;
		if (level.getGameTime() % 25 != 0) return;

		if (level.hasNeighborSignal(getBlockPos())) {
			BlockAlarm.setActive(true, level, pos);
			switch (selectedSound) {
				case 1 -> level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.ALARM, SoundSource.BLOCKS, 4F, 1F);
				case 2 -> level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.ALARM_2, SoundSource.BLOCKS, 4F, 1F);
				case 3 -> level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.ALARM_3, SoundSource.BLOCKS, 4F, 1F);
			}
		} else {
			BlockAlarm.setActive(false, level, pos);
		}
	}

	// IToolDrop
	@Override
	public ItemStack getToolDrop(final Player entityPlayer) {
		return TRContent.Machine.ALARM.getStack();
	}
}
