package net.sweenus.simplyswords.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import net.sweenus.simplyswords.SimplySwords;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class SimplySwordsMixin {
	//@Inject(at = @At("HEAD"), method = "init()V")
	//private void init(CallbackInfo info) {
		//SimplySwords.LOGGER.info("Loaded SimplySwords Mixin");
	//}
}

/*
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	@Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true)
	public void swingHand(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		ItemStack item = entity.getStackInHand(hand);
		if (item == ModItems.TWILIGHT.getDefaultStack()) {

			entity.setVelocity(entity.getRotationVector().multiply(+1));
			entity.velocityModified = true;
			entity.setOnFireFor(5);

		}
		entity.setVelocity(entity.getRotationVector().multiply(+1));
		entity.velocityModified = true;
		entity.setOnFireFor(5);

	}
}
*/