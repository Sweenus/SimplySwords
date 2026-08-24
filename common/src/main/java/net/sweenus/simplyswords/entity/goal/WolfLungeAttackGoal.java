package net.sweenus.simplyswords.entity.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class WolfLungeAttackGoal extends Goal {

    private static final int LUNGE_DURATION = 12;
    private static final int REPOSITION_DURATION = 14;
    private static final int ATTACK_COOLDOWN = 10;
    private static final int LUNGE_COOLDOWN = 26;
    private static final double ATTACK_RANGE = 2.6;
    private static final double REPOSITION_RADIUS = 10.0;
    private static final double LEAP_SPEED = 2.0;
    private static final double LEAP_UP = 0.4;

    private final PathAwareEntity entity;
    private final double approachSpeed;
    private final double triggerDistance;

    private int phase = 0;
    private int phaseTimer = 0;
    private int lungeCooldown = 0;
    private int attackCooldown = 0;
    private Vec3d repositionTarget = null;

    public WolfLungeAttackGoal(PathAwareEntity entity, double approachSpeed, double triggerDistance) {
        this.entity = entity;
        this.approachSpeed = approachSpeed;
        this.triggerDistance = triggerDistance;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.entity.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = this.entity.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.phase = 0;
        this.phaseTimer = 0;
        this.lungeCooldown = 0;
        this.attackCooldown = 0;
    }

    @Override
    public void stop() {
        this.entity.getNavigation().stop();
        this.repositionTarget = null;
    }

    @Override
    public void tick() {
        LivingEntity target = this.entity.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.lungeCooldown > 0) {
            this.lungeCooldown--;
        }

        this.entity.getLookControl().lookAt(target, 30.0F, 30.0F);

        if (this.phase == 0) {
            double distance = this.entity.squaredDistanceTo(target);
            if (distance > this.triggerDistance * this.triggerDistance) {
                this.entity.getNavigation().startMovingTo(target, this.approachSpeed);
            } else {
                this.entity.getNavigation().stop();
                if (this.lungeCooldown <= 0) {
                    this.phase = 1;
                    this.phaseTimer = LUNGE_DURATION;
                    Vec3d dir = new Vec3d(target.getX() - this.entity.getX(), 0.0, target.getZ() - this.entity.getZ());
                    if (dir.lengthSquared() > 1.0E-4) {
                        dir = dir.normalize();
                    } else {
                        dir = Vec3d.fromPolar(0.0F, this.entity.getYaw());
                    }
                    float leapYaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
                    this.entity.setYaw(leapYaw);
                    this.entity.setBodyYaw(leapYaw);
                    this.entity.prevYaw = leapYaw;
                    this.entity.getNavigation().stop();
                    this.entity.setVelocity(dir.x * LEAP_SPEED, LEAP_UP, dir.z * LEAP_SPEED);
                    this.entity.velocityModified = true;
                    this.entity.swingHand(Hand.MAIN_HAND);
                    if (this.entity.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ParticleTypes.CLOUD, this.entity.getX(), this.entity.getBodyY(0.5), this.entity.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }
        } else if (this.phase == 1) {
            double distance = this.entity.squaredDistanceTo(target);
            if (distance <= ATTACK_RANGE * ATTACK_RANGE && this.attackCooldown <= 0) {
                this.entity.tryAttack(target);
                this.attackCooldown = ATTACK_COOLDOWN;
            }

            this.phaseTimer--;
            if (this.phaseTimer <= 0) {
                this.phase = 2;
                this.phaseTimer = REPOSITION_DURATION;
                this.lungeCooldown = LUNGE_COOLDOWN;
                double angle = this.entity.getRandom().nextBetween(-2, 2) * Math.PI / 2.0;
                double offsetX = Math.cos(angle) * REPOSITION_RADIUS;
                double offsetZ = Math.sin(angle) * REPOSITION_RADIUS;
                this.repositionTarget = new Vec3d(target.getX() + offsetX, target.getY(), target.getZ() + offsetZ);
            }
        } else {
            if (this.repositionTarget != null) {
                this.entity.getNavigation().startMovingTo(this.repositionTarget.x, this.repositionTarget.y, this.repositionTarget.z, this.approachSpeed);
            }
            this.phaseTimer--;
            if (this.phaseTimer <= 0) {
                this.phase = 0;
                this.repositionTarget = null;
            }
        }
    }
}
