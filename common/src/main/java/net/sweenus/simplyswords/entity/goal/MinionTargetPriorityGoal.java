package net.sweenus.simplyswords.entity.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.util.MinionTargeting;

import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Predicate;

public class MinionTargetPriorityGoal extends Goal {

    private final MobEntity mob;
    private final Function<ServerWorld, ServerPlayerEntity> ownerFunction;
    private final Predicate<LivingEntity> validTarget;
    private final double searchRadius;
    private final int intervalTicks;
    private int cooldown;
    private LivingEntity lastGoalTarget;

    public MinionTargetPriorityGoal(MobEntity mob, Function<ServerWorld, ServerPlayerEntity> ownerFunction,
                                     Predicate<LivingEntity> validTarget, double searchRadius, int intervalTicks) {
        this.mob = mob;
        this.ownerFunction = ownerFunction;
        this.validTarget = validTarget;
        this.searchRadius = searchRadius;
        this.intervalTicks = Math.max(1, intervalTicks);
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return this.mob.isAlive();
    }

    @Override
    public void tick() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        this.cooldown = this.intervalTicks;

        if (!(this.mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        ServerPlayerEntity owner = this.ownerFunction.apply(world);
        if (owner == null) {
            return;
        }

        LivingEntity current = this.mob.getTarget();
        boolean currentValid = current != null && current.isAlive() && this.validTarget.test(current);

        if (!currentValid) {
            this.lastGoalTarget = null;
            LivingEntity recent = MinionTargeting.getRecentAttackTarget(world, owner);
            if (recent != null && this.validTarget.test(recent)) {
                set(recent);
                return;
            }
            LivingEntity nearest = MinionTargeting.findNearestValidToOwner(world, owner, this.validTarget, this.searchRadius);
            if (nearest != null) {
                set(nearest);
            }
            return;
        }

        if (current != this.lastGoalTarget) {
            this.lastGoalTarget = null;
            return;
        }

        LivingEntity recent = MinionTargeting.getRecentAttackTarget(world, owner);
        if (recent != null && recent != current && this.validTarget.test(recent)) {
            set(recent);
        }
    }

    private void set(LivingEntity target) {
        this.mob.setTarget(target);
        this.lastGoalTarget = target;
    }
}
