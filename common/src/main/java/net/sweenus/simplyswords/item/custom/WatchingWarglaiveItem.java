package net.sweenus.simplyswords.item.custom;

import net.minecraft.item.ToolMaterial;
import net.sweenus.simplyswords.world.WatcherWeaponType;

public class WatchingWarglaiveItem extends WatcherSwordItem {

	public WatchingWarglaiveItem(ToolMaterial toolMaterial, Settings settings) {
		super(toolMaterial, settings);
	}

	@Override
	protected WatcherWeaponType getWatcherWeaponType() {
		return WatcherWeaponType.WARGLAIVE;
	}
}
