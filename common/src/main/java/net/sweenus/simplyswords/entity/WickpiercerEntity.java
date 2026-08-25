package net.sweenus.simplyswords.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.api.ability.Phase2AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase2UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class WickpiercerEntity extends ThrownSpearEntity {
    public int slownessDuration;
    private @Nullable UniqueAbilityExecution abilityExecution;
    private boolean impactBurstUsed;

    // Base Constructor
    public WickpiercerEntity(EntityType<? extends WickpiercerEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public WickpiercerEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }
    @Override
    public void tick() {
        returnToPlayer = true;
        super.tick();
        if (isRemoved()) finishExecution(0);
    }

    public void setAbilityExecution(UniqueAbilityExecution execution) {
        this.abilityExecution = execution;
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target, float damage) {
        if (abilityExecution == null || !(getWorld() instanceof ServerWorld)) return;
        Phase2AbilityTuning tuning = Phase2UniqueAbilities.tuning(abilityExecution);
        int fireTicks = tuning.integer(Phase2AbilityTuning.Setting.FIRE_TICKS, 0);
        if (fireTicks > 0) target.setOnFireFor(Math.max(1, (fireTicks + 19) / 20));
        UniqueAbilityApi.emit(abilityExecution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                Phase2UniqueAbilities.HIT, target, 1, damage);
        burst(target, damage, tuning);
    }

    @Override
    protected int getAdditionalPierces() {
        return abilityExecution == null ? 0 : Phase2UniqueAbilities.tuning(abilityExecution)
                .integer(Phase2AbilityTuning.Setting.PIERCE_COUNT, 0);
    }

    @Override
    protected float getPierceDamageMultiplier() {
        return abilityExecution == null ? 1.0F : (float) Phase2UniqueAbilities.tuning(abilityExecution)
                .get(Phase2AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1);
    }

    private void burst(LivingEntity directTarget, float damage, Phase2AbilityTuning tuning) {
        double radius = tuning.get(Phase2AbilityTuning.Setting.IMPACT_RADIUS, 0);
        int cap = tuning.integer(Phase2AbilityTuning.Setting.IMPACT_TARGET_CAP, 0);
        float burstDamage = damage * (float) tuning.get(Phase2AbilityTuning.Setting.IMPACT_DAMAGE_MULTIPLIER, 0);
        if (impactBurstUsed || radius <= 0 || cap <= 0 || burstDamage <= 0
                || !(getWorld() instanceof ServerWorld world) || !(getOwner() instanceof LivingEntity owner)) return;
        impactBurstUsed = true;
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                new Box(directTarget.getPos(), directTarget.getPos()).expand(radius),
                candidate -> candidate != directTarget && candidate != owner && candidate.isAlive()
                        && candidate.squaredDistanceTo(directTarget) <= radius * radius
                        && HelperMethods.checkAbilityTarget(candidate, owner));
        targets.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(directTarget)));
        for (int i = 0; i < Math.min(cap, targets.size()); i++) {
            SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, targets.get(i),
                    burstDamage, SpellScalingProfile.FIRE);
        }
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        boolean pickedUp = super.tryPickup(player);
        if (pickedUp) finishExecution(1);
        return pickedUp;
    }

    private void finishExecution(int affected) {
        if (abilityExecution != null && !abilityExecution.isTerminal()) {
            UniqueAbilityApi.finish(abilityExecution, abilityExecution.definition().id(), affected);
        }
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.WICKPIERCER.get());
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
