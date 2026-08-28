package net.sweenus.simplyswords.entity;

import com.google.common.base.Suppliers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.Phase4StandardManager;

import java.util.Objects;
import java.util.function.Supplier;

public class BattleStandardEntity extends PathAwareEntity {
    public static final Supplier<EntityType<BattleStandardEntity>> TYPE = Suppliers.memoize(() ->
            EntityType.Builder.create(BattleStandardEntity::new, SpawnGroup.MISC).build("battlestandard"));
    private static final TrackedData<String> TRACKED_STANDARD_TYPE = DataTracker.registerData(BattleStandardEntity.class, TrackedDataHandlerRegistry.STRING);
    public LivingEntity ownerEntity;
    public String standardType;
    public String spellScalingOwner = "sunfire";
    public int decayRate;
    public String positiveEffect;
    public String positiveEffectSecondary;
    public int positiveEffectAmplifier;
    public String negativeEffect;
    public String negativeEffectSecondary;
    public int negativeEffectAmplifier;
    public boolean dealsDamage = true;
    public boolean doesHealing = true;
    private UniqueAbilityExecution phase4Execution;
    private ItemStack phase4Stack = ItemStack.EMPTY;
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

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TRACKED_STANDARD_TYPE, "");
    }

    public String getStandardType() {
        String trackedType = this.dataTracker.get(TRACKED_STANDARD_TYPE);
        return trackedType == null || trackedType.isBlank() ? this.standardType : trackedType;
    }

    public void configurePhase4(UniqueAbilityExecution execution, ItemStack stack) {
        this.phase4Execution = execution;
        this.phase4Stack = stack.copy();
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
    public void remove(RemovalReason reason) {
        if (!this.getWorld().isClient()) Phase4StandardManager.onStandardRemoved(this.getUuid());
        super.remove(reason);
    }

    @Override
    public void baseTick() {
        if (!this.getWorld().isClient()) {
            if (this.standardType != null && !this.standardType.equals(this.dataTracker.get(TRACKED_STANDARD_TYPE))) {
                this.dataTracker.set(TRACKED_STANDARD_TYPE, this.standardType);
            }
            if (this.age % 10 == 0) {
                this.setHealth(this.getHealth() - decayRate);
                if (ownerEntity == null) this.setHealth(this.getHealth() - 1000);
            }
            if (ownerEntity != null && standardType != null) {
                if (standardType.equals("sunfire") && phase4Execution != null
                        && Phase4StandardManager.tickSunfire(this, phase4Execution, phase4Stack)) {
                    super.baseTick();
                    return;
                }
                if (!ownerEntity.isAlive())
                    this.setHealth(this.getHealth() - 1000);
                int radius = 6;
                float abilityDamage = HelperMethods.abilityScaledDamage(SpellScalingComponents.component(spellScalingOwner, "damage"), ownerEntity, ownerEntity.getMainHandStack(),
                        Config.uniqueEffects.sunfire.damageScaling, Config.uniqueEffects.sunfire.spellScaling);
                // AOE Aura
                //living entity, ownerEntity, abilityDamage,
                if (this.age % 10 == 0) {
                    Box box = new Box(this.getX() + radius, this.getY() + (float) radius / 3, this.getZ() + radius,
                            this.getX() - radius, this.getY() - (float) radius / 3, this.getZ() - radius);
                    for (Entity entity : getWorld().getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((entity instanceof LivingEntity le) && HelperMethods.checkAbilityTarget(le, ownerEntity)
                                && le != ownerEntity && !(le instanceof BattleStandardEntity)
                                && !(le instanceof BattleStandardDarkEntity)) {

                            // Sunfire negative effects
                            switch (standardType) {
                                case "sunfire" -> {
                                    DamageSource damageSource = ownerEntity.getDamageSources().magic();
                                    le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments((ServerWorld) getWorld(), ownerEntity.getMainHandStack(), le, damageSource, abilityDamage));
                                    le.setOnFireFor(1);
                                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), this);
                                }
                                // Nullification negative effects
                                case "nullification" -> {
                                    for (StatusEffectInstance statusEffectInstance : le.getStatusEffects()) {
                                        StatusEffect statusEffect = statusEffectInstance.getEffectType().value();
                                        if (statusEffect != null && statusEffect.isBeneficial()) {
                                            le.removeStatusEffect(statusEffectInstance.getEffectType());
                                            break;
                                        }
                                    }
                                }
                                // API negative effects
                                case "api" -> {
                                    if (dealsDamage) {
                                        DamageSource damageSource = ownerEntity.getDamageSources().magic();
                                        le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments((ServerWorld) getWorld(), ownerEntity.getMainHandStack(), le, damageSource, abilityDamage));
                                    }
                                    if (negativeEffect != null) {
                                        try {
                                            RegistryEntry<StatusEffect> negativeEffectEntry = Registries.STATUS_EFFECT.getEntry(Identifier.of(negativeEffect)).orElseThrow();
                                            le.addStatusEffect(new StatusEffectInstance(negativeEffectEntry, 20, negativeEffectAmplifier), this);
                                        } catch (Exception e) {
                                            errorCatch(negativeEffect);
                                            this.setHealth(this.getHealth() - 1000);
                                        }
                                    }
                                    if (negativeEffectSecondary != null) {
                                        try {
                                            RegistryEntry<StatusEffect> negativeEffectSecondaryEntry = Registries.STATUS_EFFECT.getEntry(Identifier.of(negativeEffectSecondary)).orElseThrow();
                                            le.addStatusEffect(new StatusEffectInstance(negativeEffectSecondaryEntry, 20, negativeEffectAmplifier), this);
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
                        if ((entity instanceof LivingEntity le) && HelperMethods.checkAbilityTarget(le, ownerEntity) && le != ownerEntity) {
                            DamageSource damageSource = ownerEntity.getDamageSources().magic();
                            le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments((ServerWorld) getWorld(), ownerEntity.getMainHandStack(), le, damageSource, abilityDamage * 3));
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
                            float abilityHeal = HelperMethods.abilityScaledDamage(SpellScalingComponents.component(spellScalingOwner, "healing"), ownerEntity, ownerEntity.getMainHandStack(),
                                    Config.uniqueEffects.sunfire.healScaling, Config.uniqueEffects.sunfire.spellScalingHeal);
                            //Sunfire positive effects
                            switch (standardType) {
                                case "sunfire" -> {
                                    le.heal(abilityHeal);
                                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 90, 1), this);
                                }
// Nullification positive effects
                                case "nullification" -> {
                                    for (StatusEffectInstance statusEffectInstance : le.getStatusEffects()) {
                                        RegistryEntry<StatusEffect> effectEntry = statusEffectInstance.getEffectType();
                                        StatusEffect effect = effectEntry.value();
                                        if (effect != null && !effect.isBeneficial()
                                                && !Objects.equals(effectEntry, EffectRegistry.getReference(EffectRegistry.BATTLE_FATIGUE))) {
                                            le.removeStatusEffect(effectEntry);
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
                                            RegistryEntry<StatusEffect> positiveEffectEntry = Registries.STATUS_EFFECT.getEntry(Identifier.of(positiveEffect)).orElseThrow();
                                            le.addStatusEffect(new StatusEffectInstance(positiveEffectEntry, 85, positiveEffectAmplifier), this);
                                        } catch (Exception e) {
                                            errorCatch(positiveEffect);
                                            this.setHealth(this.getHealth() - 1000);
                                        }
                                    }
                                    if (positiveEffectSecondary != null) {
                                        try {
                                            RegistryEntry<StatusEffect> positiveEffectSecondaryEntry = Registries.STATUS_EFFECT.getEntry(Identifier.of(positiveEffectSecondary)).orElseThrow();
                                            le.addStatusEffect(new StatusEffectInstance(positiveEffectSecondaryEntry, 85, positiveEffectAmplifier), this);
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
