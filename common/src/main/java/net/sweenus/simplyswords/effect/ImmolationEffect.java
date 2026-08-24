package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class ImmolationEffect extends WideOrbitingEffect {
    private static final int SCALE_PRECISION = 1000;
    private static final int SPELL_PRECISION = 100;

    public ImmolationEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        particleType1 = ParticleTypes.CRIT;
        particleType2 = ParticleTypes.ENCHANT;
        yOffset = 15f;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.getWorld().isClient()) {
            if (pLivingEntity instanceof PlayerEntity player) {
                if (pLivingEntity.age % 15 == 0) {

                    player.getWorld().playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_FLYBY_03.get(),
                            SoundCategory.PLAYERS, 0.1f, 1.0f);
                    if (!Config.general.enableModernFieldEffects) {
                        HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.LAVA, player.getX(), player.getY()+0.5, player.getZ(), 0.3, 0.8, 0.2);
                        HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.LAVA, player.getX(), player.getY()+0.5, player.getZ(), -0.2, 0.6, 0.3);
                        HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.LAVA, player.getX(), player.getY()+0.5, player.getZ(), 0.5, 0.3, -0.2);
                        HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.SMOKE, player.getX(), player.getY()+0.5, player.getZ(), 0, 0, 0);
                    }

                    ItemStack checkMainStack = player.getMainHandStack();
                    ItemStack checkOffStack = player.getOffHandStack();

                    if (!(checkMainStack.getItem() instanceof SwordItem || checkOffStack.getItem() instanceof SwordItem)) {
                        player.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.IMMOLATION));
                    }

                    float gemMultiplier = 1.0F;
                    float spellDamage = 0.0F;
                    StatusEffectInstance active = player.getStatusEffect(EffectRegistry.getReference(EffectRegistry.IMMOLATION));
                    if (active instanceof SimplySwordsStatusEffectInstance scaled) {
                        int packed = scaled.getAdditionalData();
                        gemMultiplier = ((packed >>> 16) & 0xFFFF) / (float) SCALE_PRECISION;
                        spellDamage = (packed & 0xFFFF) / (float) SPELL_PRECISION;
                    }
                    float abilityDamage = Math.max(player.getHealth() / 3.0F * gemMultiplier, spellDamage);

                    //Damage
                    Box box = HelperMethods.createBox(pLivingEntity, pAmplifier);
                    for (Entity entities : player.getWorld().getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {
                                le.timeUntilRegen = 0;
                                le.damage(player.getDamageSources().indirectMagic(player, player), abilityDamage);
                                le.setOnFireFor(1);
                                le.timeUntilRegen = 0;
                            }
                        }
                    }
                }
            }
        }

        if (!Config.general.enableModernFieldEffects) {
            super.applyUpdateEffect(pLivingEntity, pAmplifier);
        }
        return true;
    }

    @Override
    public void onRemoved(AttributeContainer attributes) {
        super.onRemoved(attributes);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }

    public static SimplySwordsStatusEffectInstance createScaledInstance(
            LivingEntity owner, ItemStack stack, int duration, int amplifier, float spellScaling,
            Identifier scalingComponentId) {
        SimplySwordsStatusEffectInstance instance = new SimplySwordsStatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.IMMOLATION), duration, amplifier,
                false, true, true);
        instance.setSourceEntity(owner);
        float multiplier = AwakeningApi.getGemPowerMultiplier(stack);
        float spellDamage = HelperMethods.commonSpellAttributeScaling(
                spellScaling, owner, scalingComponentId) * multiplier;
        int packedMultiplier = Math.clamp(Math.round(multiplier * SCALE_PRECISION), 0, 0xFFFF);
        int packedSpell = Math.clamp(Math.round(spellDamage * SPELL_PRECISION), 0, 0xFFFF);
        instance.setAdditionalData((packedMultiplier << 16) | packedSpell);
        return instance;
    }
}
