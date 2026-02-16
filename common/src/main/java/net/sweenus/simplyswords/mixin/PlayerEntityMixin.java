package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.ShoulderAxolotlData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow protected abstract void dropShoulderEntities();

    @Shadow @Final private PlayerAbilities abilities;

    @Unique
    public void simplySwords$invokeDropShoulderEntities() {
        dropShoulderEntities();
    }

    @Unique
    private boolean simplyswords$hasAnyShoulderAxolotl(PlayerEntity player) {
        return ShoulderAxolotlData.getLeftVariant(player).isPresent() || ShoulderAxolotlData.getRightVariant(player).isPresent();
    }

    @Unique
    private boolean simplyswords$shouldKeepShoulderAxolotls(PlayerEntity player) {
        if (!HelperMethods.hasItemInInventory(player, ItemsRegistry.CHOMPOLOTL.get())) return false;
        if (player.isTouchingWater() || player.isSneaking() || player.isSleeping()) return false;
        if (player.inPowderSnow || !player.isAlive()) return false;
        return !this.abilities.flying;
    }

    @Unique
    private void simplyswords$spawnShoulderAxolotl(ServerPlayerEntity player, boolean leftShoulder) {
        var variantOpt = leftShoulder ? ShoulderAxolotlData.getLeftVariant(player) : ShoulderAxolotlData.getRightVariant(player);
        if (variantOpt.isEmpty()) return;

        SimplySwordsAxolotlEntity axolotl = EntityRegistry.SIMPLYAXOLOTLENTITY.get().spawn(
                (ServerWorld) player.getEntityWorld(),
                player.getBlockPos().up(),
                SpawnReason.MOB_SUMMONED
        );

        if (axolotl != null) {
            NbtCompound data = new NbtCompound();
            data.putInt("Variant", variantOpt.getAsInt());
            axolotl.copyDataFromNbt(data);
            axolotl.setOwner(player);
            axolotl.setYaw(player.getYaw());
            axolotl.setPitch(0.0f);
            double xOffset = leftShoulder ? -0.5 : 0.5;
            axolotl.refreshPositionAndAngles(player.getX() + xOffset, player.getY() + 1.1, player.getZ(), player.getYaw(), 0.0f);
        }

        if (leftShoulder) {
            ShoulderAxolotlData.clearLeftVariant(player);
            player.setLeftShoulderParrotVariant(java.util.Optional.empty());
        } else {
            ShoulderAxolotlData.clearRightVariant(player);
            player.setRightShoulderParrotVariant(java.util.Optional.empty());
        }
    }

    @Unique
    private void simplyswords$dropSimplyAxolotls(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        simplyswords$spawnShoulderAxolotl(serverPlayer, true);
        simplyswords$spawnShoulderAxolotl(serverPlayer, false);
    }

    @Inject(at = @At("HEAD"), method = "dropShoulderEntities", cancellable = true)
    public void simplyswords$dropShoulderEntities(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (simplyswords$hasAnyShoulderAxolotl(player) && simplyswords$shouldKeepShoulderAxolotls(player)) {
            // Keep shoulder axolotls attached while their retention conditions are met.
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void simplyswords$tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.age % 40 == 0) {
            if (simplyswords$hasAnyShoulderAxolotl(player) && !simplyswords$shouldKeepShoulderAxolotls(player)) {
                simplyswords$dropSimplyAxolotls(player);
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "tickMovement")
    public void simplyswords$tickMovement(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getEntityWorld().isClient() && simplyswords$hasAnyShoulderAxolotl(player)
                && (player.isTouchingWater() || player.isSneaking())) {
            simplyswords$dropSimplyAxolotls(player);
        }
    }
}
