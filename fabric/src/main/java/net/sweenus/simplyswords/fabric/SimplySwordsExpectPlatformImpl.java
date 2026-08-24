package net.sweenus.simplyswords.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.compat.SpellSchoolDisplay;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;

import java.nio.file.Path;
import java.util.List;

import static net.sweenus.simplyswords.SimplySwords.MOD_ID;

public class SimplySwordsExpectPlatformImpl {
    /**
     * This is our actual method to {@link SimplySwordsExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
    public static String getVersion() {return FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata().getVersion().toString();}
    public static float getSpellPowerDamage(float damageModifier, LivingEntity player, Identifier scalingProfileId) {return FabricHelperMethods.useSpellAttributeScaling(damageModifier, player, scalingProfileId);}
    public static float getSpellPowerDamage(float damageModifier, LivingEntity player, String legacySchool) {return FabricHelperMethods.useSpellAttributeScaling(damageModifier, player, legacySchool);}
    public static String getSpellSchoolDisplayKey(Identifier scalingProfileId) {return FabricHelperMethods.spellSchoolDisplayKey(scalingProfileId);}
    public static String getSpellSchoolDisplayKey(String legacySchool) {return FabricHelperMethods.spellSchoolDisplayKey(legacySchool);}
    public static List<Identifier> getSpellPowerSchoolIds() {return FabricHelperMethods.spellPowerSchoolIds();}
    public static List<Identifier> getIronsSpellSchoolIds() {return List.of();}
    public static SpellSchoolDisplay getActiveSpellSchoolDisplay(Identifier scalingId) {return FabricHelperMethods.activeSpellSchoolDisplay(scalingId);}
    public static RegistryEntry<EntityAttribute> getSpellPowerAttribute(Identifier scalingId) {return FabricHelperMethods.spellPowerAttribute(scalingId);}
    public static DamageSource getAbilityMagicDamageSource(ServerWorld world, LivingEntity actor, Identifier scalingProfileId) {return FabricHelperMethods.getAbilityMagicDamageSource(world, actor, scalingProfileId);}
    public static float getAbilityMagicResistanceMultiplier(LivingEntity target, Identifier scalingProfileId) {return FabricHelperMethods.getAbilityMagicResistanceMultiplier(target, scalingProfileId);}
    public static int applySpellCooldownReduction(int baseTicks, LivingEntity actor) {return FabricHelperMethods.applySpellCooldownReduction(baseTicks, actor);}
    public static boolean hasManaSystem() {return ManaCostImpl.hasManaSystem();}
    public static boolean hasMana(net.minecraft.entity.LivingEntity entity, float amount) {return ManaCostImpl.hasMana(entity, amount);}
    public static void spendMana(net.minecraft.entity.LivingEntity entity, float amount) {ManaCostImpl.spendMana(entity, amount);}
}
