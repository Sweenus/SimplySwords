package net.sweenus.simplyswords;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.compat.SpellSchoolDisplay;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public class SimplySwordsExpectPlatform {
    /**
     * We can use {@link Platform#getConfigFolder()} but this is just an example of {@link ExpectPlatform}.
     * <p>
     * This must be a <b>public static</b> method. The platform-implemented solution must be placed under a
     * platform sub-package, with its class suffixed with {@code Impl}.
     * <p>
     * Example:
     * Expect: net.examplemod.ExampleExpectPlatform#getConfigDirectory()
     * Actual Fabric: net.examplemod.fabric.ExampleExpectPlatformImpl#getConfigDirectory()
     * Actual Forge: net.examplemod.forge.ExampleExpectPlatformImpl#getConfigDirectory()
     * <p>
     * <a href="https://plugins.jetbrains.com/plugin/16210-architectury">You should also get the IntelliJ plugin to help with @ExpectPlatform.</a>
     */
    @ExpectPlatform
    public static Path getConfigDirectory() {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }
    @ExpectPlatform
    public static String getVersion() {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }
    @ExpectPlatform
    public static float getSpellPowerDamage(float damageModifier, LivingEntity entity, Identifier scalingProfileId) {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }

    public static float getSpellPowerDamage(float damageModifier, LivingEntity entity, String legacySchool) {
        return getSpellPowerDamage(damageModifier, entity,
                SpellScalingProfile.fromLegacyName(legacySchool).registryId());
    }

    @ExpectPlatform
    public static String getSpellSchoolDisplayKey(Identifier scalingProfileId) {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }

    public static String getSpellSchoolDisplayKey(String legacySchool) {
        return getSpellSchoolDisplayKey(SpellScalingProfile.fromLegacyName(legacySchool).registryId());
    }

    @ExpectPlatform
    public static List<Identifier> getSpellPowerSchoolIds() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<Identifier> getIronsSpellSchoolIds() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static SpellSchoolDisplay getActiveSpellSchoolDisplay(Identifier scalingId) {
        throw new AssertionError();
    }

    @Nullable
    @ExpectPlatform
    public static RegistryEntry<EntityAttribute> getSpellPowerAttribute(Identifier scalingId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static DamageSource getAbilityMagicDamageSource(ServerWorld world, LivingEntity actor,
                                                            Identifier scalingProfileId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static float getAbilityMagicResistanceMultiplier(LivingEntity target, Identifier scalingProfileId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int applySpellCooldownReduction(int baseTicks, LivingEntity actor) {
        throw new AssertionError();
    }

    /**
     * Whether a mana system is present. False on platforms without Iron's Spells, where costs are skipped.
     */
    @ExpectPlatform
    public static boolean hasManaSystem() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean hasMana(LivingEntity entity, float amount) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void spendMana(LivingEntity entity, float amount) {
        throw new AssertionError();
    }
}
