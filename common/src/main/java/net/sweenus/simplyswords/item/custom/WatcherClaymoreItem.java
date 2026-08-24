package net.sweenus.simplyswords.item.custom;

import net.minecraft.item.ToolMaterial;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.world.WatcherWeaponType;

public class WatcherClaymoreItem extends WatcherSwordItem  implements TwoHandedWeapon {

	public WatcherClaymoreItem(ToolMaterial toolMaterial, Settings settings) {
		super(toolMaterial, settings);
	}

	@Override
	protected WatcherWeaponType getWatcherWeaponType() {
		return WatcherWeaponType.CLAYMORE;
	}
}
