package net.sweenus.simplyswords.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.util.BufferAllocator;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

/** Collects ion-field vertices during entity rendering and submits only those buffers late. */
@Environment(EnvType.CLIENT)
public final class IonDeferredRenderController {
    private static final int BUFFER_SIZE = 32 * 1024;
    private static final BufferAllocator FALLBACK = new BufferAllocator(BUFFER_SIZE);
    private static final SequencedMap<RenderLayer, BufferAllocator> LAYER_BUFFERS = new LinkedHashMap<>();
    private static final VertexConsumerProvider.Immediate CONSUMERS;
    private static boolean frameOpen;
    private static boolean pending;

    static {
        LAYER_BUFFERS.put(LightningRenderLayers.DEFERRED_ION_FIELD, new BufferAllocator(BUFFER_SIZE));
        LAYER_BUFFERS.put(LightningRenderLayers.DEFERRED_ION_LIGHTNING, new BufferAllocator(BUFFER_SIZE));
        LAYER_BUFFERS.put(LightningRenderLayers.DEFERRED_ION_DEPTH, new BufferAllocator(BUFFER_SIZE));
        CONSUMERS = VertexConsumerProvider.immediate(LAYER_BUFFERS, FALLBACK);
    }

    private IonDeferredRenderController() {
    }

    public static void beginFrame() {
        frameOpen = true;
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
        CONSUMERS.draw(LightningRenderLayers.DEFERRED_ION_FIELD);
        CONSUMERS.draw(LightningRenderLayers.DEFERRED_ION_LIGHTNING);
        CONSUMERS.draw(LightningRenderLayers.DEFERRED_ION_DEPTH);
        pending = false;
        frameOpen = false;
    }
}
