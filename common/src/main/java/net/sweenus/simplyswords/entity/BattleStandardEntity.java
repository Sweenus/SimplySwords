package net.sweenus.simplyswords.entity;

import com.google.common.base.Supplier;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class BattleStandardEntity extends PathAwareEntity {
    public static final Supplier<EntityType<BattleStandardEntity>> TYPE = Suppliers.memoize(() -> EntityType.Builder.create(BattleStandardEntity::new, SpawnGroup.MISC).build("battlestandard"));
    int abilityDamage =  (int) (SimplySwordsConfig.getFloatValue("abyssalstandard_damage"));

    public static DefaultAttributeContainer.Builder createBattleStandardAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 150.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0f)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 100.0f);
    }

    public BattleStandardEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void baseTick() {

        if (!this.world.isClient()) {
            int radius = 6;
            int abilityDamage = 2;
            //AOE Aura
            if (this.age % 10 == 0) {
                Box box = new Box(this.getX() + radius, this.getY() + (float) radius / 3, this.getZ() + radius, this.getX() - radius, this.getY() - (float) radius / 3, this.getZ() - radius);
                this.setHealth(this.getHealth()-3);
                for (Entity entities : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if (entities != null) {
                        if ((entities instanceof LivingEntity le)) {
                            if (!(le instanceof PlayerEntity)) {
                                le.damage(DamageSource.MAGIC, abilityDamage);
                                le.setOnFireFor(1);
                                le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), this);
                            }
                        }
                    }
                }
                HelperMethods.spawnParticle(world, ParticleTypes.LAVA, this.getX(),
                        this.getY(),
                        this.getZ(),
                        0, 0, 0);
            }


            if (this.age % 80 == 0) {

                //AOE Heal
                Box box = new Box(this.getX() + radius, this.getY() + (float) radius / 3, this.getZ() + radius, this.getX() - radius, this.getY() - (float) radius / 3, this.getZ() - radius);
                this.setHealth(this.getHealth()-3);
                for (Entity entities : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if (entities != null) {
                        if ((entities instanceof PlayerEntity pe)) {
                            pe.heal(3);
                            pe.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 90, 1), this);
                        }
                    }
                }

                world.playSoundFromEntity(null, this, SoundRegistry.ELEMENTAL_BOW_EARTH_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.1f, 0.6f);
                double xpos = this.getX() - (radius + 1);
                double ypos = this.getY();
                double zpos = this.getZ() - (radius + 1);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                            float choose = (float) (Math.random() * 1);
                            HelperMethods.spawnParticle(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, xpos + i + choose,
                                    ypos + 0.1,
                                    zpos + j + choose,
                                    0, -0.1, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, xpos + i + choose,
                                    ypos + 0.1,
                                    zpos + j + choose,
                                    0, -0.1, 0);
                        }
                    }
            }
        }



        super.baseTick();
    }


}
