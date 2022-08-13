package net.sweenus.simplyswords.util;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.ModItems;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;

public class ModLootTableModifiers {

    private static final Identifier ELDER_GUARDIAN_ID  = new Identifier("minecraft", "entities/elder_guardian");
    private static final Identifier WARDEN_ID  = new Identifier("minecraft", "entities/warden");

    private static final Identifier JUNGLE_TEMPLE_CHEST_ID  = new Identifier("minecraft", "chests/jungle_temple");

    private static final Identifier ANCIENT_CITY_CHEST_ID  = new Identifier("minecraft", "chests/ancient_city");


    public static void modifyLootTables() {

        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {

            /*if (source.isBuiltin() && ELDER_GUARDIAN_ID.equals(id) || WARDEN_ID.equals(id)) {

                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.1f))
                        .with(ItemEntry.builder(ModItems.RUNIC_SWORD))
                        .with(ItemEntry.builder(ModItems.TOXIC_SWORD))
                        .with(ItemEntry.builder(ModItems.TOXIC_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.ELECTRIC_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.CURVED_SWORD))
                        .with(ItemEntry.builder(ModItems.ELECTRIC_SWORD));
                tableBuilder.pool(pool);

            }*/

            if (SimplySwordsConfig.getBooleanValue("add_weapons_to_loot_tables") && id.getPath().contains("chests") && !id.getPath().contains("village")) {

                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(SimplySwordsConfig.getFloatValue("loot_table_weight"))) // 1 = 100% of the time
                        .with(ItemEntry.builder(ModItems.RUNIC_SWORD))
                        .with(ItemEntry.builder(ModItems.CURVED_SWORD))
                        .with(ItemEntry.builder(ModItems.GOLD_RAPIER))
                        .with(ItemEntry.builder(ModItems.IRON_RAPIER))
                        .with(ItemEntry.builder(ModItems.DIAMOND_RAPIER))
                        .with(ItemEntry.builder(ModItems.NETHERITE_RAPIER))
                        .with(ItemEntry.builder(ModItems.RUNIC_RAPIER))
                        .with(ItemEntry.builder(ModItems.DIAMOND_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.RUNIC_TWINBLADE))
                        .with(ItemEntry.builder(ModItems.TOXIC_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.GOLD_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.IRON_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.RUNIC_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.RUNIC_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.BRIMSTONE_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.IRON_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.GOLD_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.GOLD_CURVED_SWORD))
                        .with(ItemEntry.builder(ModItems.DIAMOND_CURVED_SWORD))
                        .with(ItemEntry.builder(ModItems.DIAMOND_CLAYMORE));
                tableBuilder.pool(pool);
            }

        });
    }
}