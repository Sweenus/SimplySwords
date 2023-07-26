package net.sweenus.simplyswords.util;

import dev.architectury.event.events.common.LootEvent;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.EnchantRandomlyLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public class ModLootTableModifiers {

    public static void init() {


        //STANDARD
        LootEvent.MODIFY_LOOT_TABLE.register(((lootTables, id, context, builtin) -> {
            if (SimplySwords.lootConfig.enableLootDrops && id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                if (!SimplySwords.lootConfig.enableLootInVillages && id.getPath().contains("village")) {
                    //Do nothing
                }
                else {
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(SimplySwords.lootConfig.standardLootTableWeight)) // 1 = 100% of the time
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
            if (SimplySwords.lootConfig.enableLootDrops && id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                if (!SimplySwords.lootConfig.enableLootInVillages && id.getPath().contains("village")) {
                    //Do nothing
                }
                else {
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(SimplySwords.lootConfig.rareLootTableWeight)) // 1 = 100% of the time
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
            if (SimplySwords.lootConfig.enableLootDrops && id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                if (!SimplySwords.lootConfig.enableLootInVillages && id.getPath().contains("village")) {
                    //Do nothing
                }
                else {
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(SimplySwords.lootConfig.runicLootTableWeight)) // 1 = 100% of the time
                            .with(ItemEntry.builder(ItemsRegistry.RUNIC_TABLET.get()));
                    context.addPool(pool);
                }
            }
        }));

        //UNIQUE
        // Check each loot table against the listed namespaces in the loot_config.json, if there's a match modify the
        // table according to the config. Otherwise, use the loot global loot modifiers set in the general_config.json
        LootEvent.MODIFY_LOOT_TABLE.register(((lootTables, id, context, builtin) -> {
            if (SimplySwords.lootConfig.enableLootDrops) {
                if (SimplySwordsConfig.getLootList(id.toString())) {
                    float lootChance = SimplySwordsConfig.getLootModifiers(id.toString());
                    if (lootChance > 0.0) { // If chance is set to 0 treat as a blacklist and don't inject the loot at all
                        LootPool.Builder pool = LootPool.builder()
                                .rolls(ConstantLootNumberProvider.create(1))
                                .conditionally(RandomChanceLootCondition.builder(lootChance));

                        if (SimplySwords.lootConfig.enableTheWatcher)
                            pool.with(ItemEntry.builder(ItemsRegistry.WATCHER_CLAYMORE.get()));
                        if (SimplySwords.lootConfig.enableWatchingWarglaive)
                            pool.with(ItemEntry.builder(ItemsRegistry.WATCHING_WARGLAIVE.get()));
                        if (SimplySwords.lootConfig.enableLongswordOfThePlague)
                            pool.with(ItemEntry.builder(ItemsRegistry.TOXIC_LONGSWORD.get()));
                        if (SimplySwords.lootConfig.enableSwordOnAStick)
                            pool.with(ItemEntry.builder(ItemsRegistry.SWORD_ON_A_STICK.get()));
                        if (SimplySwords.lootConfig.enableBramblethorn)
                            pool.with(ItemEntry.builder(ItemsRegistry.BRAMBLETHORN.get()));
                        if (SimplySwords.lootConfig.enableStormsEdge)
                            pool.with(ItemEntry.builder(ItemsRegistry.STORMS_EDGE.get()));
                        if (SimplySwords.lootConfig.enableStormbringer)
                            pool.with(ItemEntry.builder(ItemsRegistry.STORMBRINGER.get()));
                        if (SimplySwords.lootConfig.enableMjolnir)
                            pool.with(ItemEntry.builder(ItemsRegistry.MJOLNIR.get()));
                        if (SimplySwords.lootConfig.enableEmberblade)
                            pool.with(ItemEntry.builder(ItemsRegistry.EMBERBLADE.get()));
                        if (SimplySwords.lootConfig.enableHearthflame)
                            pool.with(ItemEntry.builder(ItemsRegistry.HEARTHFLAME.get()));
                        if (SimplySwords.lootConfig.enableTwistedBlade)
                            pool.with(ItemEntry.builder(ItemsRegistry.TWISTED_BLADE.get()));
                        if (SimplySwords.lootConfig.enableSoulrender)
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULRENDER.get()));
                        if (SimplySwords.lootConfig.enableSoulpyre)
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULPYRE.get()));
                        if (SimplySwords.lootConfig.enableSoulkeeper)
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULKEEPER.get()));
                        if (SimplySwords.lootConfig.enableSoulstealer)
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULSTEALER.get()));
                        if (SimplySwords.lootConfig.enableFrostfall)
                            pool.with(ItemEntry.builder(ItemsRegistry.FROSTFALL.get()));
                        if (SimplySwords.lootConfig.enableMoltenEdge)
                            pool.with(ItemEntry.builder(ItemsRegistry.MOLTEN_EDGE.get()));
                        if (SimplySwords.lootConfig.enableLivyatan)
                            pool.with(ItemEntry.builder(ItemsRegistry.LIVYATAN.get()));
                        if (SimplySwords.lootConfig.enableIcewhisper)
                            pool.with(ItemEntry.builder(ItemsRegistry.ICEWHISPER.get()));
                        if (SimplySwords.lootConfig.enableArcanethyst)
                            pool.with(ItemEntry.builder(ItemsRegistry.ARCANETHYST.get()));
                        if (SimplySwords.lootConfig.enableThunderbrand)
                            pool.with(ItemEntry.builder(ItemsRegistry.THUNDERBRAND.get()));
                        if (SimplySwords.lootConfig.enableBrimstone)
                            pool.with(ItemEntry.builder(ItemsRegistry.BRIMSTONE_CLAYMORE.get()));
                        if (SimplySwords.lootConfig.enableSlumberingLichblade)
                            pool.with(ItemEntry.builder(ItemsRegistry.SLUMBERING_LICHBLADE.get()));
                        if (SimplySwords.lootConfig.enableShadowsting)
                            pool.with(ItemEntry.builder(ItemsRegistry.SHADOWSTING.get()));
                        if (SimplySwords.lootConfig.enableDormantRelic)
                            pool.with(ItemEntry.builder(ItemsRegistry.DORMANT_RELIC.get()));
                        if (SimplySwords.lootConfig.enableWhisperwind)
                            pool.with(ItemEntry.builder(ItemsRegistry.WHISPERWIND.get()));

                        context.addPool(pool);
                    }
                }
                else {
                    if (id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                        LootPool.Builder pool = LootPool.builder()
                                .rolls(ConstantLootNumberProvider.create(1))
                                .conditionally(RandomChanceLootCondition.builder(SimplySwords.lootConfig.uniqueLootTableWeight)); // 1 = 100% of the time
                        if (SimplySwords.lootConfig.enableTheWatcher)
                            pool.with(ItemEntry.builder(ItemsRegistry.WATCHER_CLAYMORE.get()));
                        if (SimplySwords.lootConfig.enableWatchingWarglaive)
                            pool.with(ItemEntry.builder(ItemsRegistry.WATCHING_WARGLAIVE.get()));
                        if (SimplySwords.lootConfig.enableLongswordOfThePlague)
                            pool.with(ItemEntry.builder(ItemsRegistry.TOXIC_LONGSWORD.get()));
                        if (SimplySwords.lootConfig.enableSwordOnAStick)
                            pool.with(ItemEntry.builder(ItemsRegistry.SWORD_ON_A_STICK.get()));
                        if (SimplySwords.lootConfig.enableBramblethorn)
                            pool.with(ItemEntry.builder(ItemsRegistry.BRAMBLETHORN.get()));
                        if (SimplySwords.lootConfig.enableStormsEdge)
                            pool.with(ItemEntry.builder(ItemsRegistry.STORMS_EDGE.get()));
                        if (SimplySwords.lootConfig.enableStormbringer)
                            pool.with(ItemEntry.builder(ItemsRegistry.STORMBRINGER.get()));
                        if (SimplySwords.lootConfig.enableMjolnir)
                            pool.with(ItemEntry.builder(ItemsRegistry.MJOLNIR.get()));
                        if (SimplySwords.lootConfig.enableEmberblade)
                            pool.with(ItemEntry.builder(ItemsRegistry.EMBERBLADE.get()));
                        if (SimplySwords.lootConfig.enableHearthflame)
                            pool.with(ItemEntry.builder(ItemsRegistry.HEARTHFLAME.get()));
                        if (SimplySwords.lootConfig.enableTwistedBlade)
                            pool.with(ItemEntry.builder(ItemsRegistry.TWISTED_BLADE.get()));
                        if (SimplySwords.lootConfig.enableSoulrender)
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULRENDER.get()));
                        if (SimplySwords.lootConfig.enableSoulpyre)
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULPYRE.get()));
                        if (SimplySwords.lootConfig.enableSoulkeeper)
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULKEEPER.get()));
                        if (SimplySwords.lootConfig.enableSoulstealer)
                            pool.with(ItemEntry.builder(ItemsRegistry.SOULSTEALER.get()));
                        if (SimplySwords.lootConfig.enableFrostfall)
                            pool.with(ItemEntry.builder(ItemsRegistry.FROSTFALL.get()));
                        if (SimplySwords.lootConfig.enableMoltenEdge)
                            pool.with(ItemEntry.builder(ItemsRegistry.MOLTEN_EDGE.get()));
                        if (SimplySwords.lootConfig.enableLivyatan)
                            pool.with(ItemEntry.builder(ItemsRegistry.LIVYATAN.get()));
                        if (SimplySwords.lootConfig.enableIcewhisper)
                            pool.with(ItemEntry.builder(ItemsRegistry.ICEWHISPER.get()));
                        if (SimplySwords.lootConfig.enableArcanethyst)
                            pool.with(ItemEntry.builder(ItemsRegistry.ARCANETHYST.get()));
                        if (SimplySwords.lootConfig.enableThunderbrand)
                            pool.with(ItemEntry.builder(ItemsRegistry.THUNDERBRAND.get()));
                        if (SimplySwords.lootConfig.enableBrimstone)
                            pool.with(ItemEntry.builder(ItemsRegistry.BRIMSTONE_CLAYMORE.get()));
                        if (SimplySwords.lootConfig.enableSlumberingLichblade)
                            pool.with(ItemEntry.builder(ItemsRegistry.SLUMBERING_LICHBLADE.get()));
                        if (SimplySwords.lootConfig.enableShadowsting)
                            pool.with(ItemEntry.builder(ItemsRegistry.SHADOWSTING.get()));
                        if (SimplySwords.lootConfig.enableDormantRelic)
                            pool.with(ItemEntry.builder(ItemsRegistry.DORMANT_RELIC.get()));
                        if (SimplySwords.lootConfig.enableWhisperwind)
                            pool.with(ItemEntry.builder(ItemsRegistry.WHISPERWIND.get()));

                        context.addPool(pool);
                    }
                }
            }
        }));

    }
}