package net.sweenus.simplyswords.util;

import dev.architectury.event.events.common.LootEvent;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.EnchantRandomlyLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public class ModLootTableModifiers {

    public static void init() {
        LootEvent.MODIFY_LOOT_TABLE.register(((lootTables, id, context, builtin) -> {
            if (SimplySwordsConfig.getBooleanValue("add_weapons_to_loot_tables") && id.getPath().contains("chests") && !id.getPath().contains("village")) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(SimplySwordsConfig.getGeneralSettings("standard_loot_table_weight"))) // 1 = 100% of the time
                        .apply(EnchantRandomlyLootFunction.builder())
                        .with(ItemEntry.builder(ItemsRegistry.IRON_LONGSWORD.get()));

                context.addPool(pool);
            }
        }));

    }

/*
            if (SimplySwordsConfig.getBooleanValue("add_weapons_to_loot_tables") && id.getPath().contains("chests") && !id.getPath().contains("village")) {

                //STANDARD POOL
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(SimplySwordsConfig.getGeneralSettings("standard_loot_table_weight"))) // 1 = 100% of the time
                        .apply(EnchantRandomlyLootFunction.builder())
                        .with(ItemEntry.builder(ModItems.IRON_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.IRON_TWINBLADE))
                        .with(ItemEntry.builder(ModItems.IRON_RAPIER))
                        .with(ItemEntry.builder(ModItems.IRON_CUTLASS))
                        .with(ItemEntry.builder(ModItems.IRON_KATANA))
                        .with(ItemEntry.builder(ModItems.IRON_GLAIVE))
                        .with(ItemEntry.builder(ModItems.IRON_WARGLAIVE))
                        .with(ItemEntry.builder(ModItems.IRON_SPEAR))
                        .with(ItemEntry.builder(ModItems.IRON_SAI))
                        .with(ItemEntry.builder(ModItems.IRON_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.IRON_CHAKRAM))
                        .with(ItemEntry.builder(ModItems.IRON_GREATAXE))
                        .with(ItemEntry.builder(ModItems.IRON_GREATHAMMER))
                        .with(ItemEntry.builder(ModItems.GOLD_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.GOLD_TWINBLADE))
                        .with(ItemEntry.builder(ModItems.GOLD_RAPIER))
                        .with(ItemEntry.builder(ModItems.GOLD_CUTLASS))
                        .with(ItemEntry.builder(ModItems.GOLD_KATANA))
                        .with(ItemEntry.builder(ModItems.GOLD_GLAIVE))
                        .with(ItemEntry.builder(ModItems.GOLD_WARGLAIVE))
                        .with(ItemEntry.builder(ModItems.GOLD_SPEAR))
                        .with(ItemEntry.builder(ModItems.GOLD_SAI))
                        .with(ItemEntry.builder(ModItems.GOLD_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.GOLD_GREATHAMMER))
                        .with(ItemEntry.builder(ModItems.GOLD_CHAKRAM))
                        .with(ItemEntry.builder(ModItems.GOLD_GREATAXE));
                tableBuilder.pool(pool);

                //RARE POOL
                LootPool.Builder rpool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(SimplySwordsConfig.getGeneralSettings("rare_loot_table_weight"))) // 1 = 100% of the time
                        .apply(EnchantRandomlyLootFunction.builder())
                        .with(ItemEntry.builder(ModItems.RUNIC_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.RUNIC_TWINBLADE))
                        .with(ItemEntry.builder(ModItems.RUNIC_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.RUNIC_RAPIER))
                        .with(ItemEntry.builder(ModItems.RUNIC_CUTLASS))
                        .with(ItemEntry.builder(ModItems.RUNIC_KATANA))
                        .with(ItemEntry.builder(ModItems.RUNIC_GLAIVE))
                        .with(ItemEntry.builder(ModItems.RUNIC_SPEAR))
                        .with(ItemEntry.builder(ModItems.RUNIC_SAI))
                        .with(ItemEntry.builder(ModItems.RUNIC_GREATHAMMER))
                        .with(ItemEntry.builder(ModItems.RUNIC_GREATAXE))
                        .with(ItemEntry.builder(ModItems.RUNIC_CHAKRAM))
                        .with(ItemEntry.builder(ModItems.DIAMOND_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.DIAMOND_TWINBLADE))
                        .with(ItemEntry.builder(ModItems.DIAMOND_RAPIER))
                        .with(ItemEntry.builder(ModItems.DIAMOND_CUTLASS))
                        .with(ItemEntry.builder(ModItems.DIAMOND_KATANA))
                        .with(ItemEntry.builder(ModItems.DIAMOND_SPEAR))
                        .with(ItemEntry.builder(ModItems.DIAMOND_GLAIVE))
                        .with(ItemEntry.builder(ModItems.DIAMOND_WARGLAIVE))
                        .with(ItemEntry.builder(ModItems.DIAMOND_SAI))
                        .with(ItemEntry.builder(ModItems.DIAMOND_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.DIAMOND_GREATHAMMER))
                        .with(ItemEntry.builder(ModItems.DIAMOND_CHAKRAM))
                        .with(ItemEntry.builder(ModItems.DIAMOND_GREATAXE));
                tableBuilder.pool(rpool);

                //UNIQUE POOL
                LootPool.Builder upool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(SimplySwordsConfig.getGeneralSettings("unique_loot_table_weight"))) // 1 = 100% of the time
                        .with(ItemEntry.builder(ModItems.WATCHER_CLAYMORE))
                        .with(ItemEntry.builder(ModItems.TOXIC_LONGSWORD))
                        .with(ItemEntry.builder(ModItems.SWORD_ON_A_STICK))
                        .with(ItemEntry.builder(ModItems.BRAMBLETHORN))
                        .with(ItemEntry.builder(ModItems.STORMS_EDGE))
                        .with(ItemEntry.builder(ModItems.STORMBRINGER))
                        .with(ItemEntry.builder(ModItems.MJOLNIR))
                        .with(ItemEntry.builder(ModItems.EMBERBLADE))
                        .with(ItemEntry.builder(ModItems.HEARTHFLAME))
                        .with(ItemEntry.builder(ModItems.TWISTED_BLADE))
                        .with(ItemEntry.builder(ModItems.SOULRENDER))
                        .with(ItemEntry.builder(ModItems.SOULKEEPER))
                        .with(ItemEntry.builder(ModItems.SOULSTEALER))
                        .with(ItemEntry.builder(ModItems.BRIMSTONE_CLAYMORE));
                tableBuilder.pool(upool);

            }

        });
    }
    */
}