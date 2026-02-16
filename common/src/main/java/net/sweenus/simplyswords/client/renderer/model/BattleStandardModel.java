package net.sweenus.simplyswords.client.renderer.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;

public class BattleStandardModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart supports;
	private final ModelPart bb_main;
	public BattleStandardModel(ModelPart root) {
		super(root);
		this.supports = root.getChild("supports");
		this.bb_main = root.getChild("bb_main");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData supports = modelPartData.addChild("supports", ModelPartBuilder.create().uv(0, 17).cuboid(0.0F, -30.0F, 0.0F, 1.0F, 21.0F, 1.0F, new Dilation(0.0F))
				.uv(0, 17).cuboid(0.0F, -9.0F, 0.0F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F))
				.uv(14, 17).cuboid(-4.0F, -28.0F, 0.0F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(18, 2).cuboid(-2.0F, -22.0F, 0.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(18, 0).cuboid(1.0F, -22.0F, 0.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(4, 17).cuboid(1.0F, -28.0F, 0.0F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.origin(0.0F, 24.0F, 0.0F));

		ModelPartData bb_main = modelPartData.addChild("bb_main", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -29.75F, -0.25F, 9.0F, 17.0F, 0.0F, new Dilation(0.0F)), ModelTransform.origin(0.0F, 24.0F, 0.0F));
		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(LivingEntityRenderState state) {
		super.setAngles(state);
	}
}
