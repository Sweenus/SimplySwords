package net.sweenus.simplyswords.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.SwordItem;
import net.sweenus.simplyswords.SimplySwords;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.item.custom.LevitationSwordItem;
import net.sweenus.simplyswords.item.custom.SpeedSwordItem;

public class ModItems {
    public static final Item MYTHRIL_INGOT = registerItem( "mythril_ingot",
        new Item(new FabricItemSettings().group(ItemGroup.MISC)));

    public static final Item MYTHRIL_NUGGET = registerItem( "mythril_nugget",
            new Item(new FabricItemSettings().group(ItemGroup.MISC)));

    public static final Item ELECTRIC_SWORD = registerItem( "electric_sword",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 8, 1.4f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item RDIAMOND_SWORD = registerItem( "rdiamond_sword",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 7, 1.5f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item RDIAMOND_CLAYMORE = registerItem( "rdiamond_claymore",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 10, 1.9f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item ELECTRIC_CLAYMORE = registerItem( "electric_claymore",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 11, 1.8f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }

    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mod Items for " + SimplySwords.MOD_ID);
    }

}
