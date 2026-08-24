package net.sweenus.simplyswords.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public final class LightningRenderLayers {

    private static final int BUFFER_SIZE = 1536;

    public static final RenderLayer LIGHTNING = RenderLayer.of(
            "simplyswords_lightning",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            BUFFER_SIZE,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false));

    public static final RenderLayer BLOCKY_LIGHTNING = RenderLayer.of(
            "simplyswords_blocky_lightning",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            BUFFER_SIZE,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false));

    public static final RenderLayer DEFERRED_ION_FIELD = RenderLayer.of(
            "simplyswords_deferred_ion_field",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            BUFFER_SIZE,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false));

    public static final RenderLayer DEFERRED_ION_LIGHTNING = RenderLayer.of(
            "simplyswords_deferred_ion_lightning",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            BUFFER_SIZE,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false));

    public static final RenderLayer DEFERRED_ION_DEPTH = RenderLayer.of(
            "simplyswords_deferred_ion_depth",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            BUFFER_SIZE,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .transparency(RenderPhase.NO_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.DEPTH_MASK)
                    .build(false));

    private LightningRenderLayers() {
    }
}
