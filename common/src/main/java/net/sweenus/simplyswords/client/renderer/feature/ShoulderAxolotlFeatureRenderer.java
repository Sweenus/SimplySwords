package net.sweenus.simplyswords.client.renderer.feature;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.AxolotlEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.AxolotlEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.util.ShoulderAxolotlData;

public class ShoulderAxolotlFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private static final Identifier WILD_AXOLOTL_TEXTURE = Identifier.of("minecraft", "textures/entity/axolotl/axolotl_wild.png");
    private static final Identifier LUCY_AXOLOTL_TEXTURE = Identifier.of("minecraft", "textures/entity/axolotl/axolotl_lucy.png");
    private static final Identifier GOLD_AXOLOTL_TEXTURE = Identifier.of("minecraft", "textures/entity/axolotl/axolotl_gold.png");
    private static final Identifier CYAN_AXOLOTL_TEXTURE = Identifier.of("minecraft", "textures/entity/axolotl/axolotl_cyan.png");
    private static final Identifier BLUE_AXOLOTL_TEXTURE = Identifier.of("minecraft", "textures/entity/axolotl/axolotl_blue.png");

    private final AxolotlEntityModel axolotlModel;
    private final AxolotlEntityRenderState axolotlRenderState = new AxolotlEntityRenderState();

    public ShoulderAxolotlFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context, ModelPart axolotlModel) {
        super(context);
        this.axolotlModel = new AxolotlEntityModel(axolotlModel);
    }


    @Override
    public void render(MatrixStack matrixStack, OrderedRenderCommandQueue renderCommandQueue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        ShoulderAxolotlRenderStateAccess access = (ShoulderAxolotlRenderStateAccess) state;
        int leftVariant = access.simplyswords$getLeftShoulderAxolotlVariant();
        int rightVariant = access.simplyswords$getRightShoulderAxolotlVariant();

        if (leftVariant >= 0) {
            renderAxolotlOnShoulder(matrixStack, renderCommandQueue, light, state, leftVariant, true);
        }
        if (rightVariant >= 0) {
            renderAxolotlOnShoulder(matrixStack, renderCommandQueue, light, state, rightVariant, false);
        }
    }

    private void renderAxolotlOnShoulder(MatrixStack matrixStack, OrderedRenderCommandQueue renderCommandQueue, int light,
                                         PlayerEntityRenderState state, int variantId, boolean leftShoulder) {
        matrixStack.push();
        matrixStack.translate(leftShoulder ? 0.4F : -0.4F, state.isInSneakingPose ? -1.3F : -1.5F, 0.0F);

        axolotlRenderState.variant = ShoulderAxolotlData.toAxolotlVariant(variantId);
        axolotlRenderState.age = state.age;
        axolotlRenderState.limbSwingAnimationProgress = state.limbSwingAnimationProgress;
        axolotlRenderState.limbSwingAmplitude = state.limbSwingAmplitude;
        axolotlRenderState.onGroundValue = 1.0f;
        axolotlRenderState.inWaterValue = 0.0f;
        axolotlRenderState.isMovingValue = 0.0f;
        axolotlRenderState.playingDeadValue = 0.0f;

        renderCommandQueue.submitModel(
                axolotlModel,
                axolotlRenderState,
                matrixStack,
                axolotlModel.getLayer(getAxolotlTexture(axolotlRenderState.variant)),
                light,
                OverlayTexture.DEFAULT_UV,
                state.outlineColor,
                null
        );

        matrixStack.pop();
    }

    private Identifier getAxolotlTexture(AxolotlEntity.Variant variant) {
        return switch (variant) {
            case LUCY -> LUCY_AXOLOTL_TEXTURE;
            case WILD -> WILD_AXOLOTL_TEXTURE;
            case GOLD -> GOLD_AXOLOTL_TEXTURE;
            case CYAN -> CYAN_AXOLOTL_TEXTURE;
            case BLUE -> BLUE_AXOLOTL_TEXTURE;
            default -> WILD_AXOLOTL_TEXTURE;
        };
    }
}
