package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.entity.GloampiercerSpearEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GloampiercerSpearEntityRenderer extends EntityRenderer<GloampiercerSpearEntity> {
    private static final double EMBEDDED_VISUAL_BACKOFF = 0.65;
    private static final Map<Integer, RenderState> RENDER_STATES = new HashMap<>();
    private final ItemRenderer itemRenderer;

    public GloampiercerSpearEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(GloampiercerSpearEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(GloampiercerSpearEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(4.0));
    }

    @Override
    public void render(GloampiercerSpearEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        RenderPose pose = resolvePose(entity, tickDelta);
        if (entity.getState() == GloampiercerSpearEntity.STATE_FLYING) {
            drawTrail(matrices, consumers, pose.offset, pose.direction, entity.getSeed());
        }
        ItemStack stack = entity.getWeaponStack();
        if (stack == null || stack.isEmpty()) {
            stack = ItemsRegistry.GLOAMPIERCER.get().getDefaultStack();
        }
        boolean embedded = entity.getState() == GloampiercerSpearEntity.STATE_EMBEDDED;
        Vec3d direction = pose.direction.lengthSquared() < 1.0E-6
                ? new Vec3d(0.0, -1.0, 0.0)
                : pose.direction.normalize();
        Vec3d visualOffset = embedded
                ? direction.multiply(-EMBEDDED_VISUAL_BACKOFF)
                : Vec3d.ZERO;
        matrices.push();
        matrices.translate(pose.offset.x + visualOffset.x,
                pose.offset.y + visualOffset.y,
                pose.offset.z + visualOffset.z);
        orientSpear(matrices, direction, pose.position.add(visualOffset));
        float pulse = 1.14F + MathHelper.sin((entity.age + tickDelta) * 0.32F + entity.getSeed()) * 0.035F;
        matrices.scale(pulse, pulse, pulse);
        BakedModel model = itemRenderer.getModels().getModel(stack);
        itemRenderer.renderItem(stack, ModelTransformationMode.GROUND, false, matrices, consumers,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, model);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private void orientSpear(MatrixStack matrices, Vec3d direction, Vec3d position) {
        Vec3d normalized = direction.lengthSquared() < 1.0E-6
                ? new Vec3d(0.0, -1.0, 0.0)
                : direction.normalize();
        Vector3f axis = new Vector3f((float) normalized.x, (float) normalized.y, (float) normalized.z);
        Vector3f localTip = new Vector3f(1.0F, 1.0F, 0.0F).normalize();
        Quaternionf alignment = new Quaternionf().rotationTo(localTip, axis);
        Vector3f currentNormal = alignment.transform(new Vector3f(0.0F, 0.0F, 1.0F));
        Vec3d cameraOffset = dispatcher.camera == null
                ? new Vec3d(0.0, 0.0, 1.0)
                : dispatcher.camera.getPos().subtract(position);
        Vec3d facing = cameraOffset.subtract(normalized.multiply(cameraOffset.dotProduct(normalized)));
        if (facing.lengthSquared() < 1.0E-6) {
            Vec3d fallback = Math.abs(normalized.y) < 0.9
                    ? new Vec3d(0.0, 1.0, 0.0)
                    : new Vec3d(0.0, 0.0, 1.0);
            facing = fallback.subtract(normalized.multiply(fallback.dotProduct(normalized)));
        }
        Vector3f desiredNormal = new Vector3f((float) facing.x, (float) facing.y, (float) facing.z).normalize();
        float sine = axis.dot(new Vector3f(currentNormal).cross(desiredNormal));
        float cosine = MathHelper.clamp(currentNormal.dot(desiredNormal), -1.0F, 1.0F);
        float roll = (float) Math.atan2(sine, cosine);
        matrices.multiply(new Quaternionf().fromAxisAngleRad(axis, roll).mul(alignment));
    }

    private RenderPose resolvePose(GloampiercerSpearEntity entity, float tickDelta) {
        Vec3d targetPosition = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
        Vec3d targetDirection = entity.getFlightDirection();
        if (targetDirection.lengthSquared() < 1.0E-6) {
            targetDirection = new Vec3d(0.0, -1.0, 0.0);
        } else {
            targetDirection = targetDirection.normalize();
        }
        float ageTicks = entity.age + tickDelta;
        long worldTick = entity.getWorld().getTime();
        RenderState state = RENDER_STATES.get(entity.getId());
        float delta = state == null ? 0.0F : ageTicks - state.lastAge;
        if (state == null || state.world != entity.getWorld() || !state.uuid.equals(entity.getUuid())
                || delta < 0.0F || delta > 5.0F) {
            state = new RenderState(entity.getWorld(), entity.getUuid(), targetPosition,
                    targetDirection, ageTicks, worldTick);
            RENDER_STATES.put(entity.getId(), state);
        } else if (delta > 0.0F) {
            double positionResponse = 1.0 - Math.pow(0.34, delta);
            double directionResponse = 1.0 - Math.pow(0.42, delta);
            state.position = state.position.lerp(targetPosition, positionResponse);
            Vec3d blended = state.direction.multiply(1.0 - directionResponse)
                    .add(targetDirection.multiply(directionResponse));
            state.direction = blended.lengthSquared() < 1.0E-6 ? targetDirection : blended.normalize();
            state.lastAge = ageTicks;
            state.lastWorldTick = worldTick;
        }
        if (entity.age % 80 == 0) {
            long cutoff = worldTick - 100L;
            RENDER_STATES.entrySet().removeIf(entry -> entry.getKey() != entity.getId()
                    && (entry.getValue().world != entity.getWorld()
                    || entry.getValue().lastWorldTick < cutoff));
        }
        return new RenderPose(state.position.subtract(targetPosition), state.direction, state.position);
    }

    private static void drawTrail(MatrixStack matrices, VertexConsumerProvider consumers,
                                  Vec3d offset, Vec3d direction, int seed) {
        Vec3d end = offset.subtract(direction.multiply(0.32));
        Vec3d start = end.subtract(direction.multiply(2.7));
        Vec3d side = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 1.0E-6) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        Vec3d up = direction.crossProduct(side).normalize();
        double shimmer = 0.8 + Math.sin(seed * 0.17) * 0.12;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getDebugQuads());
        drawRibbon(vertices, matrix, start, end, side.multiply(0.14 * shimmer), 24, 8, 54, 125);
        drawRibbon(vertices, matrix, start, end, up.multiply(0.052), 68, 235, 240, 215);
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix,
                                   Vec3d start, Vec3d end, Vec3d width,
                                   int red, int green, int blue, int alpha) {
        vertex(vertices, matrix, start.add(width), red, green, blue, 0);
        vertex(vertices, matrix, start.subtract(width), red, green, blue, 0);
        vertex(vertices, matrix, end.subtract(width), red, green, blue, alpha);
        vertex(vertices, matrix, end.add(width), red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3d point,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .next();
    }

    private record RenderPose(Vec3d offset, Vec3d direction, Vec3d position) {
    }

    private static final class RenderState {
        private final World world;
        private final UUID uuid;
        private Vec3d position;
        private Vec3d direction;
        private float lastAge;
        private long lastWorldTick;

        private RenderState(World world, UUID uuid, Vec3d position, Vec3d direction,
                            float lastAge, long lastWorldTick) {
            this.world = world;
            this.uuid = uuid;
            this.position = position;
            this.direction = direction;
            this.lastAge = lastAge;
            this.lastWorldTick = lastWorldTick;
        }
    }
}
