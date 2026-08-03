package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.WaxweaverWaxVisualEntity;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;

public class WaxweaverWaxVisualEntityRenderer extends EntityRenderer<WaxweaverWaxVisualEntity> {

    private static final Identifier BONE_SPRITE = new Identifier("minecraft", "block/bone_block_side");
    private static final Identifier HONEY_SPRITE = new Identifier("minecraft", "block/honey_block_side");
    private static final Identifier MAGMA_SPRITE = new Identifier("minecraft", "block/magma");
    private static final int FULL_LIGHT = 0x00F000F0;
    private static final int SHELL_SEGMENTS = 12;
    private static final int SHELL_RINGS = 7;
    private static final float[] RING_HEIGHTS = {0.0F, 0.09F, 0.31F, 0.57F, 0.78F, 0.93F, 1.0F};
    private static final float[] RING_RADII = {1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};
    private static final float[] PLATE_STARTS = {0.8F, 1.7F, 3.0F, 4.4F, 6.0F, 7.5F};

    public WaxweaverWaxVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(WaxweaverWaxVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(WaxweaverWaxVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(Math.max(2.0F, entity.getRadius() + 1.0F)));
    }

    @Override
    public void render(WaxweaverWaxVisualEntity visual, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider providers, int light) {
        SpriteAtlasTexture atlas = MinecraftClient.getInstance().getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        Sprite bone = atlas.getSprite(BONE_SPRITE);
        Sprite honey = atlas.getSprite(HONEY_SPRITE);
        Sprite magma = atlas.getSprite(MAGMA_SPRITE);
        float time = visual.age + tickDelta;

        matrices.push();
        if (visual.getMode() == WaxweaverWaxVisualEntity.MODE_ENCASE) {
            renderEncasement(visual, matrices, providers, bone, honey, magma, time, tickDelta, light);
        } else {
            renderDetonation(visual, matrices, providers, bone, honey, magma, time, light);
        }
        matrices.pop();
        super.render(visual, yaw, tickDelta, matrices, providers, light);
    }

