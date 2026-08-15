package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IonDeferredRenderController;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulstalkerStrideContactSolver;
import net.sweenus.simplyswords.entity.SoulstalkerStrideEntity;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

public final class SoulstalkerStrideEntityRenderer extends EntityRenderer<SoulstalkerStrideEntity> {
    private static final Identifier WHITE = Identifier.ofVanilla("textures/misc/white.png");
    private static final int LEG_COUNT = 6;
    private static final int PATH_POINTS = 10;
    private static final int[] TRANSITION_ORDER = {0, 3, 1, 4, 2, 5};
    private static final int DARK_CORE = 0x08040D;
    private static final int DARK_PURPLE = 0x160922;
    private static final int GLOW_PURPLE = 0x68208F;
    private static final Map<SoulstalkerStrideEntity, RigState> RIGS = new WeakHashMap<>();

    public SoulstalkerStrideEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(SoulstalkerStrideEntity entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(SoulstalkerStrideEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(4.5, 3.0, 4.5));
    }

    @Override
    public void render(SoulstalkerStrideEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        float time = entity.age + tickDelta;
        float appear = ease(MathHelper.clamp(time / 7.0F, 0.0F, 1.0F));
        if (appear <= 0.01F) {
            return;
        }
        Vec3d entityPosition = entity.getLerpedPos(tickDelta);
        LivingEntity owner = entity.getOwner();
        Vec3d ownerPosition = owner == null
                ? entityPosition.add(0.0, Config.uniqueEffects.soulstalker.riderHeight, 0.0)
                : owner.getLerpedPos(tickDelta);
        float bodyYaw = owner == null ? entity.getYaw(tickDelta)
                : MathHelper.lerpAngleDegrees(tickDelta, owner.prevBodyYaw, owner.bodyYaw);
        Vec3d forward = horizontalFacing(bodyYaw);
        Vec3d back = forward.multiply(-1.0);
        Vec3d right = rightFacing(bodyYaw);
        byte surfaceMode = entity.getSurfaceMode();
        RigState rig = RIGS.computeIfAbsent(entity, ignored -> new RigState());
        rig.beginFrame(entity, time);
        Vec3d[][] paths = new Vec3d[LEG_COUNT][];
        for (int leg = 0; leg < LEG_COUNT; leg++) {
            Vec3d root = rootPosition(ownerPosition, back, right, leg);
            SoulstalkerStrideContactSolver.Contact contact = surfaceMode == SoulstalkerStrideEntity.SURFACE_AIRBORNE
                    ? airborneContact(ownerPosition, back, right, leg)
                    : SoulstalkerStrideContactSolver.find(entity.getWorld(), entity, entityPosition, bodyYaw,
                    MathHelper.clamp(Config.uniqueEffects.soulstalker.riderHeight, 0.5, 4.0),
                    surfaceMode, entity.getSurfaceNormal(), leg);
            Vec3d foot = rig.update(leg, contact.position(), contact.normal(), time, surfaceMode);
            paths[leg] = buildPath(root, foot, contact.normal(), back, right,
                    entityPosition, leg, time, surfaceMode);
        }
        boolean firstPersonOwner = isFirstPersonOwner(entity);
        int packedLight = LightmapTextureManager.pack(
                Math.max(5, LightmapTextureManager.getBlockLightCoordinates(light)),
                LightmapTextureManager.getSkyLightCoordinates(light));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer dark = consumers.getBuffer(RenderLayer.getDebugQuads());
        for (int leg = 0; leg < LEG_COUNT; leg++) {
            Vec3d[] path = paths[leg];
            int start = firstPersonOwner ? 2 : 0;
            for (int segment = start; segment < path.length - 1; segment++) {
                float progress = segment / (float) (path.length - 1);
                float width = MathHelper.lerp(progress, 0.17F, 0.058F) * appear;
                int color = ((segment + leg) & 1) == 0 ? DARK_CORE : DARK_PURPLE;
                SoulstalkerRenderGeometry.prism(dark, matrix, path[segment], path[segment + 1],
                        width, color, Math.round(246.0F * appear), packedLight, false);
            }
        }
        VertexConsumer glow = IonDeferredRenderController.getLightningBuffer();
        for (int leg = 0; leg < LEG_COUNT; leg++) {
            Vec3d[] path = paths[leg];
            int start = firstPersonOwner ? 2 : 0;
            for (int segment = start; segment < path.length - 1; segment++) {
                float progress = segment / (float) (path.length - 1);
                float pulse = 0.68F + MathHelper.sin(time * 0.16F + leg * 1.7F + progress * 4.0F) * 0.14F;
                SoulstalkerRenderGeometry.crossRibbon(glow, matrix,
                        path[segment], path[segment + 1],
                        MathHelper.lerp(progress, 0.022F, 0.008F) * appear,
                        GLOW_PURPLE, Math.round(112.0F * appear * pulse));
            }
        }
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static Vec3d rootPosition(Vec3d ownerPosition, Vec3d back, Vec3d right, int leg) {
        int row = leg / 2;
        double side = (leg & 1) == 0 ? -1.0 : 1.0;
        double vertical = 0.38 + row * 0.37;
        double backward = 0.22 + row * 0.035;
        double lateral = side * (0.13 + row * 0.025);
        return ownerPosition.add(back.multiply(backward))
                .add(right.multiply(lateral)).add(0.0, vertical, 0.0);
    }

    private static SoulstalkerStrideContactSolver.Contact airborneContact(
            Vec3d ownerPosition, Vec3d back, Vec3d right, int leg) {
        int row = leg / 2;
        double side = (leg & 1) == 0 ? -1.0 : 1.0;
        Vec3d position = ownerPosition.add(back.multiply(0.9 + row * 0.28))
                .add(right.multiply(side * (0.34 + row * 0.08)))
                .add(0.0, 0.15 + row * 0.2, 0.0);
        return new SoulstalkerStrideContactSolver.Contact(position, back,
                net.minecraft.util.math.BlockPos.ofFloored(position), false);
    }

    private static Vec3d[] buildPath(Vec3d rootWorld, Vec3d footWorld, Vec3d contactNormal,
                                     Vec3d back, Vec3d right, Vec3d entityPosition,
                                     int leg, float time, byte surfaceMode) {
        Vec3d line = footWorld.subtract(rootWorld);
        double distance = Math.max(0.001, line.length());
        int row = leg / 2;
        double sideSign = (leg & 1) == 0 ? -1.0 : 1.0;
        Vec3d normal = contactNormal.lengthSquared() < 1.0E-6
                ? new Vec3d(0.0, 1.0, 0.0) : contactNormal.normalize();
        Vec3d firstControl = rootWorld.add(back.multiply(0.72 + row * 0.08))
                .add(right.multiply(sideSign * (0.12 + row * 0.04)))
                .add(0.0, surfaceMode == SoulstalkerStrideEntity.SURFACE_CEILING ? 0.08 : 0.22, 0.0);
        Vec3d secondControl = footWorld.add(normal.multiply(Math.min(1.15, 0.48 + distance * 0.13)))
                .add(right.multiply(-sideSign * 0.1));
        Vec3d[] points = new Vec3d[PATH_POINTS];
        for (int index = 0; index < PATH_POINTS; index++) {
            double progress = index / (double) (PATH_POINTS - 1);
            double inverse = 1.0 - progress;
            Vec3d point = rootWorld.multiply(inverse * inverse * inverse)
                    .add(firstControl.multiply(3.0 * inverse * inverse * progress))
                    .add(secondControl.multiply(3.0 * inverse * progress * progress))
                    .add(footWorld.multiply(progress * progress * progress));
            double envelope = Math.sin(progress * Math.PI);
            double flex = Math.sin(time * 0.11 + leg * 1.9 + progress * Math.PI * 2.0)
                    * envelope * 0.045;
            points[index] = point.add(right.multiply(flex * sideSign)).subtract(entityPosition);
        }
        return points;
    }

    private static Vec3d horizontalFacing(float yawDegrees) {
        float yaw = yawDegrees * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.sin(yaw), 0.0, MathHelper.cos(yaw));
    }

