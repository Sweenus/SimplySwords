package net.sweenus.simplyswords.config;

import com.google.common.collect.ImmutableMap;
import me.fzzyhmstrs.fzzy_config.annotations.Action;
import me.fzzyhmstrs.fzzy_config.annotations.RequiresAction;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedSet;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedRegistryType;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.minecraft.item.Item;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.TagRegistry;

@RequiresAction(action = Action.RELOAD_DATA)
public class LootConfig extends Config {

    public LootConfig() {
        super(Identifier.of(SimplySwords.MOD_ID, "loot"));
    }

    public static final LootConfig INSTANCE = ConfigApiJava.registerAndLoadConfig(LootConfig::new);

    public ValidatedBoolean enableLootDrops = new ValidatedBoolean(true);
    public ValidatedBoolean enableLootInVillages = new ValidatedBoolean(false);

    public ValidatedFloat standardLootTableWeight = new ValidatedFloat(0.1f, 100f, 0f);
    public ValidatedFloat rareLootTableWeight = new ValidatedFloat(0.4f, 100f, 0f);
    public ValidatedFloat runicLootTableWeight = new ValidatedFloat(0.7f, 100f, 0f);
    public ValidatedFloat uniqueLootTableWeight = new ValidatedFloat(0.3f, 100f, 0f);

    //contained remnants will be disabled by default if the unique loot chance is set to 0
    public ValidatedCondition<Boolean> enableContainedRemnants = new ValidatedBoolean()
            .toCondition(
                    () -> uniqueLootTableWeight.get() > 0f,
                    Text.translatable("simplyswords.loot.enableContainedRemnants.condition"),
                    () -> false
            ).withFailTitle(Text.translatable("simplyswords.loot.enableContainedRemnants.failTitle"));

    //Unique table options are copied over from the previous sub-config system.
    //This map only allows non-block loot table ids, and chances clamped 0f to 1f.
    //if you ever use another toDynamicKey, the predicate id should be unique unless the predicate is exactly the same (non-block)
    public ValidatedIdentifierMap<Float> uniqueLootTableOptions = new ValidatedIdentifierMap.Builder<Float>()
            .keyHandler(ValidatedIdentifier.ofDynamicKey(LootTables.END_CITY_TREASURE_CHEST.getValue(), RegistryKeys.LOOT_TABLE, "simplyswords_no_blocks", (id, e) -> e.value().getType() != LootContextTypes.BLOCK))
            .valueHandler(new ValidatedFloat(0.1f, 100f, 0f))
            .defaults(
                    ImmutableMap.<Identifier, Float>builder()
                            .put(Identifier.ofVanilla("entities/wither"), 5f)
                            .put(Identifier.ofVanilla("entities/ender_dragon"), 50f)
                            .put(Identifier.ofVanilla("chests/ruined_portal"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_armorer"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_butcher"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_cartographer"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_desert_house"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_fisher"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_fletcher"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_mason"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_plains_house"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_savanna_house"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_shepherd"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_snowy_house"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_taiga_house"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_tannery"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_temple"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_toolsmith"), 0f)
                            .put(Identifier.ofVanilla("chests/village/village_weaponsmith"), 0f)
                            .build()
            ).build();


    // This now validates using the "simplyswords:lootable_uniques" tag, instead of all items,
    // meaning non-lootable uniques do not show up in the autocompletion
    public ValidatedSet<Item> disabledUniqueWeaponLoot = ValidatedRegistryType.of(ItemsRegistry.ARCANETHYST.value(), Registries.ITEM, (entry) -> TagRegistry.isInTag(TagRegistry.lootableUniques, entry.value())).toSet();

    //
}