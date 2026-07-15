package net.sweenus.simplyswords.client.renderer;

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
import net.sweenus.simplyswords.entity.SoulkeeperLanternVisualEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SoulkeeperLanternVisualEntityRenderer extends EntityRenderer<SoulkeeperLanternVisualEntity> {

    private static final double BASE_ANGULAR_SPEED = 0.055;
    private static final double LANTERN_HEIGHT = 1.05;
    private static final double BOB_AMPLITUDE = 0.08;
    private static final double BOB_SPEED = 0.13;
    private static final float LANTERN_RENDER_SCALE = 1.0F;
    private static final Map<Integer, RenderOrbitState> RENDER_STATES = new HashMap<>();

    public SoulkeeperLanternVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(SoulkeeperLanternVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(SoulkeeperLanternVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        int count = MathHelper.clamp(entity.getLanternCount(), 0, 4);
        if (count <= 0) {
            return;
        }

        Vec3d center = getRenderCenter(entity, tickDelta);
        Vec3d entityPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
        float speedMultiplier = Math.max(1.0F, entity.getSpeedMultiplier());
        double baseAngle = getSmoothRenderPhase(entity, tickDelta, speedMultiplier);
        double radius = Math.max(0.25F, entity.getOrbitRadius());

        for (int i = 0; i < count; i++) {
            double angle = baseAngle + ((Math.PI * 2.0) / count) * i;
            double bob = Math.sin(baseAngle * (BOB_SPEED / BASE_ANGULAR_SPEED) + i * 1.7) * BOB_AMPLITUDE;
            Vec3d lanternPos = center.add(Math.cos(angle) * radius, LANTERN_HEIGHT + bob, Math.sin(angle) * radius);
            Vec3d offset = lanternPos.subtract(entityPos);

            matrices.push();
            matrices.translate(offset.x, offset.y, offset.z);
            matrices.scale(LANTERN_RENDER_SCALE, LANTERN_RENDER_SCALE, LANTERN_RENDER_SCALE);
            matrices.translate(-0.5, 0.0, -0.5);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                    Blocks.SOUL_LANTERN.getDefaultState(),
                    matrices,
                    vertexConsumers,
                    light,
                    OverlayTexture.DEFAULT_UV
            );
            matrices.pop();
        }
    }

    private static Vec3d getRenderCenter(SoulkeeperLanternVisualEntity entity, float tickDelta) {
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

    private static double getSmoothRenderPhase(SoulkeeperLanternVisualEntity entity, float tickDelta, float speedMultiplier) {
        float ageTicks = entity.age + tickDelta;
        RenderOrbitState state = RENDER_STATES.computeIfAbsent(entity.getId(), ignored -> new RenderOrbitState(entity.getOrbitPhase(), ageTicks));
        float deltaTicks = ageTicks - state.lastAgeTicks;
        if (deltaTicks < 0.0F || deltaTicks > 5.0F) {
            state.phase = entity.getOrbitPhase();
        } else if (deltaTicks > 0.0F) {
            state.phase += deltaTicks * BASE_ANGULAR_SPEED * speedMultiplier;
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

    private static final class RenderOrbitState {
        private double phase;
        private float lastAgeTicks;

        private RenderOrbitState(double phase, float ageTicks) {
            this.phase = phase;
            this.lastAgeTicks = ageTicks;
        }
    }
}
