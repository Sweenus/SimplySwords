package net.sweenus.simplyswords.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;

import java.util.EnumSet;

public class FollowNearestPlayerGoal extends Goal {
    private final SimplySwordsAxolotlEntity axolotl;
    private PlayerEntity targetPlayer;
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
        if (axolotl.getEntityWorld() instanceof ServerWorld) {
            // Find the closest player within the search radius
            PlayerEntity nearestPlayer = axolotl.getEntityWorld()
                    .getClosestPlayer(axolotl.getX(), axolotl.getY(), axolotl.getZ(), searchRadius, false);

            // Set it as the current target
            if (nearestPlayer != null) {
                this.targetPlayer = nearestPlayer;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        // Continue following while the target player
        return this.targetPlayer != null
                && this.targetPlayer.isAlive()
                && this.axolotl.squaredDistanceTo(this.targetPlayer) > (stopDistance * stopDistance);
    }

    @Override
    public void stop() {
        // Clear the target
        this.targetPlayer = null;
        axolotl.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.targetPlayer != null) {
            // Navigate toward the player
            axolotl.getNavigation().startMovingTo(
                    this.targetPlayer.getX(),
                    this.targetPlayer.getY(),
                    this.targetPlayer.getZ(),
                    this.followSpeed
            );

            // Look at the player
            Vec3d targetPosition = this.targetPlayer.getEntityPos();
            this.axolotl.getLookControl().lookAt(targetPosition.x, targetPosition.y, targetPosition.z);
        }
    }
}
