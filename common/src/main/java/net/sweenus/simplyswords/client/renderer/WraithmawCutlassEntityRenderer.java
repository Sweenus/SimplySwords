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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.entity.WraithmawCutlassEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WraithmawCutlassEntityRenderer extends EntityRenderer<WraithmawCutlassEntity> {
    private static final double POSITION_RESPONSE = 0.6;
    private static final double DIRECTION_RESPONSE = 0.6;
    private static final Map<Integer, RenderState> RENDER_STATES = new HashMap<>();
    private static final double[][] ORBIT_SLOTS = {
            {-0.62, 1.55, 0.12},
            {0.62, 1.55, 0.12},
            {-0.92, 2.02, 0.10},
            {-0.31, 2.24, 0.14},
            {0.31, 2.24, 0.14},
            {0.92, 2.02, 0.10},
            {-0.52, 2.62, 0.18},
            {0.52, 2.62, 0.18}
    };
    private final ItemRenderer itemRenderer;

    public WraithmawCutlassEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(WraithmawCutlassEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(WraithmawCutlassEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(4.0));
    }

    @Override
    public void render(WraithmawCutlassEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        ItemStack stack = entity.getWeaponStack();
        if (stack == null || stack.isEmpty()) {
            stack = ItemsRegistry.WRAITHMAW.get().getDefaultStack();
        }
        RenderPose pose = resolveRenderPose(entity, tickDelta);
        if (pose == null) {
            return;
        }
        Vec3d offset = pose.offset;
        int state = entity.getState();
        Vec3d direction = pose.direction;
        if ((state == WraithmawCutlassEntity.STATE_FALLING
                || state == WraithmawCutlassEntity.STATE_LAUNCHED)
                && direction != null && direction.lengthSquared() > 1.0E-5) {
            drawTrail(matrices, consumers, offset, direction,
                    state == WraithmawCutlassEntity.STATE_FALLING ? 2.8 : 2.1);
        }

        matrices.push();
        matrices.translate(offset.x, offset.y, offset.z);
        float age = entity.age + tickDelta;
        if (state == WraithmawCutlassEntity.STATE_FALLING
                || state == WraithmawCutlassEntity.STATE_EMBEDDED) {
            matrices.multiply(this.dispatcher.getRotation());
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-135.0F));
            if (state == WraithmawCutlassEntity.STATE_EMBEDDED) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(Math.floorMod(entity.getSeed(), 31) - 15.0F));
                matrices.translate(0.0, -0.15, 0.0);
            }
        } else if (state == WraithmawCutlassEntity.STATE_LAUNCHED
                && direction != null && direction.lengthSquared() > 1.0E-5) {
            float horizontal = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            float visualYaw = (float) -Math.toDegrees(Math.atan2(direction.x, direction.z));
            float visualPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(visualYaw));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(visualPitch - 45.0F));
        } else if (state == WraithmawCutlassEntity.STATE_ORBITING) {
            Entity ownerEntity = entity.getWorld().getEntityById(entity.getOwnerId());
            if (ownerEntity instanceof LivingEntity owner) {
                float ownerYaw = MathHelper.lerpAngleDegrees(tickDelta, owner.prevYaw, owner.getYaw());
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-ownerYaw - 90.0F));
            }
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45.0F));
        } else {
            matrices.multiply(this.dispatcher.getRotation());
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45.0F + age * 8.0F));
        }
        float pulse = 1.0F + MathHelper.sin(age * 0.18F + entity.getSeed()) * 0.045F;
        matrices.scale(pulse, pulse, pulse);
        BakedModel model = itemRenderer.getModels().getModel(stack);
        itemRenderer.renderItem(stack, ModelTransformationMode.GROUND, false, matrices,
                consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV, model);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private RenderPose resolveRenderPose(WraithmawCutlassEntity entity, float tickDelta) {
        Vec3d entityPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
        int cutlassState = entity.getState();
        Vec3d targetPosition = entityPos;
        Vec3d targetDirection = normalizedDirection(entity.getVelocity());
        if (cutlassState == WraithmawCutlassEntity.STATE_ORBITING) {
            Entity ownerEntity = entity.getWorld().getEntityById(entity.getOwnerId());
            if (!(ownerEntity instanceof LivingEntity owner)
                    || !owner.getMainHandStack().isOf(ItemsRegistry.WRAITHMAW.get())
                    && !owner.getOffHandStack().isOf(ItemsRegistry.WRAITHMAW.get())) {
                RENDER_STATES.remove(entity.getId());
                return null;
            }
            targetPosition = orbitPosition(entity, owner, tickDelta);
            double ownerYaw = Math.toRadians(MathHelper.lerpAngleDegrees(
                    tickDelta, owner.prevYaw, owner.getYaw()));
            targetDirection = new Vec3d(-Math.sin(ownerYaw), 0.0, Math.cos(ownerYaw));
        }

        float ageTicks = entity.age + tickDelta;
        long worldTick = entity.getWorld().getTime();
        RenderState renderState = RENDER_STATES.get(entity.getId());
        float deltaTicks = renderState == null ? 0.0F : ageTicks - renderState.lastAgeTicks;
        boolean reset = renderState == null
                || renderState.world != entity.getWorld()
                || !entity.getUuid().equals(renderState.entityUuid)
                || deltaTicks < 0.0F || deltaTicks > 5.0F;
        if (!reset && renderState.state == WraithmawCutlassEntity.STATE_ORBITING
                && cutlassState == WraithmawCutlassEntity.STATE_LAUNCHED) {
            targetPosition = entity.getPos();
        }
        if (reset) {
            renderState = new RenderState(entity.getWorld(), entity.getUuid(), targetPosition,
                    targetDirection, ageTicks, worldTick, cutlassState);
            RENDER_STATES.put(entity.getId(), renderState);
        } else {
            if (cutlassState == WraithmawCutlassEntity.STATE_ORBITING) {
                renderState.position = targetPosition;
            } else if (deltaTicks > 0.0F) {
                renderState.position = renderState.position.lerp(targetPosition,
                        response(POSITION_RESPONSE, deltaTicks));
            }
            if (cutlassState == WraithmawCutlassEntity.STATE_ORBITING) {
                renderState.direction = targetDirection;
            } else if ((cutlassState == WraithmawCutlassEntity.STATE_FALLING
                    || cutlassState == WraithmawCutlassEntity.STATE_LAUNCHED)
                    && targetDirection != null) {
                if (renderState.direction == null) {
                    renderState.direction = targetDirection;
                } else if (deltaTicks > 0.0F) {
                    renderState.direction = interpolateDirection(
                            renderState.direction, targetDirection,
                            response(DIRECTION_RESPONSE, deltaTicks));
                }
            }
            renderState.lastAgeTicks = ageTicks;
            renderState.lastWorldTick = worldTick;
            renderState.state = cutlassState;
        }
        if (entity.age % 80 == 0) {
            long cutoff = worldTick - 100L;
            RENDER_STATES.entrySet().removeIf(entry ->
                    entry.getKey() != entity.getId()
                            && (entry.getValue().world != entity.getWorld()
                            || entry.getValue().lastWorldTick < cutoff));
        }
        return new RenderPose(renderState.position.subtract(entityPos), renderState.direction);
    }

    private Vec3d orbitPosition(WraithmawCutlassEntity entity, LivingEntity owner, float tickDelta) {
        int slot = Math.floorMod(entity.getOrbitSlot(), ORBIT_SLOTS.length);
        double localX = ORBIT_SLOTS[slot][0];
        double localY = ORBIT_SLOTS[slot][1]
                + Math.sin((entity.age + tickDelta) * 0.11 + slot * 1.7) * 0.08;
        double localZ = ORBIT_SLOTS[slot][2];
        double ownerX = MathHelper.lerp(tickDelta, owner.prevX, owner.getX());
        double ownerY = MathHelper.lerp(tickDelta, owner.prevY, owner.getY());
        double ownerZ = MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ());
        double ownerYaw = Math.toRadians(MathHelper.lerpAngleDegrees(tickDelta, owner.prevYaw, owner.getYaw()));
        Vec3d right = new Vec3d(Math.cos(ownerYaw), 0.0, Math.sin(ownerYaw));
        Vec3d forward = new Vec3d(-Math.sin(ownerYaw), 0.0, Math.cos(ownerYaw));
        return new Vec3d(ownerX, ownerY, ownerZ)
                .add(right.multiply(localX)).add(forward.multiply(localZ)).add(0.0, localY, 0.0);
    }

    private static Vec3d normalizedDirection(Vec3d direction) {
        return direction.lengthSquared() > 1.0E-5 ? direction.normalize() : null;
    }

    private static double response(double rate, float deltaTicks) {
        return 1.0 - Math.pow(1.0 - rate, deltaTicks);
    }

    private static Vec3d interpolateDirection(Vec3d current, Vec3d target, double progress) {
        Vec3d blended = current.multiply(1.0 - progress).add(target.multiply(progress));
        return blended.lengthSquared() > 1.0E-5 ? blended.normalize() : target;
    }

    private static void drawTrail(MatrixStack matrices, VertexConsumerProvider consumers,
                                  Vec3d offset, Vec3d direction, double length) {
        Vec3d end = offset.subtract(direction.multiply(0.36));
        Vec3d start = end.subtract(direction.multiply(length));
        Vec3d side = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 1.0E-5) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        Vec3d up = direction.crossProduct(side).normalize();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getDebugQuads());
        drawRibbon(vertices, matrix, start, end, side.multiply(0.12), 92, 14, 164, 120);
        drawRibbon(vertices, matrix, start, end, up.multiply(0.055), 210, 104, 255, 205);
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
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    private record RenderPose(Vec3d offset, Vec3d direction) {
    }

    private static final class RenderState {
        private final World world;
        private final UUID entityUuid;
        private Vec3d position;
        private Vec3d direction;
        private float lastAgeTicks;
        private long lastWorldTick;
        private int state;

        private RenderState(World world, UUID entityUuid, Vec3d position, Vec3d direction,
                            float lastAgeTicks, long lastWorldTick, int state) {
            this.world = world;
            this.entityUuid = entityUuid;
            this.position = position;
            this.direction = direction;
            this.lastAgeTicks = lastAgeTicks;
            this.lastWorldTick = lastWorldTick;
            this.state = state;
        }
    }
}
