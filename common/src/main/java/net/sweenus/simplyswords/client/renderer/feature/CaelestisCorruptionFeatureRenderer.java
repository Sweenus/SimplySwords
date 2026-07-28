package net.sweenus.simplyswords.client.renderer.feature;

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
import net.sweenus.simplyswords.entity.CaelestisBreachCreature;

import java.util.function.Function;

public class CaelestisCorruptionFeatureRenderer<T extends LivingEntity, M extends EntityModel<T>>
        extends FeatureRenderer<T, M> {

    private final Function<T, Identifier> textureSelector;

    public CaelestisCorruptionFeatureRenderer(FeatureRendererContext<T, M> context,
                                              Function<T, Identifier> textureSelector) {
        super(context);
        this.textureSelector = textureSelector;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       T entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (!(entity instanceof CaelestisBreachCreature creature)) {
            return;
        }

        Identifier texture = this.textureSelector.apply(entity);
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(texture));
        int color = colorFor(creature);
        this.getContextModel().render(
                matrices,
                vertices,
                0x00F000F0,
                OverlayTexture.DEFAULT_UV,
                color
        );
    }

    public static int colorFor(CaelestisBreachCreature creature) {
        int seed = creature.getCorruptionSeed();
        if (creature.isUnbound()) {
            int red = 178 + Math.floorMod(seed, 46);
            int green = 12 + Math.floorMod(seed >>> 5, 18);
            int blue = 36 + Math.floorMod(seed >>> 9, 36);
            return 0xFF000000 | red << 16 | green << 8 | blue;
        } else {
            int red = 54 + Math.floorMod(seed, 34);
            int green = 8 + Math.floorMod(seed >>> 5, 18);
            int blue = 78 + Math.floorMod(seed >>> 9, 52);
            return 0xFF000000 | red << 16 | green << 8 | blue;
        }
    }
}
