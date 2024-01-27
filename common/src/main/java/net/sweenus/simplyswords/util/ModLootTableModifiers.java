package net.sweenus.simplyswords.util;

import dev.architectury.event.events.common.LootEvent;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.EnchantRandomlyLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public class ModLootTableModifiers {

    public static void init() {

        float standardLootWeight = Config.getFloat("standardLootTableWeight", "Loot", ConfigDefaultValues.standardLootTableWeight);
        float rareLootWeight = Config.getFloat("rareLootTableWeight", "Loot", ConfigDefaultValues.rareLootTableWeight);
        float runicLootWeight = Config.getFloat("runicLootTableWeight", "Loot", ConfigDefaultValues.runicLootTableWeight);
        float uniqueLootWeight = Config.getFloat("uniqueLootTableWeight", "Loot", ConfigDefaultValues.uniqueLootTableWeight);


        //STANDARD
        LootEvent.MODIFY_LOOT_TABLE.register(((lootTables, id, context, builtin) -> {
            if (Config.getBoolean("enableLootDrops", "Loot", ConfigDefaultValues.enableLootDrops) && id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                //System.out.println( id.getNamespace() + ":" + id.getPath()); PRINT POSSIBLE PATHS
                if (!Config.getBoolean("enableLootInVillages", "Loot", ConfigDefaultValues.enableLootInVillages) && id.getPath().contains("village")) {
                    //Do nothing
                }
                else {
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(standardLootWeight)) // 1 = 100% of the time
                            .apply(EnchantRandomlyLootFunction.builder())
                            .with(ItemEntry.builder(ItemsRegistry.IRON_LONGSWORD.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_TWINBLADE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_RAPIER.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_CUTLASS.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_KATANA.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_GLAIVE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_WARGLAIVE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_SPEAR.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_SAI.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_CLAYMORE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_CHAKRAM.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_GREATAXE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_GREATHAMMER.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_SCYTHE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.IRON_HALBERD.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_LONGSWORD.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_TWINBLADE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_RAPIER.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_CUTLASS.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_KATANA.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_GLAIVE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_WARGLAIVE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_SPEAR.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_SAI.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_CLAYMORE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_GREATHAMMER.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_CHAKRAM.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_GREATAXE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_SCYTHE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.GOLD_HALBERD.get()));

                    context.addPool(pool);
                }
            }
        }));

        //RARE
        LootEvent.MODIFY_LOOT_TABLE.register(((lootTables, id, context, builtin) -> {
            if (Config.getBoolean("enableLootDrops", "Loot", ConfigDefaultValues.enableLootDrops) && id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                if (!Config.getBoolean("enableLootInVillages", "Loot", ConfigDefaultValues.enableLootInVillages) && id.getPath().contains("village")) {
                    //Do nothing
                }
                else {
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(rareLootWeight)) // 1 = 100% of the time
                            .apply(EnchantRandomlyLootFunction.builder())
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_LONGSWORD.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_TWINBLADE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_RAPIER.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_CUTLASS.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_KATANA.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_SPEAR.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_GLAIVE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_WARGLAIVE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_SAI.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_CLAYMORE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_GREATHAMMER.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_CHAKRAM.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_GREATAXE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_SCYTHE.get()))
                            .with(ItemEntry.builder(ItemsRegistry.DIAMOND_HALBERD.get()));

                    context.addPool(pool);
                }
            }
        }));
        //RARE 2
        LootEvent.MODIFY_LOOT_TABLE.register(((lootTables, id, context, builtin) -> {
            if (Config.getBoolean("enableLootDrops", "Loot", ConfigDefaultValues.enableLootDrops) && id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                if (!Config.getBoolean("enableLootInVillages", "Loot", ConfigDefaultValues.enableLootInVillages) && id.getPath().contains("village")) {
                    //Do nothing
                }
                else {
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(runicLootWeight)) // 1 = 100% of the time
                            .with(ItemEntry.builder(ItemsRegistry.RUNIC_TABLET.get()));
                    context.addPool(pool);
                }
            }
        }));

        //UNIQUE
        // Check each loot table against the listed namespaces in the loot_config.json, if there's a match modify the
        // table according to the config. Otherwise, use the loot global loot modifiers set in the general_config.json
        LootEvent.MODIFY_LOOT_TABLE.register(((lootTables, id, context, builtin) -> {
            if (Config.getBoolean("enableLootDrops", "Loot", ConfigDefaultValues.enableLootDrops)) {
                if (SimplySwordsConfig.getLootList(id.toString())) {
                    float lootChance = SimplySwordsConfig.getLootModifiers(id.toString());
                    if (lootChance > 0.0) { // If chance is set to 0 treat as a blacklist and don't inject the loot at all
                        LootPool.Builder pool = LootPool.builder()
                                .rolls(ConstantLootNumberProvider.create(1))
                                .conditionally(RandomChanceLootCondition.builder(lootChance));

                        if (Config.getBoolean("enableTheWatcher", "Loot", ConfigDefaultValues.enableTheWatcher))
                            pool.with(ItemEntry.builder(ItemsRegistry.WATCHER_CLAYMORE.get()));
                        if (Config.getBoolean("enableWatchingWarglaive", "Loot", ConfigDefaultValues.enableWatchingWarglaive))
                            pool.with(ItemEntry.builder(ItemsRegistry.WATCHING_WARGLAIVE.get()));
                        if (Config.getBoolean("enableLongswordOfThePlague", "Loot", ConfigDefaultValues.enableLongswordOfThePlague))
                            pool.with(ItemEntry.builder(ItemsRegistry.TOXIC_LONGSWORD.get()));
                        if (Config.getBoolean("enableSwordOnAStick", "Loot", ConfigDefaultValues.enableSwordOnAStick))
                            pool.with(ItemEntry.builder(ItemsRegistry.SWORD_ON_A_STICK.get()));
                        if (Config.getBoolean("enableBramblethorn", "Loot", ConfigDefaultValues.enableBramblethorn))
                            pool.with(ItemEntry.builder(ItemsRegistry.BRAMBLETHORN.get()));
                        if (Config.getBoolean("enableStormsEdge", "Loot", ConfigDefaultValues.enableStormsEdge))
                            pool.with(ItemEntry.builder(ItemsRegistry.STORMS_EDGE.get()));
                        if (Config.getBoolean("enableStormbringer", "Loot", ConfigDefaultValues.enableStormbringer))
                            pool.with(ItemEntry.builder(ItemsRegistry.STORMBRINGER.get()));
                        if (Config.getBoolean("enableMjolnir", "Loot", ConfigDefaultValues.enableMjolnir))
                            pool.with(ItemEntry.builder(ItemsRegistry.MJOLNIR.get()));
                        if (Config.getBoolean("enableEmberblade", "Loot", ConfigDefaultValues.enableEmberblade))
                            pool.with(ItemEntry.builder(ItemsRegistry.EMBERBLADE.get()));
                        if (Config.getBoolean("enableHearthflame", "Loot", ConfigDefaultValues.enableHearthflame))
                            pool.with(ItemEntry.builder(ItemsRegistry.HEARTHFLAME.get()));
                        if (Config.getBoolean("enableTwistedBlade", "Loot", ConfigDefaultValues.enableTwistedBlade))
                            pool.with(ItemEntry.builder(ItemsRegistry.TWISTED_BLADE.get()));
                        if (Config.getBoolean("enableSoulrender", "Loot", ConfigDefaultValues.enableSoulrender))
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULRENDER.get()));
                        if (Config.getBoolean("enableSoulpyre", "Loot", ConfigDefaultValues.enableSoulpyre))
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULPYRE.get()));
                        if (Config.getBoolean("enableSoulkeeper", "Loot", ConfigDefaultValues.enableSoulkeeper))
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULKEEPER.get()));
                        if (Config.getBoolean("enableSoulstealer", "Loot", ConfigDefaultValues.enableSoulstealer))
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULSTEALER.get()));
                        if (Config.getBoolean("enableFrostfall", "Loot", ConfigDefaultValues.enableFrostfall))
                            pool.with(ItemEntry.builder(ItemsRegistry.FROSTFALL.get()));
                        if (Config.getBoolean("enableMoltenEdge", "Loot", ConfigDefaultValues.enableMoltenEdge))
                            pool.with(ItemEntry.builder(ItemsRegistry.MOLTEN_EDGE.get()));
                        if (Config.getBoolean("enableLivyatan", "Loot", ConfigDefaultValues.enableLivyatan))
                            pool.with(ItemEntry.builder(ItemsRegistry.LIVYATAN.get()));
                        if (Config.getBoolean("enableIcewhisper", "Loot", ConfigDefaultValues.enableIcewhisper))
                            pool.with(ItemEntry.builder(ItemsRegistry.ICEWHISPER.get()));
                        if (Config.getBoolean("enableArcanethyst", "Loot", ConfigDefaultValues.enableArcanethyst))
                            pool.with(ItemEntry.builder(ItemsRegistry.ARCANETHYST.get()));
                        if (Config.getBoolean("enableThunderbrand", "Loot", ConfigDefaultValues.enableThunderbrand))
                            pool.with(ItemEntry.builder(ItemsRegistry.THUNDERBRAND.get()));
                        if (Config.getBoolean("enableBrimstone", "Loot", ConfigDefaultValues.enableBrimstone))
                            pool.with(ItemEntry.builder(ItemsRegistry.BRIMSTONE_CLAYMORE.get()));
                        if (Config.getBoolean("enableSlumberingLichblade", "Loot", ConfigDefaultValues.enableSlumberingLichblade))
                            pool.with(ItemEntry.builder(ItemsRegistry.SLUMBERING_LICHBLADE.get()));
                        if (Config.getBoolean("enableShadowsting", "Loot", ConfigDefaultValues.enableShadowsting))
                            pool.with(ItemEntry.builder(ItemsRegistry.SHADOWSTING.get()));
                        if (Config.getBoolean("enableDormantRelic", "Loot", ConfigDefaultValues.enableDormantRelic))
                            pool.with(ItemEntry.builder(ItemsRegistry.DORMANT_RELIC.get()));
                        if (Config.getBoolean("enableWhisperwind", "Loot", ConfigDefaultValues.enableWhisperwind))
                            pool.with(ItemEntry.builder(ItemsRegistry.WHISPERWIND.get()));
                        if (Config.getBoolean("enableEmberlash", "Loot", ConfigDefaultValues.enableEmberlash))
                            pool.with(ItemEntry.builder(ItemsRegistry.EMBERLASH.get()));
                        if (Config.getBoolean("enableWaxweaver", "Loot", ConfigDefaultValues.enableWaxweaver))
                            pool.with(ItemEntry.builder(ItemsRegistry.WAXWEAVER.get()));
                        if (Config.getBoolean("enableHiveheart", "Loot", ConfigDefaultValues.enableHiveheart))
                            pool.with(ItemEntry.builder(ItemsRegistry.HIVEHEART.get()));
                        if (Config.getBoolean("enableStarsEdge", "Loot", ConfigDefaultValues.enableStarsEdge))
                            pool.with(ItemEntry.builder(ItemsRegistry.STARS_EDGE.get()));

                        context.addPool(pool);
                    }
                }
                else {
                    if (id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                        LootPool.Builder pool = LootPool.builder()
                                .rolls(ConstantLootNumberProvider.create(1))
                                .conditionally(RandomChanceLootCondition.builder(uniqueLootWeight)); // 1 = 100% of the time
                        if (Config.getBoolean("enableTheWatcher", "Loot", ConfigDefaultValues.enableTheWatcher))
                            pool.with(ItemEntry.builder(ItemsRegistry.WATCHER_CLAYMORE.get()));
                        if (Config.getBoolean("enableWatchingWarglaive", "Loot", ConfigDefaultValues.enableWatchingWarglaive))
                            pool.with(ItemEntry.builder(ItemsRegistry.WATCHING_WARGLAIVE.get()));
                        if (Config.getBoolean("enableLongswordOfThePlague", "Loot", ConfigDefaultValues.enableLongswordOfThePlague))
                            pool.with(ItemEntry.builder(ItemsRegistry.TOXIC_LONGSWORD.get()));
                        if (Config.getBoolean("enableSwordOnAStick", "Loot", ConfigDefaultValues.enableSwordOnAStick))
                            pool.with(ItemEntry.builder(ItemsRegistry.SWORD_ON_A_STICK.get()));
                        if (Config.getBoolean("enableBramblethorn", "Loot", ConfigDefaultValues.enableBramblethorn))
                            pool.with(ItemEntry.builder(ItemsRegistry.BRAMBLETHORN.get()));
                        if (Config.getBoolean("enableStormsEdge", "Loot", ConfigDefaultValues.enableStormsEdge))
                            pool.with(ItemEntry.builder(ItemsRegistry.STORMS_EDGE.get()));
                        if (Config.getBoolean("enableStormbringer", "Loot", ConfigDefaultValues.enableStormbringer))
                            pool.with(ItemEntry.builder(ItemsRegistry.STORMBRINGER.get()));
                        if (Config.getBoolean("enableMjolnir", "Loot", ConfigDefaultValues.enableMjolnir))
                            pool.with(ItemEntry.builder(ItemsRegistry.MJOLNIR.get()));
                        if (Config.getBoolean("enableEmberblade", "Loot", ConfigDefaultValues.enableEmberblade))
                            pool.with(ItemEntry.builder(ItemsRegistry.EMBERBLADE.get()));
                        if (Config.getBoolean("enableHearthflame", "Loot", ConfigDefaultValues.enableHearthflame))
                            pool.with(ItemEntry.builder(ItemsRegistry.HEARTHFLAME.get()));
                        if (Config.getBoolean("enableTwistedBlade", "Loot", ConfigDefaultValues.enableTwistedBlade))
                            pool.with(ItemEntry.builder(ItemsRegistry.TWISTED_BLADE.get()));
                        if (Config.getBoolean("enableSoulrender", "Loot", ConfigDefaultValues.enableSoulrender))
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULRENDER.get()));
                        if (Config.getBoolean("enableSoulpyre", "Loot", ConfigDefaultValues.enableSoulpyre))
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULPYRE.get()));
                        if (Config.getBoolean("enableSoulkeeper", "Loot", ConfigDefaultValues.enableSoulkeeper))
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULKEEPER.get()));
                        if (Config.getBoolean("enableSoulstealer", "Loot", ConfigDefaultValues.enableSoulstealer))
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULSTEALER.get()));
                        if (Config.getBoolean("enableFrostfall", "Loot", ConfigDefaultValues.enableFrostfall))
                            pool.with(ItemEntry.builder(ItemsRegistry.FROSTFALL.get()));
                        if (Config.getBoolean("enableMoltenEdge", "Loot", ConfigDefaultValues.enableMoltenEdge))
                            pool.with(ItemEntry.builder(ItemsRegistry.MOLTEN_EDGE.get()));
                        if (Config.getBoolean("enableLivyatan", "Loot", ConfigDefaultValues.enableLivyatan))
                            pool.with(ItemEntry.builder(ItemsRegistry.LIVYATAN.get()));
                        if (Config.getBoolean("enableIcewhisper", "Loot", ConfigDefaultValues.enableIcewhisper))
                            pool.with(ItemEntry.builder(ItemsRegistry.ICEWHISPER.get()));
                        if (Config.getBoolean("enableArcanethyst", "Loot", ConfigDefaultValues.enableArcanethyst))
                            pool.with(ItemEntry.builder(ItemsRegistry.ARCANETHYST.get()));
                        if (Config.getBoolean("enableThunderbrand", "Loot", ConfigDefaultValues.enableThunderbrand))
                            pool.with(ItemEntry.builder(ItemsRegistry.THUNDERBRAND.get()));
                        if (Config.getBoolean("enableBrimstone", "Loot", ConfigDefaultValues.enableBrimstone))
                            pool.with(ItemEntry.builder(ItemsRegistry.BRIMSTONE_CLAYMORE.get()));
                        if (Config.getBoolean("enableSlumberingLichblade", "Loot", ConfigDefaultValues.enableSlumberingLichblade))
                            pool.with(ItemEntry.builder(ItemsRegistry.SLUMBERING_LICHBLADE.get()));
                        if (Config.getBoolean("enableShadowsting", "Loot", ConfigDefaultValues.enableShadowsting))
                            pool.with(ItemEntry.builder(ItemsRegistry.SHADOWSTING.get()));
                        if (Config.getBoolean("enableDormantRelic", "Loot", ConfigDefaultValues.enableDormantRelic))
                            pool.with(ItemEntry.builder(ItemsRegistry.DORMANT_RELIC.get()));
                        if (Config.getBoolean("enableWhisperwind", "Loot", ConfigDefaultValues.enableWhisperwind))
                            pool.with(ItemEntry.builder(ItemsRegistry.WHISPERWIND.get()));
                        if (Config.getBoolean("enableEmberlash", "Loot", ConfigDefaultValues.enableEmberlash))
                            pool.with(ItemEntry.builder(ItemsRegistry.EMBERLASH.get()));
                        if (Config.getBoolean("enableWaxweaver", "Loot", ConfigDefaultValues.enableWaxweaver))
                            pool.with(ItemEntry.builder(ItemsRegistry.WAXWEAVER.get()));
                        if (Config.getBoolean("enableHiveheart", "Loot", ConfigDefaultValues.enableHiveheart))
                            pool.with(ItemEntry.builder(ItemsRegistry.HIVEHEART.get()));
                        if (Config.getBoolean("enableStarsEdge", "Loot", ConfigDefaultValues.enableStarsEdge))
                            pool.with(ItemEntry.builder(ItemsRegistry.STARS_EDGE.get()));

                        context.addPool(pool);
                    }
                }
            }
        }));

    }
}