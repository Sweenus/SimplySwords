package net.sweenus.simplyswords.registry;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

public class TagRegistry {

    public static TagKey<Item> spearsTag = TagKey.of(RegistryKeys.ITEM, Identifier.of(SimplySwords.MOD_ID, "spears"));
    public static TagKey<Item> lightWeaponsTag = TagKey.of(RegistryKeys.ITEM, Identifier.of(SimplySwords.MOD_ID, "light_weapons"));
    public static TagKey<Item> mediumWeaponsTag = TagKey.of(RegistryKeys.ITEM, Identifier.of(SimplySwords.MOD_ID, "medium_weapons"));
    public static TagKey<Item> heavyWeaponsTag = TagKey.of(RegistryKeys.ITEM, Identifier.of(SimplySwords.MOD_ID, "heavy_weapons"));
    public static TagKey<Item> lootableUniques = TagKey.of(RegistryKeys.ITEM, Identifier.of(SimplySwords.MOD_ID, "lootable_uniques"));


    public static boolean isInTag(TagKey<Item> tag, Item item) {
        return item.getRegistryEntry().isIn(tag);
    }


}
