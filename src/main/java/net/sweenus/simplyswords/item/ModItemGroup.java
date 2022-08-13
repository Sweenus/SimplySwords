package net.sweenus.simplyswords.item;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

public class ModItemGroup {
    public static final ItemGroup SIMPLYSWORDS = FabricItemGroupBuilder.build(new Identifier(SimplySwords.MOD_ID, "simplyswords"),
            () -> new ItemStack(ModItems.RUNIC_SWORD));
}
