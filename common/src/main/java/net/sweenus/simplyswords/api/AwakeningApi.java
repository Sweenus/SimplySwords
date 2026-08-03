package net.sweenus.simplyswords.api;

import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.component.AwakeningComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.Optional;

public final class AwakeningApi {
    private static final double PLAYER_BASE_DAMAGE = 1.0D;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;
    private static final float LEGACY_DEFAULT_DORMANT_ATTRIBUTE_MULTIPLIER = 0.25F;
    private static final String DAMAGE_SCALE_KEY = "SimplySwordsAwakeningDamageScale";
    private static final String SPEED_SCALE_KEY = "SimplySwordsAwakeningSpeedScale";

    private AwakeningApi() {
    }


    // Returns whether ordinary unique-weapon awakening is enabled globally.

    public static boolean isAwakeningSystemEnabled() {
        return Config.general.enableUniqueWeaponAwakening;
    }

     // Returns whether this stack should use its stored awakening progression.
     //
     // Form families registered with persistent progression (including the
     // Lichblade and Dormant Relic) remain active when the global system is
     // disabled. Other awakenable weapons behave as fully awakened while
     // retaining their stored level.
     //
    public static boolean usesAwakeningProgression(ItemStack stack) {
        if (!AwakeningProfileRegistry.isAwakenable(stack)) {
            return false;
        }
        return isAwakeningSystemEnabled() || isPersistentProgressionWeapon(stack);
    }

