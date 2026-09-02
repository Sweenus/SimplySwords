package net.sweenus.simplyswords.entity.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.EnumSet;

public class AttackHostileMobsGoal extends Goal {
    private final PathAwareEntity entity;
    private LivingEntity targetMob;
    private LivingEntity owner;
    private final double attackRange;
    private final double playerCheckRange;
    private final double speed;

    public AttackHostileMobsGoal(PathAwareEntity entity, double attackRange, double speed, double playerCheckRange) {
        this.entity = entity;
        this.attackRange = attackRange;
        this.speed = speed;
        this.playerCheckRange = playerCheckRange;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return false;

        owner = entity instanceof SimplySwordsAxolotlEntity axolotl ? axolotl.getOwner() : null;
        if (owner == null || !owner.isAlive() || entity.squaredDistanceTo(owner) > playerCheckRange * playerCheckRange) return false;

        if (owner instanceof net.minecraft.entity.mob.MobEntity mobOwner
                && mobOwner.getTarget() != null
                && mobOwner.getTarget().isAlive()
                && HelperMethods.checkAbilityTarget(mobOwner.getTarget(), owner)) {
            targetMob = mobOwner.getTarget();
            return true;
        }

        double range = entity instanceof SimplySwordsAxolotlEntity axolotl
                ? axolotl.getMasteryTargetRange(attackRange) : attackRange;
        int cap = entity instanceof SimplySwordsAxolotlEntity axolotl
                ? axolotl.getMasteryTargetSearchCap() : 0;
        TargetPredicate predicate = TargetPredicate.createAttackable().setBaseMaxDistance(range);
        var targets = serverWorld.getEntitiesByClass(HostileEntity.class, entity.getBoundingBox().expand(range),
                        hostile -> predicate.test(entity, hostile)
                                && HelperMethods.checkAbilityTarget(hostile, owner))
                .stream();
        if (cap > 0) targets = targets.limit(cap);
        targetMob = targets
                .min((first, second) -> Double.compare(first.squaredDistanceTo(entity), second.squaredDistanceTo(entity)))
                .orElse(null);
        return targetMob != null;
    }

    @Override
    public boolean shouldContinue() {
        return targetMob != null
                && targetMob.isAlive()
                && owner != null
                && owner.isAlive()
                && entity.squaredDistanceTo(owner) <= (playerCheckRange * playerCheckRange)
                && HelperMethods.checkAbilityTarget(targetMob, owner);
    }

    @Override
    public void stop() {
        targetMob = null;
        owner = null;
        entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetMob != null) {
            // Navigate to the hostile mob
            entity.getNavigation().startMovingTo(targetMob.getX(), targetMob.getY(), targetMob.getZ(), speed);

            // Look at the hostile mob
            entity.getLookControl().lookAt(targetMob, 30.0F, 30.0F);

            // Attack the hostile mob
            if (entity.squaredDistanceTo(targetMob) <= 2.0 * 2.0 && targetMob.timeUntilRegen < 10
                    && owner != null && HelperMethods.checkAbilityTarget(targetMob, owner)) {
                entity.tryAttack(targetMob);
            }
        }
    }
}
