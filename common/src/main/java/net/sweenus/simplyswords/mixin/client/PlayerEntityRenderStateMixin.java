package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.sweenus.simplyswords.client.renderer.feature.ShoulderAxolotlRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements ShoulderAxolotlRenderStateAccess {
    @Unique
    private int simplyswords$leftShoulderAxolotlVariant = -1;

    @Unique
    private int simplyswords$rightShoulderAxolotlVariant = -1;

    @Override
    public int simplyswords$getLeftShoulderAxolotlVariant() {
        return simplyswords$leftShoulderAxolotlVariant;
    }

    @Override
    public int simplyswords$getRightShoulderAxolotlVariant() {
        return simplyswords$rightShoulderAxolotlVariant;
    }

    @Override
    public void simplyswords$setLeftShoulderAxolotlVariant(int variant) {
        this.simplyswords$leftShoulderAxolotlVariant = variant;
    }

    @Override
    public void simplyswords$setRightShoulderAxolotlVariant(int variant) {
        this.simplyswords$rightShoulderAxolotlVariant = variant;
    }
}
