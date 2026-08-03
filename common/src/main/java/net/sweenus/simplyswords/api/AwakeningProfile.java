package net.sweenus.simplyswords.api;

//
// Public awakening parameters for addon unique weapons.
//
// The Runic Forge has eight physical tablet slots, so profiles intentionally
// share the same maximum. The other values may be adjusted by addons.
//
public record AwakeningProfile(
        float dormantAttributeMultiplier,
        float dormantAttackSpeedMultiplier,
        int abilityUnlockLevel
) {
    public static final float DEFAULT_DORMANT_ATTRIBUTE_MULTIPLIER = 0.50F;
    public static final float DEFAULT_DORMANT_ATTACK_SPEED_MULTIPLIER = 0.75F;
    public static final AwakeningProfile DEFAULT = new AwakeningProfile(
            DEFAULT_DORMANT_ATTRIBUTE_MULTIPLIER,
            DEFAULT_DORMANT_ATTACK_SPEED_MULTIPLIER,
            4
    );

    public AwakeningProfile {
        dormantAttributeMultiplier = net.minecraft.util.math.MathHelper.clamp(dormantAttributeMultiplier, 0.0F, 1.0F);
        dormantAttackSpeedMultiplier = net.minecraft.util.math.MathHelper.clamp(dormantAttackSpeedMultiplier, 0.0F, 1.0F);
        abilityUnlockLevel = net.minecraft.util.math.MathHelper.clamp(abilityUnlockLevel, 0,
                net.sweenus.simplyswords.item.component.AwakeningComponent.MAX_LEVEL);
    }

    //
    // Preserves the original addon-facing constructor while applying the
    // standard, gentler attack-speed awakening curve.
    //
    public AwakeningProfile(float dormantAttributeMultiplier, int abilityUnlockLevel) {
        this(dormantAttributeMultiplier, DEFAULT_DORMANT_ATTACK_SPEED_MULTIPLIER, abilityUnlockLevel);
    }
}
