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

package techreborn.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import reborncore.common.fluid.container.ItemFluidInfo;
import techreborn.TechReborn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The cell's item model: the three fixed layers from the JSON, plus the fluid inside, which depends on
 * the stack and so can only be drawn from code.
 * <p>
 * Port note. Upstream did this with Fabric's renderer API (DynamicCellBakedModel +
 * BaseDynamicFluidBakedModel): it asked for the baked models of cell_base, cell_background, cell_fluid
 * and cell_glass and emitted their quads one after the other, re-texturing the fluid layer with the
 * fluid's still sprite and tinting it.
 * <p>
 * NeoForge has nothing of that shape, and its ready-made {@code neoforge:fluid_container} loader draws
 * nothing here (block T7): it builds its layers out of the ALPHA SHAPE of flat sprites, while the cell's
 * fluid layer is a small 3D box inside the glass, from {@code item/cell_fluid_template}. So this is a
 * geometry loader of our own, built out of pieces NeoForge does provide:
 * <ul>
 * <li>the fixed layers stay model references in the JSON and are baked by the baker, exactly as
 *     {@code neoforge:composite} does -- the path that already works in game;</li>
 * <li>the fluid layer is one box, written below in code, baked once per fluid in the registry while the
 *     models are being baked. Nothing is baked while rendering;</li>
 * <li>which layers a stack draws is answered by {@link Baked#getRenderPasses(ItemStack, boolean)}, the
 *     NeoForge hook that hands the model the stack. That is the whole trick: item models are otherwise
 *     asked for their quads without the stack.</li>
 * </ul>
 * The fluid is read off the item through {@link ItemFluidInfo}, as upstream did, NOT through the fluid
 * capability: the capability is for machines moving fluids and is one more thing that can be wrong.
 * <p>
 * The colour comes from {@link CellItemColor}, which is why the fluid faces carry tint index 1.
 * <p>
 * If the fluid layer ever stops drawing, the fixed layers still do: the cell then looks exactly as it did
 * before, an always-empty cell. There is a log line at startup saying how many fluid layers were built,
 * and one on the first cell drawn saying what it found.
 */
public class DynamicCellModel implements IUnbakedGeometry<DynamicCellModel> {

	public static final ResourceLocation LOADER_ID = ResourceLocation.fromNamespaceAndPath(TechReborn.MOD_ID, "dynamic_cell");

	/** Keys of "children" in the JSON, in drawing order; the fluid goes between background and glass. */
	private static final String BASE = "base";
	private static final String BACKGROUND = "background";
	private static final String GLASS = "glass";

	/**
	 * The fluid box of {@code item/cell_fluid_template}, kept in step with that file by hand: three pixels
	 * wide, eight tall, in the middle of the cell, with only the two flat faces textured.
	 */
	private static final Vector3f FLUID_FROM = new Vector3f(7.0F, 4.0F, 7.5F);
	private static final Vector3f FLUID_TO = new Vector3f(10.0F, 12.0F, 8.5F);
	private static final float[] FLUID_UV = new float[]{7.0F, 4.0F, 10.0F, 12.0F};

	/** The tint index {@link CellItemColor} colours. */
	public static final int FLUID_TINT_INDEX = 1;

	/**
	 * TEMPORARY diagnostics, off unless asked for with a JVM argument, for example
	 * {@code -Dtechreborn.cell.debug=log,cutout}. Flags, comma separated:
	 * <ul>
	 * <li>{@code log}: log the sprite, the tint and the "fabulous" flag for the first few fluids;</li>
	 * <li>{@code cutout}: draw the fluid layer with a cutout render type instead of translucent;</li>
	 * <li>{@code bigoffset}: push the fluid and glass layers much further apart (depth test check);</li>
	 * <li>{@code whitetint}: ignore the fluid colour and tint the fluid layer white.</li>
	 * </ul>
	 * Remove this before a release.
	 */
	private static final String DEBUG = System.getProperty("techreborn.cell.debug", "");

	public static boolean debug(String flag) {
		return DEBUG.contains(flag);
	}

	/**
	 * The four layers are all at the same depth (the flat base quad and the three boxes of
	 * cell_fluid_template all sit between z 7.5 and 8.5), and each layer here is a separate drawing pass.
	 * Two surfaces at the very same depth fight, and the one drawn last is not necessarily the one on
	 * top, which is why the fluid did not show at all in the first version of this file. So the fluid is
	 * pushed a hair forward and the glass a hair further, exactly as NeoForge's own bucket model does:
	 * invisible to the eye (under a thousandth of the item's size), enough for the depth test.
	 */
	private static final Transformation FLUID_OFFSET = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.0F, 1.0F, debug("bigoffset") ? 1.2F : 1.002F), new Quaternionf());
	private static final Transformation GLASS_OFFSET = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.0F, 1.0F, debug("bigoffset") ? 1.4F : 1.004F), new Quaternionf());

	private final ImmutableMap<String, BlockModel> children;

	private DynamicCellModel(ImmutableMap<String, BlockModel> children) {
		this.children = children;
	}

	@Override
	public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
		TextureAtlasSprite particle = context.hasMaterial("particle") ? spriteGetter.apply(context.getMaterial("particle")) : null;

		List<BakedModel> fixedLayers = new ArrayList<>();
		BakedModel base = bakeChild(BASE, baker, spriteGetter, modelState);
		BakedModel background = bakeChild(BACKGROUND, baker, spriteGetter, modelState);
		BakedModel glass = bakeChild(GLASS, baker, spriteGetter, offset(modelState, GLASS_OFFSET));
		for (BakedModel layer : new BakedModel[]{base, background, glass}) {
			if (layer != null) {
				fixedLayers.add(layer);
			}
		}

		// One fluid layer per fluid, built now: at render time there is only a map lookup left.
		Map<Fluid, List<BakedModel>> layersByFluid = new HashMap<>();
		int loggedBakes = 0;
		for (Fluid fluid : BuiltInRegistries.FLUID) {
			if (fluid == Fluids.EMPTY) {
				continue;
			}

			List<BakedQuad> quads = bakeFluidLayer(fluid, spriteGetter, offset(modelState, FLUID_OFFSET));
			if (quads.isEmpty()) {
				continue;
			}

			if (debug("log") && loggedBakes < 6) {
				loggedBakes++;
				TechReborn.LOGGER.info("Cell model: debug bake {} -> {} quads, sprite {}", BuiltInRegistries.FLUID.getKey(fluid), quads.size(), quads.getFirst().getSprite().contents().name());
			}

			BakedModel fluidLayer = buildFluidLayer(context, particle, overrides, quads);
			List<BakedModel> layers = new ArrayList<>();
			if (base != null) {
				layers.add(base);
			}
			if (background != null) {
				layers.add(background);
			}
			layers.add(fluidLayer);
			if (glass != null) {
				layers.add(glass);
			}
			layersByFluid.put(fluid, List.copyOf(layers));
		}

		TechReborn.LOGGER.info("Cell model: {} fixed layers, fluid layers built for {} fluids", fixedLayers.size(), layersByFluid.size());

		return new Baked(context, particle, overrides, List.copyOf(fixedLayers), layersByFluid);
	}

	@Nullable
	private BakedModel bakeChild(String name, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
		BlockModel child = children.get(name);
		if (child == null) {
			return null;
		}

		return child.bake(baker, child, spriteGetter, modelState, true);
	}

	private List<BakedQuad> bakeFluidLayer(Fluid fluid, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
		ResourceLocation stillTexture = IClientFluidTypeExtensions.of(fluid).getStillTexture();
		if (stillTexture == null) {
			return List.of();
		}

		TextureAtlasSprite fluidSprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, stillTexture));
		if (fluidSprite == null) {
			return List.of();
		}

		return UnbakedGeometryHelper.bakeElements(fluidElements(), material -> fluidSprite, modelState);
	}

	private static ModelState offset(ModelState modelState, Transformation offset) {
		return new SimpleModelState(modelState.getRotation().compose(offset), modelState.isUvLocked());
	}

	private static List<BlockElement> fluidElements() {
		BlockFaceUV uv = new BlockFaceUV(FLUID_UV.clone(), 0);
		Map<Direction, BlockElementFace> faces = new HashMap<>();
		// The texture name is never looked up: bakeFluidLayer hands the same sprite to every face.
		faces.put(Direction.NORTH, new BlockElementFace(null, FLUID_TINT_INDEX, "fluid", uv));
		faces.put(Direction.SOUTH, new BlockElementFace(null, FLUID_TINT_INDEX, "fluid", uv));

		return List.of(new BlockElement(new Vector3f(FLUID_FROM), new Vector3f(FLUID_TO), faces, null, true));
	}

	private static BakedModel buildFluidLayer(IGeometryBakingContext context, @Nullable TextureAtlasSprite particle, ItemOverrides overrides, List<BakedQuad> quads) {
		// Translucent, the same pair of render types NeoForge's own bucket model uses for its fluid layer.
		RenderTypeGroup renderTypes = debug("cutout")
			? new RenderTypeGroup(RenderType.cutout(), NeoForgeRenderTypes.ITEM_LAYERED_CUTOUT.get())
			: new RenderTypeGroup(RenderType.translucent(), NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());

		IModelBuilder<?> builder = IModelBuilder.of(context.useAmbientOcclusion(), context.useBlockLight(),
			context.isGui3d(), context.getTransforms(), overrides, particle, renderTypes);
		quads.forEach(builder::addUnculledFace);

		return builder.build();
	}

	@Override
	public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
		children.values().forEach(child -> child.resolveParents(modelGetter));
	}

	@Override
	public Set<String> getConfigurableComponentNames() {
		return children.keySet();
	}

	/**
	 * The baked cell. Everything is ready: drawing a stack is a map lookup on the fluid it holds.
	 */
	private static class Baked implements IDynamicBakedModel {
		private static boolean loggedEmpty = false;
		private static boolean loggedFull = false;
		private static final java.util.Set<Fluid> LOGGED_DRAWS = new HashSet<>();

		private final boolean ambientOcclusion;
		private final boolean gui3d;
		private final boolean blockLight;
		@Nullable
		private final TextureAtlasSprite particle;
		private final ItemTransforms transforms;
		private final ItemOverrides overrides;
		private final List<BakedModel> fixedLayers;
		private final Map<Fluid, List<BakedModel>> layersByFluid;

		private Baked(IGeometryBakingContext context, @Nullable TextureAtlasSprite particle, ItemOverrides overrides, List<BakedModel> fixedLayers, Map<Fluid, List<BakedModel>> layersByFluid) {
			this.ambientOcclusion = context.useAmbientOcclusion();
			this.gui3d = context.isGui3d();
			this.blockLight = context.useBlockLight();
			this.particle = particle;
			this.transforms = context.getTransforms();
			this.overrides = overrides;
			this.fixedLayers = fixedLayers;
			this.layersByFluid = layersByFluid;
		}

		/**
		 * The hook that makes this work: NeoForge asks the model which pieces to draw for THIS stack.
		 */
		@Override
		public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
			Fluid fluid = fluidOf(stack);
			List<BakedModel> layers = fluid == Fluids.EMPTY ? null : layersByFluid.get(fluid);

			// Two separate lines, because the first cell drawn is usually an empty one and that says nothing.
			if (fluid == Fluids.EMPTY) {
				if (!loggedEmpty) {
					loggedEmpty = true;
					TechReborn.LOGGER.info("Cell model: first EMPTY cell drawn (item class {})", stack.getItem().getClass().getSimpleName());
				}
			} else if (!loggedFull) {
				loggedFull = true;
				TechReborn.LOGGER.info("Cell model: first FULL cell drawn, fluid is {}, fluid layer {}",
					BuiltInRegistries.FLUID.getKey(fluid), layers != null ? "found" : "NOT found");
			}

			if (debug("log") && fluid != Fluids.EMPTY && LOGGED_DRAWS.size() < 8 && LOGGED_DRAWS.add(fluid)) {
				TechReborn.LOGGER.info("Cell model: debug draw {} -> {} layers, fabulous={}", BuiltInRegistries.FLUID.getKey(fluid), layers == null ? 0 : layers.size(), fabulous);
			}

			return layers != null ? layers : fixedLayers;
		}

		private static Fluid fluidOf(ItemStack stack) {
			return stack.getItem() instanceof ItemFluidInfo fluidInfo ? fluidInfo.getFluid(stack) : Fluids.EMPTY;
		}

		/**
		 * Without a stack there is no fluid to draw, so this is the empty cell. Item rendering does not
		 * come through here, it goes through {@link #getRenderPasses(ItemStack, boolean)}.
		 */
		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
			List<BakedQuad> quads = new ArrayList<>();
			for (BakedModel layer : fixedLayers) {
				quads.addAll(layer.getQuads(state, side, rand, data, renderType));
			}

			return quads;
		}

		@Override
		public boolean useAmbientOcclusion() {
			return ambientOcclusion;
		}

		@Override
		public boolean isGui3d() {
			return gui3d;
		}

		@Override
		public boolean usesBlockLight() {
			return blockLight;
		}

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return particle != null ? particle : fixedLayers.getFirst().getParticleIcon();
		}

		@Override
		public ItemOverrides getOverrides() {
			return overrides;
		}

		@Override
		public ItemTransforms getTransforms() {
			return transforms;
		}
	}

	public static final class Loader implements IGeometryLoader<DynamicCellModel> {
		public static final Loader INSTANCE = new Loader();

		private Loader() {
		}

		@Override
		public DynamicCellModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
			if (!jsonObject.has("children")) {
				throw new JsonParseException("The cell model needs a \"children\" element with the fixed layers.");
			}

			ImmutableMap.Builder<String, BlockModel> children = ImmutableMap.builder();
			for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.getAsJsonObject("children").entrySet()) {
				children.put(entry.getKey(), deserializationContext.deserialize(entry.getValue(), BlockModel.class));
			}

			return new DynamicCellModel(children.build());
		}
	}
}
