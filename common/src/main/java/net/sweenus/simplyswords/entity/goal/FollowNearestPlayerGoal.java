package net.sweenus.simplyswords.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;

import java.util.EnumSet;

public class FollowNearestPlayerGoal extends Goal {
    private final SimplySwordsAxolotlEntity axolotl;
    private LivingEntity owner;
    private final double followSpeed;
    private final double searchRadius;
    private final double stopDistance;

    public FollowNearestPlayerGoal(SimplySwordsAxolotlEntity axolotl, double followSpeed, double searchRadius, double stopDistance) {
        this.axolotl = axolotl;
        this.followSpeed = followSpeed;
        this.searchRadius = searchRadius;
        this.stopDistance = stopDistance;

        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!axolotl.hasPassengers() && axolotl.getWorld() instanceof ServerWorld) {
            LivingEntity owner = axolotl.getOwner();
            if (owner != null && owner.isAlive() && axolotl.squaredDistanceTo(owner) <= searchRadius * searchRadius) {
                this.owner = owner;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return !axolotl.hasPassengers() && this.owner != null
                && this.owner.isAlive()
                && this.axolotl.squaredDistanceTo(this.owner) > (stopDistance * stopDistance);
    }

    @Override
    public void stop() {
        // Clear the target
        this.owner = null;
        axolotl.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.owner != null) {
            axolotl.getNavigation().startMovingTo(
                    this.owner.getX(),
                    this.owner.getY(),
                    this.owner.getZ(),
                    this.followSpeed
            );

            Vec3d targetPosition = this.owner.getPos();
            this.axolotl.getLookControl().lookAt(targetPosition.x, targetPosition.y, targetPosition.z);
        }
    }
}
