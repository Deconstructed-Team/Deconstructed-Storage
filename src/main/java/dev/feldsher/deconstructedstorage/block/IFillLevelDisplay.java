package dev.feldsher.deconstructedstorage.block;

import java.util.List;

/** A storage that shows on its front how full each of its slots is. */
public interface IFillLevelDisplay {
	boolean shouldShowFillLevels();

	void toggleFillLevelVisibility();

	List<Float> getSlotFillLevels();
}
