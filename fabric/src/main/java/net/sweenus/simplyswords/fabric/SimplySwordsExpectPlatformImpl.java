package net.sweenus.simplyswords.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;

import java.nio.file.Path;

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
    public static String getSpellSchoolDisplayKey(Identifier scalingProfileId) {return FabricHelperMethods.spellSchoolDisplayKey(scalingProfileId);}
    public static DamageSource getAbilityMagicDamageSource(ServerWorld world, LivingEntity actor, Identifier scalingProfileId) {return FabricHelperMethods.getAbilityMagicDamageSource(world, actor, scalingProfileId);}
    public static float getAbilityMagicResistanceMultiplier(LivingEntity target, Identifier scalingProfileId) {return FabricHelperMethods.getAbilityMagicResistanceMultiplier(target, scalingProfileId);}
    public static int applySpellCooldownReduction(int baseTicks, LivingEntity actor) {return baseTicks;}
    public static boolean hasManaSystem() {return ManaCostImpl.hasManaSystem();}
    public static boolean hasMana(LivingEntity entity, float amount) {return ManaCostImpl.hasMana(entity, amount);}
    public static void spendMana(LivingEntity entity, float amount) {ManaCostImpl.spendMana(entity, amount);}
}
