/*
 * This file is part of RebornCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 TeamReborn
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

package reborncore.common.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.GsonHelper;
import reborncore.common.util.FluidTextHelper;

/**
 * A fluid amount, stored internally in millibuckets (1000 per bucket).
 * <p>
 * Port note: upstream counted in Fabric droplets (81000 per bucket). This port uses NeoForge's own
 * unit instead, so nothing has to be converted when talking to another mod's tank or pipe. The
 * switch is safe because no Tech Reborn recipe ever spelled an amount in droplets: recipe JSON uses
 * plain numbers, which upstream already read as millibuckets.
 */
public record FluidValue(long rawValue) {
	/** Millibuckets in one bucket. */
	public static final long MILLIBUCKETS_PER_BUCKET = 1000;
	/** Droplets in one millibucket, only used when reading an old droplet-based value. */
	private static final long DROPLETS_PER_MILLIBUCKET = 81;

	public static final FluidValue EMPTY = new FluidValue(0);
	public static final FluidValue BUCKET_QUARTER = new FluidValue(MILLIBUCKETS_PER_BUCKET / 4);
	public static final FluidValue BUCKET = new FluidValue(MILLIBUCKETS_PER_BUCKET);

	public static final Codec<FluidValue> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.LONG.fieldOf("value").forGetter(FluidValue::getRawValue)
	).apply(instance, FluidValue::fromRaw));
	public static final StreamCodec<ByteBuf, FluidValue> PACKET_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_LONG, FluidValue::getRawValue,
		FluidValue::new
	);

	public static FluidValue fromMillibuckets(long millibuckets) {
		return new FluidValue(millibuckets);
	}

	/**
	 * This amount as an int, which is what NeoForge's fluid API takes.
	 */
	public int getMillibuckets() {
		return (int) Math.min(Integer.MAX_VALUE, rawValue);
	}

	public FluidValue multiply(long value) {
		return fromRaw(rawValue * value);
	}

	public FluidValue fraction(long divider) {return fromRaw(rawValue / divider);}

	public FluidValue add(FluidValue fluidValue) {
		return fromRaw(rawValue + fluidValue.rawValue);
	}

	public FluidValue subtract(FluidValue fluidValue) {
		return fromRaw(rawValue - fluidValue.rawValue);
	}

	public FluidValue min(FluidValue fluidValue) {
		return fromRaw(Math.min(rawValue, fluidValue.rawValue));
	}

	public boolean isEmpty() {
		return rawValue == 0;
	}

	public boolean moreThan(FluidValue value) {
		return rawValue > value.rawValue;
	}

	public boolean equalOrMoreThan(FluidValue value) {
		return rawValue >= value.rawValue;
	}

	public boolean lessThan(FluidValue value) {
		return rawValue < value.rawValue;
	}

	public boolean lessThanOrEqual(FluidValue value) {
		return rawValue <= value.rawValue;
	}

	@Override
	public String toString() {
		return FluidTextHelper.getValueDisplay(this) + " Mb";
	}

	//TODO move away from using this
	@Deprecated
	public long getRawValue() {
		return rawValue;
	}

	@Deprecated
	public static FluidValue fromRaw(long rawValue) {
		if (rawValue < 0) {
			rawValue = 0;
		}
		return new FluidValue(rawValue);
	}

	public static FluidValue parseFluidValue(JsonElement jsonElement) {
		if (jsonElement.isJsonObject()) {
			final JsonObject jsonObject = jsonElement.getAsJsonObject();
			if (jsonObject.has("buckets")) {
				int buckets = GsonHelper.getAsInt(jsonObject, "buckets");
				return BUCKET.multiply(buckets);
			} else if (jsonObject.has("droplets")) {
				// Kept for compatibility with hand-written Fabric-era JSON; converted to millibuckets.
				long droplets = GsonHelper.getAsLong(jsonObject, "droplets");
				return fromRaw(droplets / DROPLETS_PER_MILLIBUCKET);
			}
		} else if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber()) {
			return fromMillibuckets(jsonElement.getAsJsonPrimitive().getAsInt());
		}
		throw new JsonSyntaxException("Could not parse fluid value");
	}

}
