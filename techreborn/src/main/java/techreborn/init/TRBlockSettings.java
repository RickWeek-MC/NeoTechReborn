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

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Port note: Fabric's {@code FabricBlockSettings} is vanilla's {@link BlockBehaviour.Properties} on
 * NeoForge, so the builder calls are renamed but the values are untouched.
 * <p>
 * Map colours: Yarn IRON_GRAY/BRIGHT_RED/SPRUCE_BROWN/OAK_TAN are METAL/FIRE/PODZOL/WOOD here,
 * matched by colour value, not by name.
 * <p>
 * Fabric's {@code burnable()} IS here, it just has another name: it is
 * {@code BlockBehaviour.Properties.ignitedByLava()} (verified on the Yarn mapping of AbstractBlock),
 * and it means "the lava sets this block on fire". It is NOT the same thing as the fire spreading
 * odds, which live on the fire block ({@code FireBlock.setFlammable}, called from
 * TechReborn.commonSetup): vanilla sets both on oak planks, so the rubber wood needs both too.
 */
public class TRBlockSettings {
	private static BlockBehaviour.Properties metal() {
		return BlockBehaviour.Properties.of()
			.sound(SoundType.METAL)
			.mapColor(MapColor.METAL)
			.strength(2f, 2f);
	}

	public static BlockBehaviour.Properties machine() {
		return metal();
	}

	public static BlockBehaviour.Properties nuke() {
		return BlockBehaviour.Properties.of()
			.strength(2F, 2F)
			.mapColor(MapColor.FIRE);
	}

	public static BlockBehaviour.Properties reinforcedGlass() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
			.strength(4f, 60f)
			.sound(SoundType.STONE);
	}

	private static BlockBehaviour.Properties rubber(boolean noCollision, float hardness, float resistance) {
		var settings = BlockBehaviour.Properties.of()
			.mapColor(MapColor.PODZOL)
			.strength(hardness, resistance)
			.sound(SoundType.WOOD);

		if (noCollision) {
			settings.noCollission();
		}

		return settings;
	}

	private static BlockBehaviour.Properties rubber(float hardness, float resistance) {
		return rubber(false, hardness, resistance);
	}

	/** Upstream: {@code rubber(2f, 2f).burnable()}. Here {@code burnable()} is {@code ignitedByLava()}. */
	public static BlockBehaviour.Properties rubberWood() {
		return rubber(2f, 2f)
			.ignitedByLava();
	}

	public static BlockBehaviour.Properties rubberWoodStripped() {
		return rubberWood()
			.strength(2.0F, 15.0F);
	}

	public static BlockBehaviour.Properties rubberLeaves() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES)
			.mapColor(MapColor.PODZOL);
	}

	public static BlockBehaviour.Properties rubberSapling() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SAPLING)
			.mapColor(MapColor.PODZOL);
	}

	public static BlockBehaviour.Properties rubberLog() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG)
			.randomTicks()
			.mapColor(MapColor.PODZOL);
	}

	public static BlockBehaviour.Properties rubberLogStripped() {
		return rubberLog().strength(2.0F, 15.0F);
	}

	public static BlockBehaviour.Properties rubberSlab() {
		return rubberLog();
	}

	public static BlockBehaviour.Properties rubberFence() {
		return rubberLog();
	}

	public static BlockBehaviour.Properties rubberFenceGate() {
		return rubberLog();
	}

	public static BlockBehaviour.Properties pottedRubberSapling() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_SPRUCE_SAPLING);
	}

	public static BlockBehaviour.Properties copperWall() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)
			.strength(2f, 2f);
	}

	public static BlockBehaviour.Properties rubberTrapdoor() {
		return rubber(3.0F, 3.0F);
	}

	public static BlockBehaviour.Properties rubberDoor() {
		return rubber(3.0F, 3.0F);
	}

	public static BlockBehaviour.Properties rubberButton() {
		return rubber(true, 0.5F, 0.5F);
	}

	public static BlockBehaviour.Properties rubberPressurePlate() {
		return rubber(true, 0.5F, 0.5F);
	}

	public static BlockBehaviour.Properties refinedIronFence() {
		return metal()
			.strength(2.0F, 3.0F);
	}

	public static BlockBehaviour.Properties storageBlock(boolean isHot, float hardness, float resistance) {
		BlockBehaviour.Properties settings = BlockBehaviour.Properties.of()
			.strength(hardness, resistance)
			.mapColor(MapColor.METAL)
			.sound(SoundType.METAL);

		if (isHot) {
			settings = settings.lightLevel(state -> 15)
				.noOcclusion();
		}

		return settings;
	}

	public static BlockBehaviour.Properties ore(boolean deepslate) {
		return BlockBehaviour.Properties.of()
			.requiresCorrectToolForDrops()
			.sound(deepslate ? SoundType.DEEPSLATE : SoundType.STONE)
			.destroyTime(deepslate ? 4.5f : 3f)
			.explosionResistance(3f);
	}

	public static BlockBehaviour.Properties machineFrame() {
		return metal()
			.strength(1f, 1f);
	}

	public static BlockBehaviour.Properties machineCasing() {
		return metal()
			.strength(2f, 2f)
			.requiresCorrectToolForDrops();
	}

	public static BlockBehaviour.Properties energyStorage() {
		return metal();
	}

	public static BlockBehaviour.Properties lsuStorage() {
		return metal();
	}

	public static BlockBehaviour.Properties storageUnit(boolean wooden) {
		if (!wooden) {
			return metal();
		}

		return BlockBehaviour.Properties.of()
			.sound(SoundType.WOOD)
			.mapColor(MapColor.WOOD)
			.strength(2f, 2f);
	}

	public static BlockBehaviour.Properties fusionCoil() {
		return metal();
	}

	public static BlockBehaviour.Properties transformer() {
		return metal();
	}

	public static BlockBehaviour.Properties playerDetector() {
		return metal();
	}

	public static BlockBehaviour.Properties fluid() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.WATER);
	}

	public static BlockBehaviour.Properties computerCube() {
		return metal();
	}

	public static BlockBehaviour.Properties alarm() {
		return metal();
	}

	public static BlockBehaviour.Properties genericMachine() {
		return metal();
	}

	public static BlockBehaviour.Properties tankUnit() {
		return metal();
	}

	public static BlockBehaviour.Properties fusionControlComputer() {
		return metal();
	}

	public static BlockBehaviour.Properties solarPanel() {
		return metal();
	}

	public static BlockBehaviour.Properties cable() {
		return metal().strength(1f, 8f);
	}

	public static BlockBehaviour.Properties resinBasin() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.sound(SoundType.WOOD)
			.strength(2F, 2F);
	}

	public static BlockBehaviour.Properties lightBlock() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_BLOCK)
			.strength(2f, 2f);
	}
}
