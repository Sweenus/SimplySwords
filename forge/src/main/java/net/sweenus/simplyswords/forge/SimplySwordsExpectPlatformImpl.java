package net.sweenus.simplyswords.forge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.sweenus.simplyswords.compat.SpellSchoolDisplay;

import java.nio.file.Path;
import java.util.List;

import static net.sweenus.simplyswords.SimplySwords.MOD_ID;

public class SimplySwordsExpectPlatformImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static String getVersion() {return ModList.get().getModContainerById(MOD_ID).map(it -> it.getModInfo().getVersion().toString()).orElseThrow();}
    public static float getSpellPowerDamage(float damageModifier, LivingEntity player, Identifier scalingProfileId) {return ForgeHelperMethods.useSpellAttributeScaling(damageModifier, player, scalingProfileId);}
    public static String getSpellSchoolDisplayKey(Identifier scalingProfileId) {return ForgeHelperMethods.spellSchoolDisplayKey(scalingProfileId);}
    public static List<Identifier> getSpellPowerSchoolIds() {return ForgeHelperMethods.spellPowerSchoolIds();}
    public static List<Identifier> getIronsSpellSchoolIds() {return ForgeHelperMethods.ironsSpellSchoolIds();}
    public static SpellSchoolDisplay getActiveSpellSchoolDisplay(Identifier scalingId) {return ForgeHelperMethods.activeSpellSchoolDisplay(scalingId);}
    public static EntityAttribute getSpellPowerAttribute(Identifier scalingId) {return ForgeHelperMethods.spellPowerAttribute(scalingId);}
    public static DamageSource getAbilityMagicDamageSource(ServerWorld world, LivingEntity actor, Identifier scalingProfileId) {return ForgeHelperMethods.getAbilityMagicDamageSource(world, actor, scalingProfileId);}
    public static float getAbilityMagicResistanceMultiplier(LivingEntity target, Identifier scalingProfileId) {return ForgeHelperMethods.getAbilityMagicResistanceMultiplier(target, scalingProfileId);}
    public static int applySpellCooldownReduction(int baseTicks, LivingEntity actor) {return ForgeHelperMethods.applySpellCooldownReduction(baseTicks, actor);}
    public static boolean hasManaSystem() {return ForgeHelperMethods.hasManaSystem();}
    public static boolean hasMana(LivingEntity entity, float amount) {return ForgeHelperMethods.hasMana(entity, amount);}
    public static void spendMana(LivingEntity entity, float amount) {ForgeHelperMethods.spendMana(entity, amount);}
}
