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
        if (!(entity instanceof SimplySwordsAxolotlEntity axolotl) || !axolotl.canMasteryAttack()
                || axolotl.hasPassengers() || !(entity.getWorld() instanceof ServerWorld world)) return false;
        owner = axolotl.getOwner();
        if (owner == null || !owner.isAlive() || entity.squaredDistanceTo(owner) > playerCheckRange * playerCheckRange) return false;
        return selectTarget(world, axolotl) != null;
    }

    private boolean valid(LivingEntity target, double range) {
        return target != null && target.isAlive() && !target.isRemoved() && target != owner && target != entity
                && target.getWorld() == entity.getWorld() && entity.squaredDistanceTo(target) <= range * range
                && HelperMethods.checkAbilityTarget(target, owner);
    }

    private LivingEntity selectTarget(ServerWorld world, SimplySwordsAxolotlEntity axolotl) {
        double range = axolotl.getMasteryTargetRange(attackRange);
        LivingEntity preferred = axolotl.hasCoordinatedBite()
                ? net.sweenus.simplyswords.world.ChompolotlMasteryManager.currentTarget(owner) : null;
        if (valid(preferred, range)) axolotl.setTarget(preferred);
        else if (!valid(axolotl.getTarget(), range)) {
            axolotl.setTarget(world.getEntitiesByClass(HostileEntity.class, entity.getBoundingBox().expand(range),
                    target -> valid(target, range)).stream()
                    .sorted(java.util.Comparator.comparingDouble((HostileEntity target) -> entity.squaredDistanceTo(target))
                            .thenComparing(target -> target.getUuid().toString()))
                    .limit(Math.max(1, axolotl.getMasteryTargetSearchCap())).findFirst().orElse(null));
        }
        return axolotl.getTarget();
    }

    @Override
    public boolean shouldContinue() { return canStart(); }

    @Override
    public void stop() {
        owner = null;
        entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(entity instanceof SimplySwordsAxolotlEntity axolotl) || axolotl.hasPassengers()
                || !(entity.getWorld() instanceof ServerWorld world) || owner == null) return;
        LivingEntity target = selectTarget(world, axolotl);
        if (target == null) return;
        entity.getNavigation().startMovingTo(target, speed);
        entity.getLookControl().lookAt(target, 30, 30);
        if (entity.squaredDistanceTo(target) <= 4) entity.tryAttack(target);
    }
}
