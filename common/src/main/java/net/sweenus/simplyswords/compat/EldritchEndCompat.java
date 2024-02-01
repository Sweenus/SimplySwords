package net.sweenus.simplyswords.compat;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.eldritch_end.EldritchEndCompatMaterial;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.custom.DreadtideSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public class EldritchEndCompat {

    //Compat for Eldritch End
    static float dreadtide_damage_modifier = Config.getFloat("dreadtide_damageModifier", "WeaponAttributes", ConfigDefaultValues.dreadtide_damageModifier);
    static float dreadtide_attackspeed = Config.getFloat("dreadtide_attackSpeed", "WeaponAttributes", ConfigDefaultValues.dreadtide_attackSpeed);

    public static final DeferredRegister<Item> ITEM = ItemsRegistry.ITEM;

    public static final RegistrySupplier<DreadtideSwordItem> DREADTIDE = ITEM.register( "dreadtide", () ->
            new DreadtideSwordItem(EldritchEndCompatMaterial.ABERRATION,
                    (int) (dreadtide_damage_modifier),
                    dreadtide_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Eldritch End compat Items for " + SimplySwords.MOD_ID);
    }

}
