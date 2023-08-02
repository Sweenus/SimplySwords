package net.sweenus.simplyswords.entity;

import com.google.common.base.Suppliers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Objects;
import java.util.function.Supplier;

public class BattleStandardEntity extends PathAwareEntity {
    public static final Supplier<EntityType<BattleStandardEntity>> TYPE = Suppliers.memoize(() ->
            EntityType.Builder.create(BattleStandardEntity::new, SpawnGroup.MISC).build("battlestandard"));
    float abilityDamage = (SimplySwords.uniqueEffectsConfig.righteousStandardDamage);
    float abilityHeal = 3;
    float abilityHealScalingModifier = (SimplySwords.uniqueEffectsConfig.righteousStandardSpellScalingHeal);
    float spellScalingModifier = (SimplySwords.uniqueEffectsConfig.righteousStandardSpellScaling);
    public LivingEntity ownerEntity;
    public String standardType;
    public int decayRate;
    public String positiveEffect;
    public String positiveEffectSecondary;
    public int positiveEffectAmplifier;
    public String negativeEffect;
    public String negativeEffectSecondary;
    public int negativeEffectAmplifier;
    public boolean dealsDamage = true;
    public boolean doesHealing = true;
    private static boolean errorLogged = false;

    public static DefaultAttributeContainer.Builder createBattleStandardAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 150.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0f)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 100.0f);
    }

    public BattleStandardEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    private static void errorCatch(String identifier) {
        if (!errorLogged) {
            System.out.println("ERROR: Identifier " + identifier + " does not match any registered effects.\nDestroying banner entity now.");
            errorLogged = true;
        }
    }

    @Override
    protected boolean isImmobile() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return ownerEntity == null;
    }

    @Override
    public void baseTick() {
        if (!this.getWorld().isClient()) {
            if (this.age % 10 == 0) {
                this.setHealth(this.getHealth() - decayRate);
                if (ownerEntity == null) this.setHealth(this.getHealth() - 1000);
            }
            if (ownerEntity != null && standardType != null) {
                int radius = 6;
                if (HelperMethods.commonSpellAttributeScaling(spellScalingModifier, ownerEntity, "fire") > 0) {
                    abilityDamage = HelperMethods.commonSpellAttributeScaling(spellScalingModifier, ownerEntity, "fire");
                }
                //AOE Aura
                if (this.age % 10 == 0) {
                    Box box = new Box(this.getX() + radius, this.getY() + (float) radius / 3, this.getZ() + radius,
                            this.getX() - radius, this.getY() - (float) radius / 3, this.getZ() - radius);
                    for (Entity entity : getWorld().getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, ownerEntity)
                                && le != ownerEntity && !(le instanceof BattleStandardEntity)
                                && !(le instanceof BattleStandardDarkEntity)) {

                            //Sunfire negative effects
                            switch (standardType) {
                                case "sunfire" -> {
                                    le.damage(ownerEntity.getDamageSources().magic(), abilityDamage);
                                    le.setOnFireFor(1);
                                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), this);
                                }
                                //Nullification negative effects
                                case "nullification" -> {
                                    for (StatusEffectInstance statusEffect : le.getStatusEffects()) {
                                        if (statusEffect != null && statusEffect.getEffectType().isBeneficial()) {
                                            le.removeStatusEffect(statusEffect.getEffectType());
                                            break;
                                        }
                                    }
                                }
                                // API negative effects
                                case "api" -> {
                                    if (dealsDamage)
                                        le.damage(ownerEntity.getDamageSources().magic(), abilityDamage);
                                    if (negativeEffect != null) {
                                        try {
                                            le.addStatusEffect(new StatusEffectInstance(
                                                    Registries.STATUS_EFFECT.get(new Identifier(negativeEffect)),
                                                    20, negativeEffectAmplifier), this);
                                        } catch (Exception e) {
                                            errorCatch(negativeEffect);
                                            this.setHealth(this.getHealth() - 1000);
                                        }
                                    }
                                    if (negativeEffectSecondary != null) {
                                        try {
                                            le.addStatusEffect(new StatusEffectInstance(
                                                    Registries.STATUS_EFFECT.get(new Identifier(negativeEffectSecondary)),
                                                    20, negativeEffectAmplifier), this);
                                        } catch (Exception e) {
                                            errorCatch(negativeEffectSecondary);
                                            this.setHealth(this.getHealth() - 1000);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HelperMethods.spawnParticle(getWorld(), ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(),
                            0, 0, 0);
                }

                //Landing effects
                if (this.getHealth() > this.getMaxHealth() - 2 && this.isOnGround()) {
                    HelperMethods.spawnParticle(getWorld(), ParticleTypes.LAVA,
                            this.getX(), this.getY(), this.getZ(),
                            0, 0.3, 0);
                    HelperMethods.spawnParticle(getWorld(), ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            this.getX(), this.getY(), this.getZ(),
                            0, 0.1, 0);

                    //Launch nearby entities on land
                    Box box = new Box(this.getX() + 1, this.getY() + 1, this.getZ() + 1,
                            this.getX() - 1, this.getY() - (float) 1, this.getZ() - 1);
                    for (Entity entity : getWorld().getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, ownerEntity) && le != ownerEntity) {
                            le.damage(ownerEntity.getDamageSources().magic(), abilityDamage * 3);
                            le.setOnFireFor(1);
                            le.setVelocity((le.getX() - this.getX()) / 4, 0.5, (le.getZ() - this.getZ()) / 4);
                        }
                    }
                }
                if (this.age % 80 == 0) {
                    Box box = new Box(this.getX() + radius, this.getY() + (float) radius / 3, this.getZ() + radius,
                            this.getX() - radius, this.getY() - (float) radius / 3, this.getZ() - radius);
                    for (Entity entities : getWorld().getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if (entities instanceof LivingEntity le && !HelperMethods.checkFriendlyFire(le, ownerEntity)) {
                            if (HelperMethods.commonSpellAttributeScaling(abilityHealScalingModifier, ownerEntity, "healing") > 0) {
                                abilityHeal = HelperMethods.commonSpellAttributeScaling(abilityHealScalingModifier, ownerEntity, "healing");
                            }
                            //Sunfire positive effects
                            switch (standardType) {
                                case "sunfire" -> {
                                    le.heal(abilityHeal);
                                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 90, 1), this);
                                }
                                //Nullification positive effects
                                case "nullification" -> {
                                    for (StatusEffectInstance statusEffect : le.getStatusEffects()) {
                                        if (statusEffect != null && !statusEffect.getEffectType().isBeneficial()
                                                && !Objects.equals(statusEffect.getEffectType(), EffectRegistry.BATTLE_FATIGUE.get())) {
                                            le.removeStatusEffect(statusEffect.getEffectType());
                                            break;
                                        }
                                    }
                                }
                                // API positive effects
                                case "api" -> {
                                    if (doesHealing)
                                        le.heal(abilityHeal);
                                    if (positiveEffect != null) {
                                        try {
                                            le.addStatusEffect(new StatusEffectInstance(
                                                    Registries.STATUS_EFFECT.get(new Identifier(positiveEffect)),
                                                    20, positiveEffectAmplifier), this);
                                        } catch (Exception e) {
                                            errorCatch(positiveEffect);
                                            this.setHealth(this.getHealth() - 1000);
                                        }
                                    }
                                    if (positiveEffectSecondary != null) {
                                        try {
                                            le.addStatusEffect(new StatusEffectInstance(
                                                    Registries.STATUS_EFFECT.get(new Identifier(positiveEffectSecondary)),
                                                    20, positiveEffectAmplifier), this);
                                        } catch (Exception e) {
                                            errorCatch(positiveEffectSecondary);
                                            this.setHealth(this.getHealth() - 1000);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    getWorld().playSoundFromEntity(null, this, SoundRegistry.ELEMENTAL_BOW_EARTH_SHOOT_IMPACT_02.get(),
                            this.getSoundCategory(), 0.1f, 0.6f);
                    double xpos = this.getX() - (radius + 1);
                    double ypos = this.getY();
                    double zpos = this.getZ() - (radius + 1);

                    for (int i = radius * 2; i > 0; i--) {
                        for (int j = radius * 2; j > 0; j--) {
                            float choose = (float) (Math.random() * 1);
                            if (choose > 0.5) {
                                HelperMethods.spawnParticle(getWorld(), ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                        xpos + i + choose, ypos, zpos + j + choose,
                                        0, -0.1, 0);
                            }
                        }
                    }
                }
            }
        }
        super.baseTick();
    }
}
