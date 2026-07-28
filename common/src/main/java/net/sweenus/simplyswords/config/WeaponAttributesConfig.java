package net.sweenus.simplyswords.config;

import dev.architectury.platform.Platform;
import me.fzzyhmstrs.fzzy_config.annotations.Action;
import me.fzzyhmstrs.fzzy_config.annotations.RequiresAction;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

@RequiresAction(action = Action.RESTART)
public class WeaponAttributesConfig extends Config {

    public WeaponAttributesConfig() {
        super(Identifier.of(SimplySwords.MOD_ID, "weapon_attributes"));
    }

    public TypeDamageModifier typeDamageModifier = new TypeDamageModifier();
    public MaterialDamageModifier materialDamageModifier = new MaterialDamageModifier();
    public TypeAttackSpeed typeAttackSpeed = new TypeAttackSpeed();
    public UniqueDamageModifier uniqueDamageModifier = new UniqueDamageModifier();
    public UniqueAttackSpeed uniqueAttackSpeed = new UniqueAttackSpeed();

    @RequiresAction(action = Action.RESTART)
    public static class TypeDamageModifier extends ConfigSection {
        public float chakram_damageModifier = -1.0f;
        public float claymore_damageModifier = 2.0f;
        public float cutlass_damageModifier = 0.0f;
        public float glaive_damageModifier = 0.0f;
        public float greataxe_damageModifier = 3.0f;
        public float greathammer_damageModifier = 4.0f;
        public float halberd_damageModifier = 3.0f;
        public float katana_damageModifier = 0.0f;
        public float longsword_damageModifier = 0.0f;
        public float rapier_damageModifier = -1.0f;
        public float sai_damageModifier = -3.0f;
        public float scythe_damageModifier = 1.0f;
        public float spear_damageModifier = 0.0f;
        public float twinblade_damageModifier = 0.0f;
        public float warglaive_damageModifier = 0.0f;
    }

    @RequiresAction(action = Action.RESTART)
    public static class MaterialDamageModifier extends ConfigSection {
        public float iron_damageModifier = 3.0f;
        public float gold_damageModifier = 3.0f;
        public float diamond_damageModifier = 3.0f;
        public float netherite_damageModifier = 3.0f;
        public float runic_damageModifier = 3.0f;

        //mythic metals compat, all gated behind a mod-loaded condition
        public ValidatedCondition<Float> adamantite_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> aquarium_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> banglum_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> bronze_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> carmot_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> celestium_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> copper_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> durasteel_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> kyber_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> metallurgium_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> mythril_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> orichalcum_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> osmium_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> palladium_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> prometheum_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> quadrillum_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> runite_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> starPlatinum_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> steel_damageModifier = createCondition(3.0f, "mythicmetals");
        public ValidatedCondition<Float> stormyx_damageModifier = createCondition(3.0f, "mythicmetals");

        //gobber compat - all mod-loaded gated
        public ValidatedCondition<Float> gobber_damageModifier = createCondition(1.0f, "gobber2");
        public ValidatedCondition<Float> gobberNether_damageModifier = createCondition(3.0f, "gobber2");
        public ValidatedCondition<Float> gobberEnd_damageModifier = createCondition(6.0f, "gobber2");

        private ValidatedCondition<Float> createCondition(float defaultValue, String modNeeded) {
            return new ValidatedFloat(defaultValue)
                    .toCondition(
                            () -> Platform.isModLoaded(modNeeded),
                            Text.translatable("simplyswords.weapon_attributes.materialDamageModifier." + modNeeded),
                            () -> defaultValue
                    ).withFailTitle(Text.translatable("simplyswords.weapon_attributes.materialDamageModifier." + modNeeded + ".failTitle"));
        }
    }

    @RequiresAction(action = Action.RESTART)
    public static class TypeAttackSpeed extends ConfigSection {
        public float chakram_attackSpeed = -3.0f;
        public float claymore_attackSpeed = -2.8f;
        public float cutlass_attackSpeed = -2.0f;
        public float glaive_attackSpeed = -2.6f;
        public float greataxe_attackSpeed = -3.1f;
        public float greathammer_attackSpeed = -3.2f;
        public float halberd_attackSpeed = -2.8f;
        public float katana_attackSpeed = -2.0f;
        public float longsword_attackSpeed = -2.4f;
        public float rapier_attackSpeed = -1.8f;
        public float sai_attackSpeed = -1.5f;
        public float scythe_attackSpeed = -2.7f;
        public float spear_attackSpeed = -2.7f;
        public float twinblade_attackSpeed = -2.0f;
        public float warglaive_attackSpeed = -2.2f;


    }

