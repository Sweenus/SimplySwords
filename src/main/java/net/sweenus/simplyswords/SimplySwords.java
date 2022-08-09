package net.sweenus.simplyswords;

import net.fabricmc.api.ModInitializer;
import net.sweenus.simplyswords.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplySwords implements ModInitializer {
	public static final String MOD_ID = "simplyswords";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		ModItems.registerModItems();

	}
}
