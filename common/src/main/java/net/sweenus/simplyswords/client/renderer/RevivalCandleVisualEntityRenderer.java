package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.RevivalCandleVisualEntity;

public class RevivalCandleVisualEntityRenderer extends EntityRenderer<RevivalCandleVisualEntity> {

    private static final double IDLE_SIDE_OFFSET = 0.72;
    private static final double IDLE_BACK_OFFSET = -0.18;
    private static final double IDLE_HEIGHT_OFFSET = 0.08;
    private static final double ACTIVE_FORWARD_OFFSET = 1.15;
    private static final double ACTIVE_HEIGHT_OFFSET = -0.08;
    private static final double BOB_AMPLITUDE = 0.045;
    private static final double BOB_SPEED = 0.18;
    private static final float CANDLE_RENDER_SCALE = 1.04F;

    public RevivalCandleVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(RevivalCandleVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(RevivalCandleVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float scale = MathHelper.clamp(entity.getScale(), 0.0F, 1.4F);
        if (scale <= 0.01F) {
            return;
        }

        matrices.push();
        Entity ownerEntity = entity.getWorld().getEntityById(entity.getOwnerEntityId());
        if (ownerEntity instanceof PlayerEntity owner) {
            applyRenderFollowAnchor(entity, owner, tickDelta, matrices);
        }

        matrices.scale(CANDLE_RENDER_SCALE * scale, CANDLE_RENDER_SCALE * scale, CANDLE_RENDER_SCALE * scale);
        matrices.translate(-0.5, 0.0, -0.5);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                getCandleState(entity),
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }

    private static void applyRenderFollowAnchor(RevivalCandleVisualEntity candle, PlayerEntity owner, float tickDelta, MatrixStack matrices) {
        Vec3d anchor = candle.isActivating()
                ? getActivationAnchor(owner, tickDelta)
                : getIdleAnchor(candle, owner, tickDelta);
        Vec3d entityPos = new Vec3d(
                MathHelper.lerp(tickDelta, candle.prevX, candle.getX()),
                MathHelper.lerp(tickDelta, candle.prevY, candle.getY()),
                MathHelper.lerp(tickDelta, candle.prevZ, candle.getZ())
        );
        Vec3d correction = anchor.subtract(entityPos);
        matrices.translate(correction.x, correction.y, correction.z);
    }

    private static Vec3d getIdleAnchor(RevivalCandleVisualEntity candle, PlayerEntity owner, float tickDelta) {
        float ownerYaw = MathHelper.lerpAngleDegrees(tickDelta, owner.prevYaw, owner.getYaw());
        Vec3d ownerPos = new Vec3d(
                MathHelper.lerp(tickDelta, owner.prevX, owner.getX()),
                MathHelper.lerp(tickDelta, owner.prevY, owner.getY()),
                MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ())
        );
        Vec3d forward = Vec3d.fromPolar(0.0F, ownerYaw).normalize();
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        double side = candle.getWeaponType() == 2 ? -IDLE_SIDE_OFFSET : IDLE_SIDE_OFFSET;
        double bob = Math.sin((candle.age + tickDelta) * BOB_SPEED) * BOB_AMPLITUDE;
        return ownerPos
                .add(0.0, owner.getStandingEyeHeight() + IDLE_HEIGHT_OFFSET + bob, 0.0)
                .add(right.multiply(side))
                .add(forward.multiply(IDLE_BACK_OFFSET));
    }

    private static Vec3d getActivationAnchor(PlayerEntity owner, float tickDelta) {
        Vec3d ownerPos = new Vec3d(
                MathHelper.lerp(tickDelta, owner.prevX, owner.getX()),
                MathHelper.lerp(tickDelta, owner.prevY, owner.getY()),
                MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ())
        );
        Vec3d look = owner.getRotationVec(tickDelta).normalize();
        return ownerPos
                .add(0.0, owner.getStandingEyeHeight() + ACTIVE_HEIGHT_OFFSET, 0.0)
                .add(look.multiply(ACTIVE_FORWARD_OFFSET));
    }

    private static BlockState getCandleState(RevivalCandleVisualEntity entity) {
        return switch (entity.getWeaponType()) {
            case 2 -> Blocks.ORANGE_CANDLE.getDefaultState();
            default -> Blocks.CANDLE.getDefaultState();
        };
    }
}
