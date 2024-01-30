package net.sweenus.simplyswords.fabric.mixin;


import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.sweenus.simplyswords.fabric.compat.eldritch_end.EldritchEndMethods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(at = @At("HEAD"), method = "tick")
    public void simplyswords$tick(CallbackInfo ci) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (FabricLoader.getInstance().isModLoaded("eldritch_end")) {
            EldritchEndMethods.generateVoidcloakStacks(livingEntity);
        }

    }

}