    public static int getLevel(ItemStack stack) {
        if (!AwakeningProfileRegistry.isAwakenable(stack)) {
            return AwakeningComponent.MAX_LEVEL;
        }
        if (!usesAwakeningProgression(stack)) {
            return AwakeningComponent.MAX_LEVEL;
        }
        AwakeningComponent component = ComponentTypeRegistry.AWAKENING.get(stack);
        if (component != null) {
            return component.level();
        }
        // These two registry ids represented the explicitly dormant legacy stages.
        if (stack.isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get())
                || stack.isOf(ItemsRegistry.DORMANT_RELIC.get())) {
            return 0;
        }
        return AwakeningComponent.MAX_LEVEL;
    }

    public static void ensureInitialized(ItemStack stack) {
        if (!AwakeningProfileRegistry.isAwakenable(stack)) {
            return;
        }
        AwakeningFormRegistry.ensureInitialized(stack);
        if (!usesAwakeningProgression(stack)) {
            clearScaledAttributes(stack);
            return;
        }
        if (ComponentTypeRegistry.AWAKENING.contains(stack)) {
            int level = getLevel(stack);
            float damageScale = getAttributeMultiplier(stack, level);
            float speedScale = getAttackSpeedMultiplier(stack, level);
            if (!hasExpectedScales(stack, damageScale, speedScale)) {
                rebuildAttributes(stack);
            }
            return;
        }
        setLevel(stack, getLevel(stack));
    }

    public static ItemStack initializeNaturalDrop(ItemStack stack) {
        setLevel(stack, 0);
        return stack;
    }

    public static ItemStack initializeFullyAwakened(ItemStack stack) {
        setLevel(stack, AwakeningComponent.MAX_LEVEL);
        return stack;
    }

    public static void setLevel(ItemStack stack, int requestedLevel) {
        if (!AwakeningProfileRegistry.isAwakenable(stack)) {
            return;
        }
        int level = net.minecraft.util.math.MathHelper.clamp(requestedLevel, 0, AwakeningComponent.MAX_LEVEL);
        ComponentTypeRegistry.AWAKENING.set(stack, new AwakeningComponent(level));
        rebuildAttributes(stack);
    }

    public static float getAttributeMultiplier(ItemStack stack) {
        return getAttributeMultiplier(stack, getLevel(stack));
    }

    private static float getAttributeMultiplier(ItemStack stack, int level) {
        AwakeningProfile profile = AwakeningProfileRegistry.get(stack).orElse(AwakeningProfile.DEFAULT);
        return interpolateMultiplier(profile.dormantAttributeMultiplier(), level);
    }

    //Returns the independently scaled attack-speed strength for this stack.
    public static float getAttackSpeedMultiplier(ItemStack stack) {
        return getAttackSpeedMultiplier(stack, getLevel(stack));
    }

    private static float getAttackSpeedMultiplier(ItemStack stack, int level) {
        AwakeningProfile profile = AwakeningProfileRegistry.get(stack).orElse(AwakeningProfile.DEFAULT);
        return interpolateMultiplier(profile.dormantAttackSpeedMultiplier(), level);
    }

    private static float interpolateMultiplier(float dormantMultiplier, int level) {
        float progress = net.minecraft.util.math.MathHelper.clamp(level, 0, AwakeningComponent.MAX_LEVEL)
                / (float) AwakeningComponent.MAX_LEVEL;
        return dormantMultiplier + (1.0F - dormantMultiplier) * progress;
    }

    private static boolean usesDefaultProfile(ItemStack stack) {
        return AwakeningProfileRegistry.get(stack)
                .orElse(AwakeningProfile.DEFAULT)
                .equals(AwakeningProfile.DEFAULT);
    }

    private static boolean isPersistentProgressionWeapon(ItemStack stack) {
        return AwakeningFormRegistry.isPersistentProgression(stack)
                || stack.isOf(ItemsRegistry.DECAYING_RELIC.get());
    }

    //
    // Returns the route locked onto this stack, if it belongs to a branching
    // awakening family and has selected one.
    //
    public static Optional<Identifier> getFormRoute(ItemStack stack) {
        return AwakeningFormRegistry.getRoute(stack);
    }

    //
    // Returns the level-resolved form metadata used by names, models, tooltips,
    // and addon weapon logic.
    //
    public static Optional<AwakeningFormStage> getFormStage(ItemStack stack) {
        return AwakeningFormRegistry.getStage(stack);
    }

    public static Optional<Identifier> getFormId(ItemStack stack) {
        return getFormStage(stack).map(AwakeningFormStage::id);
    }

    public static float getFormModelValue(ItemStack stack) {
        return getFormStage(stack).map(AwakeningFormStage::modelValue).orElse(0.0F);
    }

    public static float getEffectMultiplier(ItemStack stack) {
        AwakeningProfile profile = AwakeningProfileRegistry.get(stack).orElse(AwakeningProfile.DEFAULT);
        int level = getLevel(stack);
        return level < profile.abilityUnlockLevel()
                ? 0.0F
                : level / (float) AwakeningComponent.MAX_LEVEL;
    }

    public static boolean isAbilityUnlocked(ItemStack stack) {
        return getLevel(stack) >= getAbilityUnlockLevel(stack);
    }

     // Returns the awakening level at which this weapon's unique ability becomes active.
     // Tooltip integrations and addons should use this instead of reading the profile
     // registry directly.
    public static int getAbilityUnlockLevel(ItemStack stack) {
        return AwakeningProfileRegistry.get(stack)
                .orElse(AwakeningProfile.DEFAULT)
                .abilityUnlockLevel();
    }

    public static float scaleEffect(ItemStack stack, float fullValue) {
        return fullValue * getEffectMultiplier(stack);
    }

    public static double scaleEffect(ItemStack stack, double fullValue) {
        return fullValue * getEffectMultiplier(stack);
    }

    public static int scaleChance(ItemStack stack, int fullChance) {
        return net.minecraft.util.math.MathHelper.clamp(Math.round(fullChance * getEffectMultiplier(stack)), 0, 100);
    }

    //
    // Returns the strength available to socketed gem powers on this stack.
    //
    // Awakenable weapons follow the same unlock threshold and level curve as
    // their unique effects. Non-awakenable weapons, including runic weapons,
    // always return 1.0F.
    //
    public static float getGemPowerMultiplier(ItemStack stack) {
        return getEffectMultiplier(stack);
    }

    public static boolean areGemPowersActive(ItemStack stack) {
        return getGemPowerMultiplier(stack) > 0.0F;
    }

    public static float scaleGemPower(ItemStack stack, float fullValue) {
        return fullValue * getGemPowerMultiplier(stack);
    }

    public static double scaleGemPower(ItemStack stack, double fullValue) {
        return fullValue * getGemPowerMultiplier(stack);
    }

    //
    // Scales a duration while preserving at least one tick for an active power.
    //
    public static int scaleGemPowerDuration(ItemStack stack, int fullDuration) {
        if (fullDuration <= 0) {
            return 0;
        }
        float multiplier = getGemPowerMultiplier(stack);
        return multiplier <= 0.0F ? 0 : Math.max(1, Math.round(fullDuration * multiplier));
    }

    public static void rebuildAttributes(ItemStack stack) {
        float damageMultiplier = getAttributeMultiplier(stack);
        float attackSpeedMultiplier = getAttackSpeedMultiplier(stack);
        stack.removeSubNbt("AttributeModifiers");

        if (damageMultiplier < 0.99999F || attackSpeedMultiplier < 0.99999F) {
            Multimap<EntityAttribute, EntityAttributeModifier> defaults =
                    stack.getItem().getAttributeModifiers(EquipmentSlot.MAINHAND);
            defaults.forEach((attribute, modifier) -> {
                double value = modifier.getValue();
                if (attribute == EntityAttributes.GENERIC_ATTACK_DAMAGE) {
                    value = (PLAYER_BASE_DAMAGE + value) * damageMultiplier - PLAYER_BASE_DAMAGE;
                } else if (attribute == EntityAttributes.GENERIC_ATTACK_SPEED) {
                    value = (PLAYER_BASE_ATTACK_SPEED + value) * attackSpeedMultiplier - PLAYER_BASE_ATTACK_SPEED;
                }
                stack.addAttributeModifier(attribute,
                        new EntityAttributeModifier(modifier.getId(), modifier.getName(), value, modifier.getOperation()),
                        EquipmentSlot.MAINHAND);
            });
        }

        stack.getOrCreateNbt().putFloat(DAMAGE_SCALE_KEY, damageMultiplier);
        stack.getOrCreateNbt().putFloat(SPEED_SCALE_KEY, attackSpeedMultiplier);
    }

    private static boolean hasExpectedScales(ItemStack stack, float damage, float speed) {
        if (!stack.hasNbt()) return false;
        return Math.abs(stack.getNbt().getFloat(DAMAGE_SCALE_KEY) - damage) < 0.00001F
                && Math.abs(stack.getNbt().getFloat(SPEED_SCALE_KEY) - speed) < 0.00001F;
    }

    private static void clearScaledAttributes(ItemStack stack) {
        stack.removeSubNbt("AttributeModifiers");
        if (stack.hasNbt()) {
            stack.getNbt().remove(DAMAGE_SCALE_KEY);
            stack.getNbt().remove(SPEED_SCALE_KEY);
        }
    }
}
