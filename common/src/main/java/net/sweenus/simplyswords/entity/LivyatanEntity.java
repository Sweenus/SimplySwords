package net.sweenus.simplyswords.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class LivyatanEntity extends ThrownSwordEntity {
    public int slownessDuration;

    // Base Constructor
    public LivyatanEntity(EntityType<? extends LivyatanEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public LivyatanEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }
    @Override
    public void tick() {
        returnToPlayer = true;
        super.tick();
    }

    @Override
    protected void damageOnReturn(double radius, float damage) {
        if (getWorld().isClient()) return;
        if (this.stack == null || this.stack.isEmpty()) return;

        ServerWorld world = (ServerWorld) this.getWorld();
        if (this.getOwner() != null && this.getOwner() instanceof ServerPlayerEntity user) {
            DamageSource damageSource = user.getDamageSources().trident(this, user);
            float returnDamage = EnchantmentHelper.getDamage(world, stack, this, damageSource, damage);

            Box box = new Box(this.getX() + radius, this.getY() + radius, this.getZ() + radius,
                    this.getX() - radius, this.getY() - radius, this.getZ() - radius);

            for (Entity entity : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user) && le.age %5 == 0) {

                    HelperMethods.damageThroughIframes(le, damageSource, returnDamage);
                    world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_ICE_ATTACK_01.get(),
                            user.getSoundCategory(), 0.2f, 1.5f);
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slownessDuration, 2), user);
                    HelperMethods.spawnOrbitParticles(world, this.getPos(), ParticleTypes.POOF, 0.5f, 3);
                }
            }
        }
    }

    @Override
    protected void doEffects(ServerWorld serverWorld, float baseDamage, Entity entity) {
        int bonusParticles =  ((int) baseDamage / 2);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.POOF, 0.5f, 3+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.CRIT, 0.5f, 5+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.SNOWFLAKE, 0.5f, 2+bonusParticles);
        if (baseDamage > primaryBaseDamage)
            serverWorld.playSoundFromEntity(null, entity, SoundRegistry.MAGIC_SWORD_PARRY_VARIOUS_HITS.get(),
                    this.getSoundCategory(), 0.3f, 1.2f);
    }

    @Override
    protected SoundEvent getReturnSound() {
        return SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_01.get();
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.FROSTFALL.get());
    }

    @Override
    protected byte getLoyalty() {
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            return 3;
        } else {
            return 0;
        }
    }

}
