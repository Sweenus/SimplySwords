package net.sweenus.simplyswords.client.renderer.feature;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.sweenus.simplyswords.entity.CaelestisBreachCreature;
import net.sweenus.simplyswords.client.renderer.LegacyRenderColor;

public class CaelestisGrowthFeatureRenderer<T extends LivingEntity & CaelestisBreachCreature,
        M extends EntityModel<T>> extends FeatureRenderer<T, M> {

    private static final Identifier WHITE =
            new Identifier("minecraft", "textures/misc/white.png");

    private final ModelPart growths;

    public CaelestisGrowthFeatureRenderer(FeatureRendererContext<T, M> context, Shape shape) {
        super(context);
        this.growths = createModel(shape).createModel().getChild("growths");
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       T entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        float pulse = 0.88F + 0.12F * MathHelper.sin(
                animationProgress * 0.34F + Math.floorMod(entity.getCorruptionSeed(), 17));
        this.growths.xScale = pulse;
        this.growths.yScale = 1.0F + (pulse - 1.0F) * 1.8F;
        this.growths.zScale = pulse;
        this.growths.roll = MathHelper.sin(
                animationProgress * 0.18F + Math.floorMod(entity.getCorruptionSeed(), 29)) * 0.035F;

        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE));
        int color = growthColor(entity);
        this.growths.render(
                matrices,
                vertices,
                0x00F000F0,
                OverlayTexture.DEFAULT_UV,
                LegacyRenderColor.red(color), LegacyRenderColor.green(color),
                LegacyRenderColor.blue(color), LegacyRenderColor.alpha(color)
        );
    }

    private static int growthColor(CaelestisBreachCreature creature) {
        int seed = creature.getCorruptionSeed();
        if (creature.isUnbound()) {
            return 0xE8FF0000
                    | (18 + Math.floorMod(seed >>> 3, 32)) << 8
                    | 28 + Math.floorMod(seed >>> 9, 36);
        }
        return 0xDC000000
                | (126 + Math.floorMod(seed, 54)) << 16
                | (18 + Math.floorMod(seed >>> 5, 26)) << 8
                | 184 + Math.floorMod(seed >>> 10, 64);
    }

    private static TexturedModelData createModel(Shape shape) {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartBuilder builder = ModelPartBuilder.create();
        if (shape == Shape.RIFTLING) {
            builder
                    .uv(0, 0).cuboid(-0.5F, 9.5F, 8.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
                    .uv(0, 0).cuboid(-3.5F, 11.0F, 9.5F, 1.0F, 5.0F, 1.0F, new Dilation(0.0F))
                    .uv(0, 0).cuboid(2.5F, 10.0F, 9.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
                    .uv(0, 0).cuboid(-2.5F, 12.0F, -7.0F, 1.0F, 3.0F, 1.0F, new Dilation(0.0F))
                    .uv(0, 0).cuboid(1.5F, 11.0F, -7.5F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F));
        } else {
            builder
                    .uv(0, 0).cuboid(-5.5F, -2.5F, 1.0F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
                    .uv(0, 0).cuboid(4.5F, -1.0F, 1.5F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
                    .uv(0, 0).cuboid(-2.5F, -10.0F, 0.5F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
                    .uv(0, 0).cuboid(1.5F, -11.0F, 1.0F, 1.0F, 5.0F, 1.0F, new Dilation(0.0F))
                    .uv(0, 0).cuboid(-0.5F, 2.0F, 2.5F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F));
        }
        root.addChild("growths", builder, ModelTransform.NONE);
        return TexturedModelData.of(data, 16, 16);
    }

    public enum Shape {
        RIFTLING,
        HOLLOW
    }
}
