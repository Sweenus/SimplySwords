package net.sweenus.simplyswords.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.SwordItem;
import net.sweenus.simplyswords.SimplySwords;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.item.custom.FreezeSwordItem;
import net.sweenus.simplyswords.item.custom.LevitationSwordItem;
import net.sweenus.simplyswords.item.custom.PoisonSwordItem;
import net.sweenus.simplyswords.item.custom.SpeedSwordItem;

public class ModItems {
   // public static final Item ITEMNAMEHERE = registerItem( "item_name",
        //new Item(new FabricItemSettings().group(ItemGroup.MISC)));

    public static final Item ELECTRIC_SWORD = registerItem( "electric_sword",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 3, -1.8f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item TOXIC_SWORD = registerItem( "toxic_sword",
            new PoisonSwordItem(ModToolMaterial.ELEMENTITE, -1, -2.5f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item RDIAMOND_SWORD = registerItem( "rdiamond_sword",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 2, -2f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item CURVED_SWORD = registerItem( "curved_sword",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 0, -2f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item RUNIC_SWORD = registerItem( "runic_sword",
            new FreezeSwordItem(ModToolMaterial.ELEMENTITE, 1, -2f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item RDIAMOND_CLAYMORE = registerItem( "rdiamond_claymore",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 4, -2.6f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item ELECTRIC_CLAYMORE = registerItem( "electric_claymore",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 5, -2.5f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    public static final Item TOXIC_CLAYMORE = registerItem( "toxic_claymore",
            new PoisonSwordItem(ModToolMaterial.ELEMENTITE, 1, -2.8f,
                    new FabricItemSettings().group(ItemGroup.COMBAT)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }

    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mod Items for " + SimplySwords.MOD_ID);
    }

}
