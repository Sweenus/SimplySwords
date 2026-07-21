package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.DancingBladeVisualEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DancingBladeVisualEntityRenderer extends EntityRenderer<DancingBladeVisualEntity> {

    private static final double BASE_ANGULAR_SPEED = 0.085;
    private static final double BLADE_HEIGHT = 1.15;
    private static final double BOB_AMPLITUDE = 0.12;
    private static final double BOB_SPEED = 0.16;
    private static final float ATTACK_ANIMATION_TICKS = 10.0F;
    private static final Map<Integer, RenderOrbitState> RENDER_STATES = new HashMap<>();

    private final ItemRenderer itemRenderer;

    public DancingBladeVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public Identifier getTexture(DancingBladeVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(DancingBladeVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        ItemStack stack = entity.getWeaponStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }

        int maxSwords = Math.max(1, net.sweenus.simplyswords.config.Config.gemPowers.dancingBlades.maxSwords);
        Vec3d center = getRenderCenter(entity, tickDelta);
        Vec3d entityPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
        double baseAngle = getSmoothRenderPhase(entity, tickDelta);
        double angle = baseAngle + ((Math.PI * 2.0) / maxSwords) * entity.getOrbitSlot();
        double bob = Math.sin(baseAngle * (BOB_SPEED / BASE_ANGULAR_SPEED) + entity.getOrbitSlot() * 1.7) * BOB_AMPLITUDE;
        Vec3d bladePos = center.add(Math.cos(angle) * Math.max(0.5F, entity.getOrbitRadius()), BLADE_HEIGHT + bob, Math.sin(angle) * Math.max(0.5F, entity.getOrbitRadius()));
        Vec3d offset = bladePos.subtract(entityPos);

        float renderAge = entity.age + tickDelta;
        float attackProgress = getAttackProgress(entity, tickDelta);
        AttackPose attackPose = getAttackPose(attackProgress);
        float spin = (float) Math.toDegrees(-angle) + 90.0F;
        float idleTilt = 35.0F + MathHelper.sin(renderAge * 0.12F + entity.getOrbitSlot()) * 7.0F;

        matrices.push();
        matrices.translate(offset.x, offset.y, offset.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(idleTilt + attackPose.impactRoll));
        BakedModel model = this.itemRenderer.getModels().getModel(stack);
        this.itemRenderer.renderItem(
                stack,
                ModelTransformationMode.GROUND,
                false,
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV,
                model
        );
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static Vec3d getRenderCenter(DancingBladeVisualEntity entity, float tickDelta) {
        Entity ownerEntity = entity.getWorld().getEntityById(entity.getOwnerEntityId());
        if (ownerEntity instanceof PlayerEntity owner) {
            return new Vec3d(
                    MathHelper.lerp(tickDelta, owner.prevX, owner.getX()),
                    MathHelper.lerp(tickDelta, owner.prevY, owner.getY()),
                    MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ())
            );
        }
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
    }

    private static double getSmoothRenderPhase(DancingBladeVisualEntity entity, float tickDelta) {
        float ageTicks = entity.age + tickDelta;
        RenderOrbitState state = RENDER_STATES.computeIfAbsent(entity.getId(), ignored -> new RenderOrbitState(entity.getOrbitPhase(), ageTicks));
        float deltaTicks = ageTicks - state.lastAgeTicks;
        if (deltaTicks < 0.0F || deltaTicks > 5.0F) {
            state.phase = entity.getOrbitPhase();
        } else if (deltaTicks > 0.0F) {
            state.phase += deltaTicks * BASE_ANGULAR_SPEED;
        }
        state.lastAgeTicks = ageTicks;
        while (state.phase > Math.PI * 2.0) {
            state.phase -= Math.PI * 2.0;
        }

        if (entity.age % 80 == 0) {
            Iterator<Map.Entry<Integer, RenderOrbitState>> iterator = RENDER_STATES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, RenderOrbitState> entry = iterator.next();
                if (entry.getKey() != entity.getId() && ageTicks - entry.getValue().lastAgeTicks > 100.0F) {
                    iterator.remove();
                }
            }
        }
        return state.phase;
    }

    private static float getAttackProgress(DancingBladeVisualEntity entity, float tickDelta) {
        float attackAge = entity.age + tickDelta - entity.getAttackStartAge();
        if (attackAge < 0.0F || attackAge > ATTACK_ANIMATION_TICKS) {
            return 0.0F;
        }
        return attackAge / ATTACK_ANIMATION_TICKS;
    }

    private static AttackPose getAttackPose(float progress) {
        if (progress <= 0.0F) {
            return AttackPose.IDLE;
        }
        if (progress < 0.25F) {
            float windup = easeOut(progress / 0.25F);
            return new AttackPose(
                    -windup * 25.0F
            );
        }
        if (progress < 0.55F) {
            float slash = easeOut((progress - 0.25F) / 0.3F);
            float impact = MathHelper.sin(slash * MathHelper.PI);
            return new AttackPose(
                    MathHelper.lerp(slash, -25.0F, 35.0F) + impact * 8.0F
            );
        }
        float recovery = easeOut((progress - 0.55F) / 0.45F);
        return new AttackPose(
                MathHelper.lerp(recovery, 35.0F, 0.0F)
        );
    }

    private static float easeOut(float progress) {
        float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
        return 1.0F - (1.0F - clamped) * (1.0F - clamped);
    }

    private record AttackPose(float impactRoll) {
        private static final AttackPose IDLE = new AttackPose(0.0F);
    }

    private static final class RenderOrbitState {
        private double phase;
        private float lastAgeTicks;

        private RenderOrbitState(double phase, float ageTicks) {
            this.phase = phase;
            this.lastAgeTicks = ageTicks;
        }
    }
}
