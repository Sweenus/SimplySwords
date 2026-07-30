package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.ThunderbrandAbilityManager;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;

import java.util.List;

public class ThunderbrandSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {

    public ThunderbrandSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient()) {
            int chargeChance = Config.uniqueEffects.thunderbrand.chance;
            if (attacker.getRandom().nextInt(100) <= chargeChance && (attacker instanceof PlayerEntity player) && player.getItemCooldownManager().getCooldownProgress(this, 1f) > 0) {
                player.getItemCooldownManager().set(this, 0);
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_BLOCK_01.get(),
                        attacker.getSoundCategory(), 0.7f, 1f);
            } else if (attacker.getRandom().nextInt(100) <= chargeChance
                    && attacker.getWorld() instanceof ServerWorld
                    && !(attacker instanceof PlayerEntity)) {
                WeaponAbilityCooldownManager.clearCooldown(attacker, stack);
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_BLOCK_01.get(),
                        attacker.getSoundCategory(), 0.7f, 1f);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || ThunderbrandAbilityManager.isActive(context.actor())) {
            return false;
        }
        if (context.target() == null) {
            return context.actor() instanceof PlayerEntity;
        }
        if (!HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        return context.sourcePlayer() == null
                || context.target() != context.sourcePlayer()
                && HelperMethods.checkFriendlyFire(context.target(), context.sourcePlayer());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return ThunderbrandAbilityManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.thunderbrand.cooldown;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        TypedActionResult<ItemStack> result = UniqueWeaponActiveAbility.super.startPlayerAbility(world, user, hand);
        if (result.getResult().isAccepted()) {
            user.setCurrentHand(hand);
        }
        return result;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        // The shared server-side manager advances both player and non-player activations.
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient) {
            ThunderbrandAbilityManager.cancelCharging(user);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return Math.max(1, Config.uniqueEffects.thunderbrand.chargeDuration)
                + Math.max(1, Config.uniqueEffects.thunderbrand.dashDuration)
                + 5;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip5").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.thunderbrand.cooldown);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "lightning");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.THUNDERBRAND::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 250;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.18f;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 2;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.36f;
        @ValidatedInt.Restrict(min = 1)
        public int chargeDuration = 40;
        @ValidatedInt.Restrict(min = 1)
        public int dashDuration = 15;
        @ValidatedFloat.Restrict(min = 0.1f)
        public float dashSpeed = 4.0f;
        @ValidatedInt.Restrict(min = 1)
        public int chainTargets = 4;
        @ValidatedFloat.Restrict(min = 0.5f)
        public float chainRange = 6.0f;
    }
}
