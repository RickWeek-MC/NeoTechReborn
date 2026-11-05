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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import reborncore.RebornRegistry;
import reborncore.common.fluid.FluidSettings;
import reborncore.common.fluid.RebornBucketItem;
import reborncore.common.fluid.RebornFluid;
import reborncore.common.fluid.RebornFluidBlock;
import reborncore.common.fluid.RebornFluidManager;
import techreborn.TechReborn;
import techreborn.utils.InitUtils;

import java.util.Locale;

/**
 * The mod's own fluids: 35 of them, each with a still fluid, a flowing fluid, a block and a bucket.
 * <p>
 * Port note: NeoForge wants a FluidType per fluid, which Fabric has no notion of. RebornFluidManager
 * builds and registers one per fluid, so the constructor used here is unchanged from upstream.
 * Textures come from the upstream resources, already in place.
 */
public enum ModFluids implements ItemLike {
	BERYLLIUM,
	CALCIUM,
	CALCIUM_CARBONATE,
	CARBON,
	CARBON_FIBER,
	CHLORITE,
	COMPRESSED_AIR,
	DEUTERIUM,
	DIESEL,
	ELECTROLYZED_WATER,
	GLYCERYL,
	HELIUM,
	HELIUM3,
	HELIUMPLASMA,
	HYDROGEN,
	LITHIUM,
	MERCURY,
	METHANE,
	NITRO_CARBON,
	NITRO_DIESEL,
	NITROCOAL_FUEL,
	NITROFUEL,
	NITROGEN,
	NITROGEN_DIOXIDE,
	OIL,
	POTASSIUM,
	SILICON,
	SODIUM,
	SODIUM_SULFIDE,
	SODIUM_PERSULFATE,
	SULFUR,
	SULFURIC_ACID,
	TRITIUM,
	WOLFRAMIUM,
	BIOFUEL;

	private RebornFluid stillFluid;
	private RebornFluid flowingFluid;

	private RebornFluidBlock block;
	private RebornBucketItem bucket;
	private final ResourceLocation identifier;

	ModFluids() {
		this.identifier = ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, this.toString().toLowerCase(Locale.ROOT));

		FluidSettings fluidSettings = FluidSettings.create();

		ResourceLocation texture_still = ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, "block/fluids/" + this.toString().toLowerCase(Locale.ROOT) + "_still");
		ResourceLocation texture_flowing = ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, "block/fluids/" + this.toString().toLowerCase(Locale.ROOT) + "_flowing");

		fluidSettings.setStillTexture(texture_still);
		fluidSettings.setFlowingTexture(texture_flowing);

		stillFluid = new RebornFluid(true, fluidSettings, () -> block, () -> bucket, () -> flowingFluid, () -> stillFluid) {
		};
		flowingFluid = new RebornFluid(false, fluidSettings, () -> block, () -> bucket, () -> flowingFluid, () -> stillFluid) {
		};

		block = new RebornFluidBlock(stillFluid, TRBlockSettings.fluid());
		bucket = new RebornBucketItem(stillFluid, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
	}

	public void register() {
		RebornFluidManager.register(stillFluid, identifier);
		RebornFluidManager.register(flowingFluid, ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, identifier.getPath() + "_flowing"));

		// Port note: upstream registered straight into the registries here. Those are frozen on
		// NeoForge, so block and bucket go through RebornRegistry, which queues them and flushes
		// them during the registry event. The fluids themselves are queued by RebornFluidManager.
		RebornRegistry.registerBlockNoItem(InitUtils.setup(block, identifier.getPath()));
		RebornRegistry.registerItem(bucket, ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, identifier.getPath() + "_bucket"));
	}

	public RebornFluid getFluid() {
		return stillFluid;
	}

	public RebornFluid getFlowingFluid() {
		return flowingFluid;
	}

	public RebornFluidBlock getBlock() {
		return block;
	}

	public ResourceLocation getResourceLocation() {
		return identifier;
	}

	public RebornBucketItem getBucket() {
		return bucket;
	}

	@Override
	public Item asItem() {
		return getBucket();
	}
}
