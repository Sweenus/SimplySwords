package net.sweenus.simplyswords.entity.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

public class AttackHostileMobsGoal extends Goal {
    private final PathAwareEntity entity;
    private final TargetPredicate hostileMobTargetPredicate;
    private LivingEntity targetMob;
    private PlayerEntity targetPlayer;
    private final double attackRange;
    private final double playerCheckRange;
    private final double speed;

    public AttackHostileMobsGoal(PathAwareEntity entity, double attackRange, double speed, double playerCheckRange) {
        this.entity = entity;
        this.attackRange = attackRange;
        this.speed = speed;
        this.playerCheckRange = playerCheckRange;
        this.hostileMobTargetPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(attackRange);
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return false;

        // Find the nearest player within the playerCheckRange
        targetPlayer = serverWorld.getClosestPlayer(entity.getX(), entity.getY(), entity.getZ(), playerCheckRange, false);
        if (targetPlayer == null) return false;

        // Look for the nearest hostile mob within the attack range
        targetMob = serverWorld.getClosestEntity(HostileEntity.class, hostileMobTargetPredicate, entity, entity.getX(), entity.getY(), entity.getZ(), entity.getBoundingBox().expand(attackRange));
        return targetMob != null;
    }

    @Override
    public boolean shouldContinue() {
        // Continue attacking if the hostile mob is valid and the player is within range
        return targetMob != null
                && targetMob.isAlive()
                && targetPlayer != null
                && entity.squaredDistanceTo(targetPlayer) <= (playerCheckRange * playerCheckRange);
    }

    @Override
    public void stop() {
        targetMob = null;
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
            if (entity.squaredDistanceTo(targetMob) <= 2.0 * 2.0 && targetMob.timeUntilRegen < 10) {
                entity.tryAttack(targetMob);
            }
        }
    }
}