    private static void renderEncasement(WaxweaverWaxVisualEntity visual, MatrixStack matrices,
                                          VertexConsumerProvider providers, Sprite bone, Sprite honey,
                                          Sprite magma, float time, float tickDelta, int light) {
        Entity entity = visual.getWorld().getEntityById(visual.getTargetId());
        if (!(entity instanceof LivingEntity target)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (target == client.player && client.options.getPerspective().isFirstPerson()) return;

        Vec3d correction = lerped(target, tickDelta).subtract(lerped(visual, tickDelta));
        matrices.translate(correction.x, correction.y, correction.z);

        float fullHeight = Math.max(0.62F, target.getHeight() + 0.28F);
        float baseRadius = Math.max(0.58F, target.getWidth() * 0.72F + 0.23F);
        float formation = smooth(MathHelper.clamp(time / WaxweaverEncasementManager.FORMATION_TICKS, 0.0F, 1.0F));
        float shownHeight = Math.max(0.045F, fullHeight * formation);
        float remaining = visual.getLifetime() - time;
        float warning = smooth(MathHelper.clamp((20.0F - remaining) / 20.0F, 0.0F, 1.0F));
        float shudder = warning * warning * 0.025F;
        if (shudder > 0.0F) {
            matrices.translate(MathHelper.sin(time * 3.7F + visual.getSeed() * 0.013F) * shudder,
                    MathHelper.sin(time * 5.1F) * shudder * 0.35F,
                    MathHelper.cos(time * 4.3F + visual.getSeed() * 0.009F) * shudder);
        }

        int waxLight = Math.max(light, 0x00C000C0);
        VertexConsumer solid = providers.getBuffer(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        drawFootMound(solid, matrices.peek(), bone, visual.getSeed(), baseRadius,
                MathHelper.clamp(time / 2.8F, 0.0F, 1.0F), waxLight);
        drawCoreShell(solid, matrices.peek(), bone, visual.getSeed(), baseRadius,
                fullHeight, shownHeight, waxLight);
        drawLayeredPlates(solid, matrices.peek(), bone, visual.getSeed(), baseRadius,
                fullHeight, time, waxLight);

        float seal = smooth(MathHelper.clamp((time - 7.2F) / 2.8F, 0.0F, 1.0F));
        if (seal > 0.0F) {
            drawHardenedDrips(solid, matrices, bone, visual.getSeed(), baseRadius,
                    fullHeight, seal, waxLight);
            drawWick(solid, matrices, bone, fullHeight, seal, waxLight);

            VertexConsumer translucent = providers.getBuffer(RenderLayer.getTranslucentMovingBlock());
            drawSeams(translucent, matrices, honey, visual.getSeed(), baseRadius,
                    fullHeight, seal, warning, Math.max(light, 0x00D000D0), false);
            if (warning > 0.025F) {
                VertexConsumer emissive = providers.getBuffer(
                        RenderLayer.getEntityTranslucentEmissive(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
                drawSeams(emissive, matrices, magma, visual.getSeed(), baseRadius,
                        fullHeight, seal, warning, FULL_LIGHT, true);
            }
        }
    }

    private static void drawFootMound(VertexConsumer vertices, MatrixStack.Entry entry, Sprite sprite,
                                      int seed, float baseRadius, float progress, int light) {
        progress = smooth(progress);
        float radius = baseRadius * (1.12F + 0.34F * progress);
        float height = 0.045F + 0.105F * progress;
        float centerU = (sprite.getMinU() + sprite.getMaxU()) * 0.5F;
        float centerV = (sprite.getMinV() + sprite.getMaxV()) * 0.5F;
        for (int segment = 0; segment < SHELL_SEGMENTS; segment++) {
            int next = (segment + 1) % SHELL_SEGMENTS;
            float angle0 = MathHelper.TAU * segment / SHELL_SEGMENTS;
            float angle1 = MathHelper.TAU * next / SHELL_SEGMENTS;
            float r0 = radius * irregular(seed, -1, segment);
            float r1 = radius * irregular(seed, -1, next);
            float x0 = MathHelper.cos(angle0) * r0;
            float z0 = MathHelper.sin(angle0) * r0;
            float x1 = MathHelper.cos(angle1) * r1;
            float z1 = MathHelper.sin(angle1) * r1;
            float normal = (angle0 + angle1) * 0.5F;
            texturedQuad(vertices, entry,
                    x1, 0.0F, z1, x0, 0.0F, z0, x0, height, z0, x1, height, z1,
                    sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(),
                    light, 1.0F, 0.78F, 0.28F, 1.0F,
                    MathHelper.cos(normal), 0.0F, MathHelper.sin(normal));
            texturedQuad(vertices, entry,
                    0.0F, height, 0.0F, x1, height, z1, x0, height, z0, 0.0F, height, 0.0F,
                    centerU, centerV, sprite.getMaxU(), sprite.getMaxV(),
                    light, 1.0F, 0.85F, 0.43F, 1.0F, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void drawCoreShell(VertexConsumer vertices, MatrixStack.Entry entry, Sprite sprite,
                                      int seed, float baseRadius, float fullHeight,
                                      float shownHeight, int light) {
        for (int ring = 0; ring < SHELL_RINGS - 1; ring++) {
            float y0 = fullHeight * RING_HEIGHTS[ring];
            if (y0 >= shownHeight) break;
            float naturalY1 = fullHeight * RING_HEIGHTS[ring + 1];
            float y1 = Math.min(naturalY1, shownHeight);
            float partial = MathHelper.clamp((y1 - y0) / Math.max(0.001F, naturalY1 - y0), 0.0F, 1.0F);
            float upperFactor = MathHelper.lerp(partial, RING_RADII[ring], RING_RADII[ring + 1]);
            for (int segment = 0; segment < SHELL_SEGMENTS; segment++) {
                int next = (segment + 1) % SHELL_SEGMENTS;
                float angle0 = MathHelper.TAU * segment / SHELL_SEGMENTS;
                float angle1 = MathHelper.TAU * next / SHELL_SEGMENTS;
                float r00 = baseRadius * RING_RADII[ring] * verticalIrregular(seed, segment);
                float r01 = baseRadius * RING_RADII[ring] * verticalIrregular(seed, next);
                float r10 = baseRadius * upperFactor * verticalIrregular(seed, segment);
                float r11 = baseRadius * upperFactor * verticalIrregular(seed, next);
                float shade = 0.93F + Math.floorMod(seed + ring * 17 + segment * 11, 8) / 100.0F;
                drawShellPanel(vertices, entry, sprite, angle0, angle1,
                        r00, r01, r10, r11, y0, y1, light,
                        1.0F, 0.77F * shade, 0.31F * shade, 1.0F);
            }
            if (y1 >= shownHeight) {
                drawCap(vertices, entry, sprite, seed, ring + 1,
                        baseRadius * upperFactor, y1, light, 1.0F, 0.82F, 0.37F);
                break;
            }
        }
    }

    private static void drawLayeredPlates(VertexConsumer vertices, MatrixStack.Entry entry, Sprite sprite,
                                          int seed, float baseRadius, float fullHeight,
                                          float time, int light) {
        for (int ring = 0; ring < SHELL_RINGS - 1; ring++) {
            float y0 = fullHeight * RING_HEIGHTS[ring];
            float y1 = fullHeight * RING_HEIGHTS[ring + 1];
            for (int segment = 0; segment < SHELL_SEGMENTS; segment++) {
                float delay = PLATE_STARTS[ring] + ((segment + ring) & 1) * 0.28F;
                float progress = smooth(MathHelper.clamp((time - delay) / 2.15F, 0.0F, 1.0F));
                if (progress <= 0.0F) continue;

                int next = (segment + 1) % SHELL_SEGMENTS;
                float angle0 = MathHelper.TAU * segment / SHELL_SEGMENTS;
                float angle1 = MathHelper.TAU * next / SHELL_SEGMENTS;
                float overlap = 0.03F;
                float radialArrival = (1.0F - progress) * 0.22F;
                float lower = baseRadius * (RING_RADII[ring] + overlap + radialArrival);
                float upper = baseRadius * (RING_RADII[ring + 1] + overlap + radialArrival);
                float yShift = -(1.0F - progress) * (0.13F + ring * 0.012F);
                float r00 = lower * verticalIrregular(seed + 71, segment);
                float r01 = lower * verticalIrregular(seed + 71, next);
                float r10 = upper * verticalIrregular(seed + 71, segment);
                float r11 = upper * verticalIrregular(seed + 71, next);
                int tint = Math.floorMod(seed + segment * 23 + ring * 37, 4);
                float red = tint == 0 ? 1.0F : 0.96F;
                float green = switch (tint) {
                    case 0 -> 0.88F;
                    case 1 -> 0.72F;
                    case 2 -> 0.81F;
                    default -> 0.76F;
                };
                float blue = switch (tint) {
                    case 0 -> 0.48F;
                    case 1 -> 0.22F;
                    case 2 -> 0.34F;
                    default -> 0.28F;
                };
                drawShellPanel(vertices, entry, sprite, angle0, angle1,
                        r00, r01, r10, r11, y0 + yShift, y1 + yShift,
                        light, red, green, blue, 1.0F);
            }
        }
    }

    private static void drawShellPanel(VertexConsumer vertices, MatrixStack.Entry entry, Sprite sprite,
                                       float angle0, float angle1,
                                       float r00, float r01, float r10, float r11,
                                       float y0, float y1, int light,
                                       float red, float green, float blue, float alpha) {
        float x00 = MathHelper.cos(angle0) * r00;
        float z00 = MathHelper.sin(angle0) * r00;
        float x01 = MathHelper.cos(angle1) * r01;
        float z01 = MathHelper.sin(angle1) * r01;
        float x10 = MathHelper.cos(angle0) * r10;
        float z10 = MathHelper.sin(angle0) * r10;
        float x11 = MathHelper.cos(angle1) * r11;
        float z11 = MathHelper.sin(angle1) * r11;
        float normalAngle = (angle0 + angle1) * 0.5F;
        texturedQuad(vertices, entry,
                x01, y0, z01, x00, y0, z00, x10, y1, z10, x11, y1, z11,
                sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(),
                light, red, green, blue, alpha,
                MathHelper.cos(normalAngle), 0.04F, MathHelper.sin(normalAngle));
    }

    private static void drawCap(VertexConsumer vertices, MatrixStack.Entry entry, Sprite sprite,
                                int seed, int ring, float radius, float y, int light,
                                float red, float green, float blue) {
        float centerU = (sprite.getMinU() + sprite.getMaxU()) * 0.5F;
        float centerV = (sprite.getMinV() + sprite.getMaxV()) * 0.5F;
        for (int segment = 0; segment < SHELL_SEGMENTS; segment++) {
            int next = (segment + 1) % SHELL_SEGMENTS;
            float angle0 = MathHelper.TAU * segment / SHELL_SEGMENTS;
            float angle1 = MathHelper.TAU * next / SHELL_SEGMENTS;
            float r0 = radius * irregular(seed, ring, segment);
            float r1 = radius * irregular(seed, ring, next);
            texturedQuad(vertices, entry,
                    0.0F, y, 0.0F,
                    MathHelper.cos(angle1) * r1, y, MathHelper.sin(angle1) * r1,
                    MathHelper.cos(angle0) * r0, y, MathHelper.sin(angle0) * r0,
                    0.0F, y, 0.0F,
                    centerU, centerV, sprite.getMaxU(), sprite.getMaxV(),
                    light, red, green, blue, 1.0F, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void drawHardenedDrips(VertexConsumer vertices, MatrixStack matrices, Sprite sprite,
                                           int seed, float radius, float height,
                                           float progress, int light) {
        for (int drip = 0; drip < 7; drip++) {
            float angle = MathHelper.TAU * drip / 7.0F + seed * 0.0041F;
            float length = (0.2F + Math.floorMod(seed + drip * 31, 21) / 100.0F) * progress;
            float width = 0.08F + Math.floorMod(seed + drip * 17, 7) / 100.0F;
            float shoulderY = height * (0.67F + (drip % 3) * 0.045F);
            matrices.push();
            matrices.translate(MathHelper.cos(angle) * radius * 1.15F,
                    shoulderY - length * 0.5F, MathHelper.sin(angle) * radius * 1.15F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-angle));
            matrices.scale(width, length, width * 0.8F);
            drawTexturedCube(vertices, matrices.peek(), sprite, light,
                    0.96F, 0.7F, 0.22F, 1.0F);
            matrices.pop();
        }
    }

    private static void drawWick(VertexConsumer vertices, MatrixStack matrices, Sprite sprite,
                                 float height, float progress, int light) {
        matrices.push();
        matrices.translate(0.0F, height + 0.075F * progress, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-8.0F));
        matrices.scale(0.075F * progress, 0.19F * progress, 0.075F * progress);
        drawTexturedCube(vertices, matrices.peek(), sprite, light,
                0.13F, 0.09F, 0.055F, 1.0F);
        matrices.pop();
    }

    private static void drawSeams(VertexConsumer vertices, MatrixStack matrices, Sprite sprite,
                                  int seed, float radius, float height, float seal,
                                  float warning, int light, boolean heated) {
        for (int seam = 0; seam < 6; seam++) {
            float threshold = seam / 7.0F;
            float heat = heated ? smooth(MathHelper.clamp((warning - threshold) * 2.2F, 0.0F, 1.0F)) : 1.0F;
            if (heat <= 0.0F) continue;
            float angle = MathHelper.TAU * seam / 6.0F + seed * 0.0023F;
            float seamHeight = height * (0.49F + (seam % 2) * 0.18F);
            float seamY = height * (0.16F + (seam % 3) * 0.1F) + seamHeight * 0.5F;
            float shellRadius = radius * 1.04F;
            matrices.push();
            matrices.translate(MathHelper.cos(angle) * shellRadius, seamY,
                    MathHelper.sin(angle) * shellRadius);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-angle));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((seam % 2 == 0 ? 1.0F : -1.0F) * 7.0F));
            matrices.scale((heated ? 0.045F : 0.055F) * seal,
                    seamHeight * seal, (heated ? 0.04F : 0.05F) * seal);
            drawTexturedCube(vertices, matrices.peek(), sprite, light,
                    heated ? 1.0F : 1.0F,
                    heated ? 0.68F : 0.77F,
                    heated ? 0.18F : 0.28F,
                    heated ? heat : 0.78F);
            matrices.pop();
        }

        for (int band = 0; band < 3; band++) {
            float threshold = 0.14F + band * 0.22F;
            float heat = heated ? smooth(MathHelper.clamp((warning - threshold) * 2.4F, 0.0F, 1.0F)) : 1.0F;
            if (heat <= 0.0F) continue;
            float y = height * (0.31F + band * 0.235F);
            float profileRadius = radius * 1.04F;
            drawNarrowBand(vertices, matrices.peek(), sprite, seed + band * 53,
                    profileRadius, y, 0.025F + (heated ? 0.008F : 0.0F), light,
                    heated ? 1.0F : 1.0F,
                    heated ? 0.64F : 0.8F,
                    heated ? 0.15F : 0.32F,
                    heated ? heat : 0.74F);
        }
    }

    private static void drawNarrowBand(VertexConsumer vertices, MatrixStack.Entry entry, Sprite sprite,
                                       int seed, float radius, float y, float halfHeight,
                                       int light, float red, float green, float blue, float alpha) {
        for (int segment = 0; segment < SHELL_SEGMENTS; segment++) {
            int next = (segment + 1) % SHELL_SEGMENTS;
            float angle0 = MathHelper.TAU * segment / SHELL_SEGMENTS;
            float angle1 = MathHelper.TAU * next / SHELL_SEGMENTS;
            float r0 = radius * irregular(seed, 9, segment);
            float r1 = radius * irregular(seed, 9, next);
            float normal = (angle0 + angle1) * 0.5F;
            texturedQuadDoubleSided(vertices, entry,
                    MathHelper.cos(angle1) * r1, y - halfHeight, MathHelper.sin(angle1) * r1,
                    MathHelper.cos(angle0) * r0, y - halfHeight, MathHelper.sin(angle0) * r0,
                    MathHelper.cos(angle0) * r0, y + halfHeight, MathHelper.sin(angle0) * r0,
                    MathHelper.cos(angle1) * r1, y + halfHeight, MathHelper.sin(angle1) * r1,
                    sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(),
                    light, red, green, blue, alpha,
                    MathHelper.cos(normal), 0.0F, MathHelper.sin(normal));
        }
    }

    private static void renderDetonation(WaxweaverWaxVisualEntity visual, MatrixStack matrices,
                                         VertexConsumerProvider providers, Sprite bone,
                                         Sprite honey, Sprite magma, float time, int light) {
        // NeoForge may end a shared builder when another render layer is requested. Finish each
        // debris pass before acquiring the consumer for the next layer.
        VertexConsumer solid = providers.getBuffer(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        renderDebrisGroup(visual, matrices, solid, bone, time, Math.max(light, 0x00D000D0),
                0, 18, 1.0F, 0.78F, 0.28F, false);

        VertexConsumer translucent = providers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        renderDebrisGroup(visual, matrices, translucent, honey, time, Math.max(light, 0x00D000D0),
                18, 8, 1.0F, 0.72F, 0.2F, false);

        VertexConsumer emissive = providers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        renderDebrisGroup(visual, matrices, emissive, magma, time, FULL_LIGHT,
                26, 6, 1.0F, 0.64F, 0.12F, true);
    }

    private static void renderDebrisGroup(WaxweaverWaxVisualEntity visual, MatrixStack matrices,
                                          VertexConsumer vertices, Sprite sprite, float time, int light,
                                          int start, int count, float red, float green, float blue,
                                          boolean hot) {
        float radius = visual.getRadius();
        int total = 32;
        for (int offset = 0; offset < count; offset++) {
            int fragment = start + offset;
            float delay = fragment % 5 * 0.22F;
            float duration = 12.0F + fragment % 6;
            float progress = MathHelper.clamp((time - delay) / duration, 0.0F, 1.0F);
            if (progress <= 0.0F || progress >= 1.0F) continue;
            double angle = MathHelper.TAU * fragment / total + visual.getSeed() * 0.0067;
            double distance = radius * (0.42 + Math.floorMod(visual.getSeed() + fragment * 37, 48) / 100.0) * progress;
            double height = 0.14 + Math.sin(progress * MathHelper.PI) * (0.9 + fragment % 5 * 0.2);
            float fade = 1.0F - smooth(MathHelper.clamp((progress - 0.74F) / 0.26F, 0.0F, 1.0F));
            float size = (hot ? 0.105F : 0.145F) + fragment % 4 * 0.042F;

            matrices.push();
            matrices.translate(Math.cos(angle) * distance, height, Math.sin(angle) * distance);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(progress * (280.0F + fragment * 21.0F)));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(progress * (350.0F - fragment * 13.0F)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(progress * (210.0F + fragment * 17.0F)));
            float stretch = 0.7F + Math.floorMod(visual.getSeed() + fragment * 13, 52) / 100.0F;
            matrices.scale(size, size * stretch, size / stretch);
            drawTexturedCube(vertices, matrices.peek(), sprite, light,
                    red, green, blue, fade);
            matrices.pop();
        }
    }

    private static void drawTexturedCube(VertexConsumer vertices, MatrixStack.Entry entry,
                                         Sprite sprite, int light,
                                         float red, float green, float blue, float alpha) {
        float min = -0.5F;
        float max = 0.5F;
        texturedQuadDoubleSided(vertices, entry, min, min, max, max, min, max, max, max, max, min, max, max,
                sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), light, red, green, blue, alpha, 0, 0, 1);
        texturedQuadDoubleSided(vertices, entry, max, min, min, min, min, min, min, max, min, max, max, min,
                sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), light, red, green, blue, alpha, 0, 0, -1);
        texturedQuadDoubleSided(vertices, entry, max, min, max, max, min, min, max, max, min, max, max, max,
                sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), light, red, green, blue, alpha, 1, 0, 0);
        texturedQuadDoubleSided(vertices, entry, min, min, min, min, min, max, min, max, max, min, max, min,
                sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), light, red, green, blue, alpha, -1, 0, 0);
        texturedQuadDoubleSided(vertices, entry, min, max, max, max, max, max, max, max, min, min, max, min,
                sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), light, red, green, blue, alpha, 0, 1, 0);
        texturedQuadDoubleSided(vertices, entry, min, min, min, max, min, min, max, min, max, min, min, max,
                sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), light, red, green, blue, alpha, 0, -1, 0);
    }

    private static void texturedQuadDoubleSided(VertexConsumer vertices, MatrixStack.Entry entry,
                                                 float x0, float y0, float z0,
                                                 float x1, float y1, float z1,
                                                 float x2, float y2, float z2,
                                                 float x3, float y3, float z3,
                                                 float u0, float v0, float u1, float v1,
                                                 int light, float red, float green, float blue, float alpha,
                                                 float nx, float ny, float nz) {
        texturedQuad(vertices, entry, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
                u0, v0, u1, v1, light, red, green, blue, alpha, nx, ny, nz);
        texturedQuad(vertices, entry, x3, y3, z3, x2, y2, z2, x1, y1, z1, x0, y0, z0,
                u0, v0, u1, v1, light, red, green, blue, alpha, -nx, -ny, -nz);
    }

    private static void texturedQuad(VertexConsumer vertices, MatrixStack.Entry entry,
                                     float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float u0, float v0, float u1, float v1,
                                     int light, float red, float green, float blue, float alpha,
                                     float nx, float ny, float nz) {
        vertex(vertices, entry, x0, y0, z0, u0, v1, light, red, green, blue, alpha, nx, ny, nz);
        vertex(vertices, entry, x1, y1, z1, u1, v1, light, red, green, blue, alpha, nx, ny, nz);
        vertex(vertices, entry, x2, y2, z2, u1, v0, light, red, green, blue, alpha, nx, ny, nz);
        vertex(vertices, entry, x3, y3, z3, u0, v0, light, red, green, blue, alpha, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vertices, MatrixStack.Entry entry,
                               float x, float y, float z, float u, float v, int light,
                               float red, float green, float blue, float alpha,
                               float nx, float ny, float nz) {
        vertices.vertex(entry.getPositionMatrix(), x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz).next();
    }

    private static float irregular(int seed, int ring, int segment) {
        long value = MathHelper.hashCode(seed + ring * 41, segment * 29, ring * 17 - segment * 13);
        return 0.94F + Math.floorMod(value, 13L) / 100.0F;
    }

    private static float verticalIrregular(int seed, int segment) {
        long value = MathHelper.hashCode(seed, segment * 29, -segment * 13);
        return 0.96F + Math.floorMod(value, 9L) / 100.0F;
    }

    private static Vec3d lerped(Entity entity, float tickDelta) {
        return new Vec3d(MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }
}