    private static Vec3d rightFacing(float yawDegrees) {
        float yaw = yawDegrees * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(MathHelper.cos(yaw), 0.0, MathHelper.sin(yaw));
    }

    private static boolean isFirstPersonOwner(SoulstalkerStrideEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && entity.hasPassenger(client.player)
                && client.getCameraEntity() == client.player
                && client.options.getPerspective() == Perspective.FIRST_PERSON;
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static int transitionRank(int leg) {
        for (int index = 0; index < TRANSITION_ORDER.length; index++) {
            if (TRANSITION_ORDER[index] == leg) {
                return index;
            }
        }
        return leg;
    }

    private static final class RigState {
        private final LegState[] legs = new LegState[LEG_COUNT];
        private byte surfaceMode = Byte.MIN_VALUE;
        private int stepSequence = Integer.MIN_VALUE;
        private int commandedLeg = -1;
        private int leapSequence = Integer.MIN_VALUE;
        private float modeChangedAt;

        private void beginFrame(SoulstalkerStrideEntity entity, float time) {
            if (surfaceMode != entity.getSurfaceMode()) {
                surfaceMode = entity.getSurfaceMode();
                modeChangedAt = time;
                for (LegState leg : legs) {
                    if (leg != null) {
                        leg.pendingTransition = true;
                    }
                }
            }
            if (stepSequence != entity.getStepSequence()) {
                if (stepSequence != Integer.MIN_VALUE) {
                    commandedLeg = entity.getStepLeg();
                }
                stepSequence = entity.getStepSequence();
            }
            if (leapSequence != entity.getLeapSequence()) {
                if (leapSequence != Integer.MIN_VALUE) {
                    for (LegState leg : legs) {
                        if (leg != null) {
                            leg.pendingTransition = true;
                        }
                    }
                    modeChangedAt = time;
                }
                leapSequence = entity.getLeapSequence();
            }
        }

        private Vec3d update(int index, Vec3d desired, Vec3d normal, float time, byte mode) {
            LegState state = legs[index];
            if (state == null) {
                state = new LegState(desired, normal);
                legs[index] = state;
            }
            if (mode == SoulstalkerStrideEntity.SURFACE_AIRBORNE) {
                state.planted = state.planted.lerp(desired, 0.38);
                state.stepNormal = normal;
                state.stepping = false;
                state.pendingTransition = false;
                return state.planted;
            }
            if (commandedLeg == index) {
                state.begin(desired, normal, time);
                state.pendingTransition = false;
                commandedLeg = -1;
            } else if (state.pendingTransition
                    && time >= modeChangedAt + 10.0F + transitionRank(index) * 2.0F) {
                state.begin(desired, normal, time);
                state.pendingTransition = false;
            } else if (!state.stepping && state.planted.squaredDistanceTo(desired) > 12.0) {
                state.begin(desired, normal, time - 2.5F);
            }
            if (state.stepping) {
                float progress = MathHelper.clamp((time - state.stepStarted) / 5.0F, 0.0F, 1.0F);
                float eased = ease(progress);
                Vec3d point = state.stepStart.lerp(state.stepTarget, eased)
                        .add(state.stepNormal.multiply(Math.sin(progress * Math.PI)
                                * (mode == SoulstalkerStrideEntity.SURFACE_WALL ? 0.24 : 0.42)));
                if (progress >= 1.0F) {
                    state.planted = state.stepTarget;
                    state.stepping = false;
                    return state.planted;
                }
                return point;
            }
            return state.planted;
        }
    }

    private static final class LegState {
        private Vec3d planted;
        private Vec3d stepStart;
        private Vec3d stepTarget;
        private Vec3d stepNormal;
        private float stepStarted;
        private boolean stepping;
        private boolean pendingTransition;

        private LegState(Vec3d planted, Vec3d normal) {
            this.planted = planted;
            this.stepNormal = normal;
        }

        private void begin(Vec3d target, Vec3d normal, float time) {
            stepStart = planted;
            stepTarget = target;
            stepNormal = normal.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 1.0, 0.0) : normal.normalize();
            stepStarted = time;
            stepping = true;
        }
    }
}
