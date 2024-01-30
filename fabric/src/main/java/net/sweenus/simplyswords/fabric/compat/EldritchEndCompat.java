package net.sweenus.simplyswords.fabric.compat;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.fabric.item.DreadtideSwordItem;
import net.sweenus.simplyswords.item.ModToolMaterial;

public class EldritchEndCompat {

    //Compat for Eldritch End
    static float wickpiercer_damage_modifier = Config.getFloat("wickpiercer_damageModifier", "WeaponAttributes", ConfigDefaultValues.wickpiercer_damageModifier);
    static float wickpiercer_attackspeed = Config.getFloat("wickpiercer_attackSpeed", "WeaponAttributes", ConfigDefaultValues.wickpiercer_attackSpeed);

    public static final Item DREADTIDE = registerItem( "dreadtide",
            new DreadtideSwordItem(ModToolMaterial.UNIQUE,
                    (int) (wickpiercer_damage_modifier),
                    wickpiercer_attackspeed,
                    new Item.Settings().rarity(Rarity.EPIC).fireproof()));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Eldritch End compat Items for " + SimplySwords.MOD_ID);
    }

}
