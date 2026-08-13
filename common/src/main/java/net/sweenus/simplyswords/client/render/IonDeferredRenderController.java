package net.sweenus.simplyswords.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.Map;

/** Collects ion-field vertices during entity rendering and submits only those buffers late. */
@Environment(EnvType.CLIENT)
public final class IonDeferredRenderController {
    private static final int BUFFER_SIZE = 32 * 1024;
    private static final BufferBuilder FALLBACK = new BufferBuilder(BUFFER_SIZE);
    private static final Map<RenderLayer, BufferBuilder> LAYER_BUFFERS = new LinkedHashMap<>();
    private static final VertexConsumerProvider.Immediate CONSUMERS;
    private static final Matrix4f FRAME_MODEL_VIEW = new Matrix4f();
    private static boolean frameOpen;
    private static boolean pending;

    static {
        LAYER_BUFFERS.put(LightningRenderLayers.DEFERRED_ION_FIELD, new BufferBuilder(BUFFER_SIZE));
        LAYER_BUFFERS.put(LightningRenderLayers.DEFERRED_ION_LIGHTNING, new BufferBuilder(BUFFER_SIZE));
        LAYER_BUFFERS.put(LightningRenderLayers.DEFERRED_ION_DEPTH, new BufferBuilder(BUFFER_SIZE));
        CONSUMERS = VertexConsumerProvider.immediate(LAYER_BUFFERS, FALLBACK);
    }

    private IonDeferredRenderController() {
    }

    public static void beginFrame() {
        frameOpen = true;
        FRAME_MODEL_VIEW.set(RenderSystem.getModelViewStack().peek().getPositionMatrix());
    }

    public static VertexConsumer getFieldBuffer() {
        pending = true;
        return VertexConsumers.union(
                CONSUMERS.getBuffer(LightningRenderLayers.DEFERRED_ION_FIELD),
                CONSUMERS.getBuffer(LightningRenderLayers.DEFERRED_ION_DEPTH));
    }

    public static VertexConsumer getLightningBuffer() {
        pending = true;
        return CONSUMERS.getBuffer(LightningRenderLayers.DEFERRED_ION_LIGHTNING);
    }

    public static void drawDeferred() {
        if (!frameOpen || !pending || IrisCompat.isRenderingShadowPass()) return;
        MatrixStack modelView = RenderSystem.getModelViewStack();
        modelView.push();
        modelView.loadIdentity();
        modelView.multiplyPositionMatrix(FRAME_MODEL_VIEW);
        RenderSystem.applyModelViewMatrix();
        try {
            CONSUMERS.draw(LightningRenderLayers.DEFERRED_ION_FIELD);
            CONSUMERS.draw(LightningRenderLayers.DEFERRED_ION_LIGHTNING);
            CONSUMERS.draw(LightningRenderLayers.DEFERRED_ION_DEPTH);
        } finally {
            modelView.pop();
            RenderSystem.applyModelViewMatrix();
        }
        pending = false;
        frameOpen = false;
    }
}
