package dev.feldsher.deconstructedstorage.block;

import java.util.List;

/** A storage that shows on its front how many items each of its slots holds. */
public interface ICountDisplay {
	boolean shouldShowCounts();

	void toggleCountVisibility();

	List<Integer> getSlotCounts();
}
