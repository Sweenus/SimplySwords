package net.sweenus.simplyswords.client.renderer;

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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.ThrownSpearEntity;
import net.sweenus.simplyswords.entity.WraithfangEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.world.WickpiercerThrowMath;
import org.joml.Matrix4f;

public class ThrownSpearEntityRenderer extends EntityRenderer<ThrownSpearEntity> {
    private final ItemRenderer itemRenderer;

    public ThrownSpearEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownSpearEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        ItemStack swordStack = entity.getItemStack();
        if (swordStack == null || swordStack.isEmpty()) {
            //System.out.println("ThrownSwordEntityRenderer: swordStack is null or empty! Using default ItemStack.");
            swordStack = ItemsRegistry.LIVYATAN.get().getDefaultStack(); // Fallback in case the entity's stack is empty
        }

        if (swordStack != null && !swordStack.isEmpty()) {
            OrbitRenderPose orbitPose = resolveOrbitPose(entity, swordStack, tickDelta);
            if (orbitPose != null) {
                drawOrbitTrail(matrices, vertexConsumers,
                        orbitPose.offset.add(0.0, 0.5, 0.0), orbitPose.direction);
            }
            matrices.push();
            if (orbitPose != null) {
                matrices.translate(orbitPose.offset.x, orbitPose.offset.y, orbitPose.offset.z);
            }
            matrices.translate(0, 0.5, 0);
            matrices.scale(1.0F, 1.0F, 1.0F);
            if (orbitPose != null) {
                Vec3d direction = orbitPose.direction;
                float horizontal = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);
                float flightYaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z)) - 90.0F;
                float flightPitch = (float) Math.toDegrees(Math.atan2(direction.y, horizontal)) - 45.0F;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(flightYaw));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(flightPitch));
            } else if (entity instanceof WraithfangEntity fang && fang.hasWraithridePresentation()) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(fang.flightYaw(tickDelta)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(fang.flightPitch(tickDelta)));
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-entity.getPitch()));
            }
            BakedModel model = this.itemRenderer.getModels().getModel(swordStack); // Get the BakedModel for the swordStack
            this.itemRenderer.renderItem(
                    swordStack,
                    ModelTransformationMode.GROUND,
                    false,
                    matrices,
                    vertexConsumers,
                    light,
                    OverlayTexture.DEFAULT_UV,
                    model
            );

            matrices.translate(0, 0.25, 0);
            matrices.pop();
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static OrbitRenderPose resolveOrbitPose(ThrownSpearEntity entity, ItemStack stack,
                                                     float tickDelta) {
        if (!stack.isOf(ItemsRegistry.WICKPIERCER.get()) || !entity.hasOrbitPresentation()) {
            return null;
        }
        Entity targetEntity = entity.getWorld().getEntityById(entity.getOrbitTargetId());
        if (!(targetEntity instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) {
            return null;
        }
        int elapsedTicks = (int) entity.getWorld().getTime() - entity.getOrbitStartTick();
        double elapsed = elapsedTicks + tickDelta;
        int duration = entity.getOrbitDurationTicks();
        if (elapsed < 0.0 || elapsed > duration + 1.0) {
            return null;
        }
        double pathElapsed = Math.min(elapsed, duration);
        Vec3d targetCenter = new Vec3d(
                MathHelper.lerp(tickDelta, target.prevX, target.getX()),
                MathHelper.lerp(tickDelta, target.prevY, target.getY()) + target.getHeight() * 0.55,
                MathHelper.lerp(tickDelta, target.prevZ, target.getZ()));
        Vec3d entityPosition = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
        Vec3d currentOffset = WickpiercerThrowMath.orbitOffset(
                pathElapsed, entity.getOrbitIntervalTicks(), entity.getOrbitPhase(),
                WickpiercerThrowMath.DEFAULT_ORBIT_RADIUS);
        Vec3d desiredPosition = targetCenter.add(currentOffset);

        double sampleDistance = 0.08;
        double before = Math.max(0.0, pathElapsed - sampleDistance);
        double after = Math.min(duration, pathElapsed + sampleDistance);
        Vec3d beforeOffset = WickpiercerThrowMath.orbitOffset(
                before, entity.getOrbitIntervalTicks(), entity.getOrbitPhase(),
                WickpiercerThrowMath.DEFAULT_ORBIT_RADIUS);
        Vec3d afterOffset = WickpiercerThrowMath.orbitOffset(
                after, entity.getOrbitIntervalTicks(), entity.getOrbitPhase(),
                WickpiercerThrowMath.DEFAULT_ORBIT_RADIUS);
        double cycleAge = pathElapsed % entity.getOrbitIntervalTicks();
        double edgeWindow = sampleDistance;
        Vec3d direction;
        if (pathElapsed >= duration || cycleAge > entity.getOrbitIntervalTicks() - edgeWindow
                || pathElapsed > 0.0 && cycleAge < 1.0E-6) {
            direction = currentOffset.subtract(beforeOffset);
        } else if (cycleAge < edgeWindow) {
            direction = afterOffset.subtract(currentOffset);
        } else {
            direction = afterOffset.subtract(beforeOffset);
        }
        if (direction.lengthSquared() < 1.0E-6) {
            direction = entity.getVelocity();
        }
        if (direction.lengthSquared() < 1.0E-6) {
            direction = new Vec3d(0.0, 0.0, 1.0);
        }
        return new OrbitRenderPose(desiredPosition.subtract(entityPosition), direction.normalize());
    }

    private static void drawOrbitTrail(MatrixStack matrices, VertexConsumerProvider consumers,
                                       Vec3d end, Vec3d direction) {
        Vec3d start = end.subtract(direction.multiply(1.0));
        Vec3d side = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 1.0E-5) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        Vec3d up = direction.crossProduct(side).normalize();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getDebugQuads());
        drawRibbon(vertices, matrix, start, end, side.multiply(0.09), 255, 92, 18, 112);
        drawRibbon(vertices, matrix, start, end, up.multiply(0.035), 255, 214, 96, 210);
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

    private record OrbitRenderPose(Vec3d offset, Vec3d direction) {
    }

    @Override
    public Identifier getTexture(ThrownSpearEntity entity) {
        // This renderer doesn't use a specific texture, as it renders the ItemStack
        return null;
    }
}
