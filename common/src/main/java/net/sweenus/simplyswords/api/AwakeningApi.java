package net.sweenus.simplyswords.api;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.component.AwakeningComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public final class AwakeningApi {
    private static final double PLAYER_BASE_DAMAGE = 1.0D;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;
    private static final float LEGACY_DEFAULT_DORMANT_ATTRIBUTE_MULTIPLIER = 0.25F;

    private AwakeningApi() {
    }


    // Returns whether ordinary unique-weapon awakening is enabled globally.

    public static boolean isAwakeningSystemEnabled() {
        return Config.general.enableUniqueWeaponAwakening;
    }

     // Returns whether this stack should use its stored awakening progression.
     //
     // Lichblade and Dormant Relic progression remains active when the global
     // system is disabled. Other awakenable weapons behave as fully awakened
     // while retaining their stored level.
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
        AwakeningComponent component = stack.get(ComponentTypeRegistry.AWAKENING.get());
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
        if (!usesAwakeningProgression(stack)) {
            AttributeModifiersComponent full = stack.getItem().getComponents()
                    .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            AttributeModifiersComponent current = stack.getOrDefault(
                    DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            if (!current.equals(full)) {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, full);
            }
            return;
        }
        if (stack.contains(ComponentTypeRegistry.AWAKENING.get())) {
            AttributeModifiersComponent current = stack.getOrDefault(
                    DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            AttributeModifiersComponent full = stack.getItem().getComponents()
                    .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            int level = getLevel(stack);
            AttributeModifiersComponent expected = buildAttributeModifiers(
                    stack,
                    getAttributeMultiplier(stack, level),
                    getAttackSpeedMultiplier(stack, level)
            );
            if (current.equals(expected)) {
                return;
            }

            AttributeModifiersComponent legacyAtCurrentLevel = buildAttributeModifiers(
                    stack,
                    getAttributeMultiplier(stack, level),
                    getAttributeMultiplier(stack, level)
            );
            boolean unscaledPartialWeapon = level < AwakeningComponent.MAX_LEVEL
                    && current.equals(full);
            boolean legacySpeedCurve = level < AwakeningComponent.MAX_LEVEL
                    && current.equals(legacyAtCurrentLevel);
            boolean legacyDefaultDamageCurve = false;
            if (level < AwakeningComponent.MAX_LEVEL && usesDefaultProfile(stack)) {
                float legacyDamageMultiplier = interpolateMultiplier(
                        LEGACY_DEFAULT_DORMANT_ATTRIBUTE_MULTIPLIER,
                        level
                );
                legacyDefaultDamageCurve = current.equals(buildAttributeModifiers(
                        stack,
                        legacyDamageMultiplier,
                        legacyDamageMultiplier
                )) || current.equals(buildAttributeModifiers(
                        stack,
                        legacyDamageMultiplier,
                        getAttackSpeedMultiplier(stack, level)
                ));
            }
            boolean corruptedMaxWeapon = level == AwakeningComponent.MAX_LEVEL
                    && (current.equals(buildAttributeModifiers(
                            stack,
                            getAttributeMultiplier(stack, 0),
                            getAttributeMultiplier(stack, 0)))
                    || current.equals(buildAttributeModifiers(
                            stack,
                            getAttributeMultiplier(stack, 0),
                            getAttackSpeedMultiplier(stack, 0)))
                    || usesDefaultProfile(stack)
                    && (current.equals(buildAttributeModifiers(
                            stack,
                            LEGACY_DEFAULT_DORMANT_ATTRIBUTE_MULTIPLIER,
                            LEGACY_DEFAULT_DORMANT_ATTRIBUTE_MULTIPLIER))
                    || current.equals(buildAttributeModifiers(
                            stack,
                            LEGACY_DEFAULT_DORMANT_ATTRIBUTE_MULTIPLIER,
                            getAttackSpeedMultiplier(stack, 0)))));
            if (unscaledPartialWeapon || legacySpeedCurve || legacyDefaultDamageCurve || corruptedMaxWeapon) {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, expected);
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
        int level = Math.clamp(requestedLevel, 0, AwakeningComponent.MAX_LEVEL);
        stack.set(ComponentTypeRegistry.AWAKENING.get(), new AwakeningComponent(level));
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
        float progress = Math.clamp(level, 0, AwakeningComponent.MAX_LEVEL)
                / (float) AwakeningComponent.MAX_LEVEL;
        return dormantMultiplier + (1.0F - dormantMultiplier) * progress;
    }

    private static boolean usesDefaultProfile(ItemStack stack) {
        return AwakeningProfileRegistry.get(stack)
                .orElse(AwakeningProfile.DEFAULT)
                .equals(AwakeningProfile.DEFAULT);
    }

    private static boolean isPersistentProgressionWeapon(ItemStack stack) {
        return stack.isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get())
                || stack.isOf(ItemsRegistry.WAKING_LICHBLADE.get())
                || stack.isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())
                || stack.isOf(ItemsRegistry.DORMANT_RELIC.get())
                || stack.isOf(ItemsRegistry.RIGHTEOUS_RELIC.get())
                || stack.isOf(ItemsRegistry.TAINTED_RELIC.get())
                || stack.isOf(ItemsRegistry.SUNFIRE.get())
                || stack.isOf(ItemsRegistry.HARBINGER.get())
                || stack.isOf(ItemsRegistry.DECAYING_RELIC.get());
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
        return Math.clamp(Math.round(fullChance * getEffectMultiplier(stack)), 0, 100);
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
        AttributeModifiersComponent rebuilt = buildAttributeModifiers(
                stack,
                getAttributeMultiplier(stack),
                getAttackSpeedMultiplier(stack)
        );
        if (rebuilt.modifiers().isEmpty()) {
            return;
        }
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, rebuilt);
    }

    private static AttributeModifiersComponent buildAttributeModifiers(
            ItemStack stack,
            float attributeMultiplier,
            float attackSpeedMultiplier
    ) {
        AttributeModifiersComponent full = stack.getItem().getComponents()
                .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        if (full.modifiers().isEmpty()) {
            return full;
        }

        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
        for (AttributeModifiersComponent.Entry entry : full.modifiers()) {
            EntityAttributeModifier modifier = entry.modifier();
            double value = modifier.value();
            if (entry.attribute() == EntityAttributes.GENERIC_ATTACK_DAMAGE) {
                value = (PLAYER_BASE_DAMAGE + value) * attributeMultiplier - PLAYER_BASE_DAMAGE;
            } else if (entry.attribute() == EntityAttributes.GENERIC_ATTACK_SPEED) {
                value = (PLAYER_BASE_ATTACK_SPEED + value) * attackSpeedMultiplier - PLAYER_BASE_ATTACK_SPEED;
            }
            builder.add(entry.attribute(),
                    new EntityAttributeModifier(modifier.id(), value, modifier.operation()),
                    entry.slot());
        }
        return builder.build().withShowInTooltip(full.showInTooltip());
    }
}