    @RequiresAction(action = Action.RESTART)
    public static class UniqueDamageModifier extends ConfigSection {
        public float arcanethyst_damageModifier = 7.0f;
        public float bramblethorn_damageModifier = 3.0f;
        public float brimstone_damageModifier = 6.0f;
        public float caelestis_damageModifier = 6.0f;
        public float emberblade_damageModifier = 3.0f;
        public float emberlash_damageModifier = 0.0f;
        public float enigma_damageModifier = 7.0f;
        public float flamewind_damageModifier = 3.0f;
        public float frostfall_damageModifier = 5.0f;
        public float harbinger_damageModifier = 3.0f;
        public float hearthflame_damageModifier = 8.0f;
        public float hiveheart_damageModifier = 7.0f;
        public float icewhisper_damageModifier = 7.0f;
        public float lichblade_damageModifier = 7.0f;
        public float livyatan_damageModifier = 4.0f;
        public float longswordofplague_damageModifier = 3.0f;
        public float magiblade_damageModifier = 3.0f;
        public float magiscythe_damageModifier = 4.0f;
        public float magispear_damageModifier = 4.0f;
        public float mjolnir_damageModifier = 3.0f;
        public float moltenedge_damageModifier = 4.0f;
        public float ribboncleaver_damageModifier = 7.0f;
        public float shadowsting_damageModifier = 1.0f;
        public float soulkeeper_damageModifier = 8.0f;
        public float soulpyre_damageModifier = 7.0f;
        public float soulrender_damageModifier = 3.0f;
        public float soulstealer_damageModifier = 0.0f;
        public float starsedge_damageModifier = 3.0f;
        public float stormsedge_damageModifier = 3.0f;
        public float stormbringer_damageModifier = 3.0f;
        public float sunfire_damageModifier = 3.0f;
        public float swordonastick_damageModifier = 5.0f;
        public float tempest_damageModifier = 0.0f;
        public float thewatcher_damageModifier = 6.0f;
        public float thunderbrand_damageModifier = 7.0f;
        public float twistedblade_damageModifier = 0.0f;
        public float watchingwarglaive_damageModifier = 3.0f;
        public float waxweaver_damageModifier = 6.0f;
        public float whisperwind_damageModifier = 3.0f;
        public float wickpiercer_damageModifier = 4.0f;
        public float wraithfang_damageModifier = 1.0f;
        public float chompolotl_damageModifier = 0.0f;

        //eldritch end compat
        public ValidatedCondition<Float> dreadtide_damageModifier = new ValidatedFloat(3.0f)
                .toCondition(
                        () -> SimplySwords.passVersionCheck("eldritch_end", SimplySwords.minimumEldritchEndVersion),
                        Text.translatable("simplyswords.weapon_attributes.uniqueDamageModifier.eldritch_end"),
                        () -> 3.0f
                ).withFailTitle(Text.translatable("simplyswords.weapon_attributes.uniqueDamageModifier.eldritch_end.failTitle"));
    }

    @RequiresAction(action = Action.RESTART)
    public static class UniqueAttackSpeed extends ConfigSection {
        public float arcanethyst_attackSpeed = -2.7f;
        public float bramblethorn_attackSpeed = -1.8f;
        public float brimstone_attackSpeed = -2.8f;
        public float caelestis_attackSpeed = -2.9f;
        public float emberblade_attackSpeed = -2.4f;
        public float emberlash_attackSpeed = -1.5f;
        public float enigma_attackSpeed = -3.2f;
        public float flamewind_attackSpeed = -2.6f;
        public float frostfall_attackSpeed = -2.5f;
        public float harbinger_attackSpeed = -2.4f;
        public float hearthflame_attackSpeed = -3.2f;
        public float hiveheart_attackSpeed = -3.0f;
        public float icewhisper_attackSpeed = -2.7f;
        public float lichblade_attackSpeed = -3.1f;
        public float livyatan_attackSpeed = -2.1f;
        public float longswordofplague_attackSpeed = -2.4f;
        public float mjolnir_attackSpeed = -3.0f;
        public float magiblade_attackSpeed = -2.0f;
        public float magiscythe_attackSpeed = -2.4f;
        public float magispear_attackSpeed = -2.5f;
        public float moltenedge_attackSpeed = -2.1f;
        public float ribboncleaver_attackSpeed = -3.2f;
        public float shadowsting_attackSpeed = -1.7f;
        public float soulkeeper_attackSpeed = -2.9f;
        public float soulpyre_attackSpeed = -3.0f;
        public float soulrender_attackSpeed = -2.4f;
        public float soulstealer_attackSpeed = -1.5f;
        public float starsedge_attackSpeed = -2.0f;
        public float stormsedge_attackSpeed = -2.0f;
        public float stormbringer_attackSpeed = -2.4f;
        public float sunfire_attackSpeed = -2.4f;
        public float swordonastick_attackSpeed = -2.6f;
        public float tempest_attackSpeed = -2.5f;
        public float thewatcher_attackSpeed = -2.8f;
        public float thunderbrand_attackSpeed = -2.7f;
        public float twistedblade_attackSpeed = -2.6f;
        public float watchingwarglaive_attackSpeed = -2.2f;
        public float waxweaver_attackSpeed = -2.9f;
        public float whisperwind_attackSpeed = -2.0f;
        public float wickpiercer_attackSpeed = -2.1f;
        public float wraithfang_attackSpeed = -2.0f;
        public float chompolotl_attackSpeed = -2.5f;

        //eldritch end compat
        public ValidatedCondition<Float> dreadtide_attackSpeed = new ValidatedFloat(-2.0f)
                .toCondition(
                        () -> SimplySwords.passVersionCheck("eldritch_end", SimplySwords.minimumEldritchEndVersion),
                        Text.translatable("simplyswords.weapon_attributes.uniqueDamageModifier.eldritch_end"),
                        () -> -2.0f
                ).withFailTitle(Text.translatable("simplyswords.weapon_attributes.uniqueDamageModifier.eldritch_end.failTitle"));
    }

}
