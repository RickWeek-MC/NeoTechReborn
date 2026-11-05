package techreborn.client.compat.jei;

import reborncore.client.gui.GuiBuilder.ProgressDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything goes in a machine recipe. Coordinates are the ones the REI plugin used upstream
 * (relative to a recipe area that had a 4 pixel border); {@link #OFFSET} moves them into JEI's space,
 * where the border is added by JEI itself.
 */
final class Layout {
	static final int OFFSET = 4;

	record ItemSlot(boolean output, int index, int x, int y, boolean bigResult) {
	}

	record Bar(int x, int y, ProgressDirection direction) {
	}

	record Tank(boolean output, int x, int y, boolean downwards) {
	}

	int width = 150 - 2 * OFFSET;
	int height = 66 - 2 * OFFSET;
	final List<ItemSlot> slots = new ArrayList<>();
	final List<Bar> bars = new ArrayList<>();
	Tank tank;
	boolean energy = true;
	int energyY = 8;
	int labelX = 150 - 5;
	int labelY = 5;
	boolean labelRight = true;
	boolean showHeat;

	Layout in(int index, int reiX, int reiY) {
		slots.add(new ItemSlot(false, index, reiX, reiY, false));
		return this;
	}

	Layout out(int index, int reiX, int reiY) {
		slots.add(new ItemSlot(true, index, reiX, reiY, false));
		return this;
	}

	/** An output slot drawn with the big "result" frame, like a furnace output. */
	Layout result(int index, int reiX, int reiY) {
		slots.add(new ItemSlot(true, index, reiX, reiY, true));
		return this;
	}

	Layout bar(int reiX, int reiY, ProgressDirection direction) {
		bars.add(new Bar(reiX, reiY, direction));
		return this;
	}

	Layout fluid(boolean output, int reiX, int reiY, boolean downwards) {
		tank = new Tank(output, reiX, reiY, downwards);
		return this;
	}

	Layout label(int reiX, int reiY, boolean right) {
		labelX = reiX;
		labelY = reiY;
		labelRight = right;
		return this;
	}

	Layout height(int reiHeight) {
		height = reiHeight - 2 * OFFSET;
		return this;
	}

	Layout energyAt(int reiY) {
		energyY = reiY;
		return this;
	}

	Layout heat() {
		showHeat = true;
		return this;
	}

	static Layout oneInOneOut() {
		return new Layout()
			.in(0, 46, 26).result(0, 46 + 46, 26)
			.bar(46 + 21, 30, ProgressDirection.RIGHT);
	}

	static Layout twoInCenterOut() {
		return new Layout()
			.in(0, 29, 26).in(1, 29 + 92, 26).result(0, 29 + 46, 26)
			.bar(29 + 21, 30, ProgressDirection.RIGHT)
			.bar(29 + 71, 30, ProgressDirection.LEFT);
	}

	static Layout assembling() {
		return new Layout()
			.in(0, 55 - 9, 35 - 19).in(1, 55 - 9, 55 - 19).result(0, 101 - 9, 45 - 19)
			.bar(76 - 9, 48 - 19, ProgressDirection.RIGHT);
	}

	static Layout blastFurnace() {
		return new Layout()
			.in(0, 55 - 17, 35 - 19).in(1, 55 - 17, 55 - 19)
			.out(0, 101 - 17, 45 - 19).out(1, 101 + 20 - 17, 45 - 19)
			.bar(76 - 17, 48 - 19, ProgressDirection.RIGHT)
			.heat().label(24, 5, false);
	}

	static Layout distillation() {
		return new Layout()
			.in(0, 55 - 23, 35 - 19).in(1, 55 - 23, 55 - 19)
			.out(0, 101 - 23, 45 - 19).out(1, 101 + 20 - 23, 45 - 19).out(2, 101 + 40 - 23, 45 - 19)
			.bar(76 - 23, 48 - 19, ProgressDirection.RIGHT);
	}

	static Layout electrolyzer() {
		return new Layout()
			.in(0, 55 - 20, 41).in(1, 55, 41)
			.out(0, 55 + 17 - 9 - 20, 36 - 22).out(1, 55 + 17 - 9, 36 - 22)
			.out(2, 55 + 17 - 9 + 20, 36 - 22).out(3, 55 + 17 - 9 + 40, 36 - 22)
			.bar(55 + 21, 36 + 4, ProgressDirection.UP)
			.label(150 - 17, 66 - 13, true);
	}

	static Layout industrialGrinder() {
		return new Layout()
			.in(0, 55, 36)
			.out(0, 55 + 46, 36 - 9 - 18).out(1, 55 + 46, 36 - 9)
			.out(2, 55 + 46, 36 - 9 + 18).out(3, 55 + 46, 36 - 9 + 36)
			.bar(55 + 21, 36 + 4, ProgressDirection.RIGHT)
			.fluid(false, 55 - 26, 18, true)
			.energyAt(18).label(51, 15, false).height(88);
	}

	static Layout implosion() {
		return new Layout()
			.in(0, 55 - 15, 35 - 19).in(1, 55 - 15, 55 - 19)
			.out(0, 97 - 15, 45 - 19).out(1, 97 + 18 - 15, 45 - 19)
			.bar(76 - 15, 48 - 19, ProgressDirection.RIGHT);
	}

	static Layout centrifuge() {
		return new Layout()
			.in(0, 55 - 17, 35 - 19).in(1, 55 - 17, 55 - 19)
			.out(0, 97 - 17, 45 - 19).out(1, 116 - 17, 26 - 19)
			.out(2, 135 - 17, 45 - 19).out(3, 116 - 17, 64 - 19)
			.bar(76 - 17, 48 - 19, ProgressDirection.RIGHT)
			.label(24, 5, false);
	}

	static Layout sawmill() {
		return new Layout()
			.in(0, 55, 26)
			.out(0, 55 + 46, 26 - 18).out(1, 55 + 46, 26).out(2, 55 + 46, 26 + 18)
			.bar(55 + 21, 30, ProgressDirection.RIGHT)
			.fluid(false, 55 - 26, 8, true)
			.label(51, 5, false);
	}

	static Layout fluidReplicator() {
		return new Layout()
			.in(0, 46, 26)
			.bar(46 + 21, 30, ProgressDirection.RIGHT)
			.fluid(true, 46 + 46, 8, false)
			.label(24, 5, false);
	}
}
