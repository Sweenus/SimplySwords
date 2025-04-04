package net.sweenus.simplyswords.util;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.architectury.event.events.common.LootEvent;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.config.LootConfig;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.TagRegistry;

import java.util.List;

public class ModLootTableModifiers {

    //supplies a list of every unique sword item currently registered
    private static final Supplier<List<Item>> swords = Suppliers.memoize(() -> Registries.ITEM.stream().filter(it -> it instanceof UniqueSwordItem).toList());



    public static void init() {

        //STANDARD
        LootEvent.MODIFY_LOOT_TABLE.register(((RegistryKey<LootTable> key, LootEvent.LootTableModificationContext context, boolean builtin) -> {
            Identifier id = key.getValue();
            if (LootConfig.INSTANCE.enableLootDrops.get() && id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                //System.out.println( id.getNamespace() + ":" + id.getPath()); //PRINT POSSIBLE PATHS
                if (LootConfig.INSTANCE.enableLootInVillages.get() || !id.getPath().contains("village")) {
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(LootConfig.INSTANCE.standardLootTableWeight.get() / 100))
                            .apply(EnchantRandomlyFromTagLootFunction.create(EnchantmentTags.DAMAGE_EXCLUSIVE_SET))
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
        LootEvent.MODIFY_LOOT_TABLE.register(((RegistryKey<LootTable> key, LootEvent.LootTableModificationContext context, boolean builtin) -> {
            Identifier id = key.getValue();
            if (LootConfig.INSTANCE.enableLootDrops.get() && id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                if (LootConfig.INSTANCE.enableLootInVillages.get() || !id.getPath().contains("village")) {
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(LootConfig.INSTANCE.rareLootTableWeight.get() / 100))
                            .apply(EnchantRandomlyFromTagLootFunction.create(EnchantmentTags.DAMAGE_EXCLUSIVE_SET)) //This is not ideal, but forge doesn't expose the registry wrapper so...
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

        //Runic / Rare 2
        LootEvent.MODIFY_LOOT_TABLE.register(((RegistryKey<LootTable> key, LootEvent.LootTableModificationContext context, boolean builtin) -> {
            Identifier id = key.getValue();
            if (LootConfig.INSTANCE.enableLootDrops.get() && id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                if (LootConfig.INSTANCE.enableLootInVillages.get() || !id.getPath().contains("village")) {
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(LootConfig.INSTANCE.runicLootTableWeight.get() / 100))
                            .with(ItemEntry.builder(ItemsRegistry.RUNIC_TABLET.get()));
                    context.addPool(pool);
                }
            }
        }));

        //UNIQUE
        // Check each loot table against the listed namespaces in the loot_config.json, if there's a match modify the
        // table according to the config. Otherwise, use the loot global loot modifiers set in the general_config.json

        LootEvent.MODIFY_LOOT_TABLE.register(((RegistryKey<LootTable> key, LootEvent.LootTableModificationContext context, boolean builtin) -> {
            Identifier id = key.getValue();
            if (LootConfig.INSTANCE.enableLootDrops.get()) {
                Float lootChance = LootConfig.INSTANCE.uniqueLootTableOptions.get(id);
                if (lootChance != null && lootChance > 0f) {

                    LootPool.Builder pool = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(lootChance / 100));

                    swords.get().stream()
                            .filter(item ->
                                    // Check if the item is not disabled in the loot configuration
                                    !LootConfig.INSTANCE.disabledUniqueWeaponLoot.contains(item)
                                            // Filter out non-lootable uniques
                                            && TagRegistry.isInTag(TagRegistry.lootableUniques, item.asItem())
                            )
                            .forEach(item ->
                                    pool.with(ItemEntry.builder(item))
                            );

                    context.addPool(pool);
                }
                else {
                    if (id.getPath().contains("chests") && !id.getPath().contains("spectrum")) {
                        LootPool.Builder pool = LootPool.builder()
                                .rolls(ConstantLootNumberProvider.create(1))
                                .conditionally(RandomChanceLootCondition.builder(LootConfig.INSTANCE.uniqueLootTableWeight.get() / 100));

                        swords.get().stream()
                                .filter(item ->
                                        // Check if the item is not disabled in the loot configuration
                                        !LootConfig.INSTANCE.disabledUniqueWeaponLoot.contains(item)
                                                // Filter out non-lootable uniques
                                                && TagRegistry.isInTag(TagRegistry.lootableUniques, item.asItem())
                                )
                                .forEach(item ->
                                        pool.with(ItemEntry.builder(item))
                                );



                        context.addPool(pool);
                    }
                }
            }
        }));


    }
}