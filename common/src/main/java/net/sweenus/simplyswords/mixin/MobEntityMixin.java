package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry;
import net.sweenus.simplyswords.api.AdditionalGemSocketApi;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SimplySwordsSkeletonMinionEntity;
import net.sweenus.simplyswords.entity.SimplySwordsWolfMinionEntity;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;
import net.sweenus.simplyswords.item.UniqueWeaponItem;
import net.sweenus.simplyswords.item.custom.IcewhisperSwordItem;
import net.sweenus.simplyswords.item.custom.LichbladeSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.MinionTargeting;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import net.sweenus.simplyswords.world.GloamMechanicsManager;
import net.sweenus.simplyswords.world.SoulkeeperLanternManager;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    @Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
    private void simplyswords$preventWaxEncasedAttack(net.minecraft.entity.Entity target,
                                                      CallbackInfoReturnable<Boolean> cir) {
        MobEntity mob = (MobEntity) (Object) this;
        if (WaxweaverEncasementManager.isEncased(mob)
                || IncapacitatingStatusEffectRegistry.isIncapacitated(mob)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tickNewAi", at = @At("HEAD"), cancellable = true)
    private void simplyswords$pauseIncapacitatedAi(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;
        if (GloamMechanicsManager.isGrasped(mob)) {
            mob.getNavigation().stop();
        }
        if (IncapacitatingStatusEffectRegistry.isIncapacitated(mob)) {
            mob.getNavigation().stop();
            ci.cancel();
        }
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void simplyswords$ignoreShadowDancingTargets(LivingEntity target, CallbackInfo ci) {
        if (target instanceof ServerPlayerEntity player && ShadowstingShadowDanceManager.isActive(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplyswords$tryUseHeldWeaponAbility(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;
        if (!Config.general.enableNonPlayerWeaponAbilityUse
                || mob.getWorld().isClient()
                || !(mob.getWorld() instanceof ServerWorld world)
                || !mob.isAlive()
                || WaxweaverEncasementManager.isEncased(mob)
                || IncapacitatingStatusEffectRegistry.isIncapacitated(mob)) {
            return;
        }

        ItemStack stack = mob.getMainHandStack();
        if (!stack.isEmpty()) {
            stack.inventoryTick(world, mob, 0, true);
        }
        if (AwakeningApi.isAbilityUnlocked(stack) && stack.isOf(ItemsRegistry.ICEWHISPER.get())) {
            IcewhisperSwordItem.tickPassiveAura(world, mob, stack);
        }
        if (AwakeningApi.isAbilityUnlocked(stack) && stack.isOf(ItemsRegistry.SOULKEEPER.get())) {
            SoulkeeperLanternManager.tickFromItem(mob, stack);
        }
        if (AwakeningApi.isAbilityUnlocked(stack) && stack.getItem() instanceof LichbladeSwordItem) {
            LichbladeSwordItem.tickPassiveAura(world, mob, stack);
        }
        if (mob instanceof SimplySwordsSkeletonMinionEntity || mob instanceof SimplySwordsWolfMinionEntity) {
            return;
        }

        int interval = Math.max(1, Config.general.nonPlayerWeaponAbilityCheckInterval);
        if ((mob.age + mob.getId()) % interval != 0) {
            return;
        }
        int chance = net.minecraft.util.math.MathHelper.clamp(Config.general.nonPlayerWeaponAbilityChance, 0, 100);
        if (chance <= 0 || mob.getRandom().nextInt(100) >= chance) {
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || !HelperMethods.checkAbilityTarget(target, mob)) {
            return;
        }
        WeaponAbilityContext context = WeaponAbilityContext.of(world, stack, mob, null, target, Hand.MAIN_HAND, WeaponAbilityActivationSource.MOB);
        if (SimplySwordsAPI.tryActivateWeaponAbility(context)) {
            mob.swingHand(Hand.MAIN_HAND);
        }
    }

    @Inject(method = "tryAttack", at = @At("RETURN"))
    private void simplyswords$applyHeldWeaponPostHit(net.minecraft.entity.Entity target, CallbackInfoReturnable<Boolean> cir) {
        MobEntity mob = (MobEntity) (Object) this;
        if (!cir.getReturnValue()
                || mob instanceof SimplySwordsSkeletonMinionEntity
                || mob instanceof SimplySwordsWolfMinionEntity
                || mob.getWorld().isClient()
                || !(target instanceof LivingEntity livingTarget)) {
            return;
        }

        ItemStack stack = mob.getMainHandStack();
        if (AdditionalGemSocketApi.ensureInitialized(stack)) {
            GemPowerComponent component = ComponentTypeRegistry.GEM_POWER.getOrDefault(stack, GemPowerComponent.DEFAULT);
            component.postHit(stack, livingTarget, mob);
            MinionTargeting.recordLastAttack(mob, livingTarget);
            return;
        }
        if (stack.getItem() instanceof UniqueWeaponItem
                || stack.getItem() instanceof RunicSwordItem
                || stack.getItem() instanceof SimplySwordsSwordItem) {
            SimplySwordsAPI.applyEntityWeaponPostHit(stack, livingTarget, mob, (float) net.sweenus.simplyswords.util.HelperMethods.getEntityAttackDamage(mob));
            MinionTargeting.recordLastAttack(mob, livingTarget);
        }
    }
}
