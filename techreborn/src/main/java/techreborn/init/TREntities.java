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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techreborn.TechReborn;
import techreborn.entities.EntityNukePrimed;

/**
 * TechReborn's entity types. There is one: the primed nuke.
 * <p>
 * Port note: upstream built it with FabricEntityTypeBuilder in a static field of TRContent and
 * registered it from TRContent.register(). It cannot stay there: TRContent is initialized inside
 * the registration window (see the note on TRContent), while a DeferredRegister has to be created
 * and handed the mod event bus while the mod constructor runs. So entity types get their own class,
 * exactly like {@link ModSounds}, and the field is a holder: users call {@code .get()}.
 * <p>
 * trackRangeChunks(10) is clientTrackingRange(10), and the MobCategory upstream never stated is
 * MISC, which is what FabricEntityTypeBuilder.create() defaults to.
 */
public class TREntities {
	private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
		DeferredRegister.create(Registries.ENTITY_TYPE, TechReborn.MOD_ID);

	public static final DeferredHolder<EntityType<?>, EntityType<EntityNukePrimed>> NUKE =
		ENTITY_TYPES.register("nuke", () -> EntityType.Builder
			.<EntityNukePrimed>of(EntityNukePrimed::new, MobCategory.MISC)
			.sized(1.0F, 1.0F)
			.clientTrackingRange(10)
			.build("nuke"));

	public static void register(IEventBus modEventBus) {
		ENTITY_TYPES.register(modEventBus);
	}
}
