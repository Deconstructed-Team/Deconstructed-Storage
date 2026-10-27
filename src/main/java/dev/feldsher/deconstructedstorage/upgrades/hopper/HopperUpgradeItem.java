package dev.feldsher.deconstructedstorage.upgrades.hopper;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

import java.util.function.IntSupplier;

public class HopperUpgradeItem extends UpgradeItemBase<HopperUpgradeWrapper> {
	public static final UpgradeType<HopperUpgradeWrapper> TYPE = new UpgradeType<>(HopperUpgradeWrapper::new);
	private final IntSupplier inputFilterSlotCount;
	private final IntSupplier outputFilterSlotCount;
	private final IntSupplier transferSpeedTicks;
	private final IntSupplier maxTransferStackSize;

	public HopperUpgradeItem(IntSupplier inputFilterSlotCount, IntSupplier outputFilterSlotCount, IntSupplier transferSpeedTicks, IntSupplier maxTransferStackSize,
			IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.inputFilterSlotCount = inputFilterSlotCount;
		this.outputFilterSlotCount = outputFilterSlotCount;
		this.transferSpeedTicks = transferSpeedTicks;
		this.maxTransferStackSize = maxTransferStackSize;
	}

	@Override
	public UpgradeType<HopperUpgradeWrapper> getType() {
		return TYPE;
	}

	public int getInputFilterSlotCount() {
		return inputFilterSlotCount.getAsInt();
	}

	public int getOutputFilterSlotCount() {
		return outputFilterSlotCount.getAsInt();
	}

	public int getTransferSpeedTicks() {
		return transferSpeedTicks.getAsInt();
	}

	public int getMaxTransferStackSize() {
		return maxTransferStackSize.getAsInt();
	}
}
