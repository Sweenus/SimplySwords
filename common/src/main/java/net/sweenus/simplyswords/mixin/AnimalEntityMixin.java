package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnimalEntity.class)
public class AnimalEntityMixin {

    @Inject(
            method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )

    public void simplyswords$breed(ServerWorld world, AnimalEntity other, CallbackInfo ci) {
        // Random chance to spawn a Chomp'olotl item instead of breeding
        if ((Object) this instanceof AxolotlEntity axolotlEntity) {
            float dropChance = Config.uniqueEffects.chompolotl.breedChance;
            if (world.random.nextFloat() < dropChance) {
                ItemStack chompolotlItem = new ItemStack(ItemsRegistry.CHOMPOLOTL.get());
                ItemEntity itemEntity = new ItemEntity(world, ((AxolotlEntity) (Object) this).getX(), ((AxolotlEntity) (Object) this).getY(), ((AxolotlEntity) (Object) this).getZ(), chompolotlItem);
                world.spawnEntity(itemEntity);
                axolotlEntity.setBreedingAge(6000);
                other.setBreedingAge(6000);
                axolotlEntity.resetLoveTicks();
                other.resetLoveTicks();

                // Cancel the default breeding action
                ci.cancel();
            }
        }
        if ((Object) this instanceof SimplySwordsAxolotlEntity)
            ci.cancel();
    }
}
