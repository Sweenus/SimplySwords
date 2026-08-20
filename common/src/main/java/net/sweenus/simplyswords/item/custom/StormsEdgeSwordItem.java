package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
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
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.StormsEdgeAbilityManager;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;

import java.util.List;

public class StormsEdgeSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public StormsEdgeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        HelperMethods.playHitSounds(attacker, target);
        if (attacker.getWorld().isClient()) {
            return super.postHit(stack, target, attacker);
        }

        int refreshChance = Math.clamp(Config.uniqueEffects.storms_edge.chance, 0, 100);
        if (refreshChance <= 0 || attacker.getRandom().nextInt(100) >= refreshChance) {
            return super.postHit(stack, target, attacker);
        }

        boolean refreshed = false;
        if (attacker instanceof PlayerEntity player && player.getItemCooldownManager().isCoolingDown(this)) {
            player.getItemCooldownManager().set(this, 0);
            refreshed = true;
        } else if (!(attacker instanceof PlayerEntity)
                && attacker.getWorld() instanceof ServerWorld serverWorld
                && WeaponAbilityCooldownManager.isCoolingDown(serverWorld, attacker, stack)) {
            WeaponAbilityCooldownManager.clearCooldown(attacker, stack);
            refreshed = true;
        }
        if (refreshed) {
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_BLOCK_01.get(),
                    attacker.getSoundCategory(), 0.7f, 1f);
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
                || !context.stack().isOf(ItemsRegistry.STORMS_EDGE.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || StormsEdgeAbilityManager.isDashing(context.actor())) {
            return false;
        }

        if (context.actor() instanceof PlayerEntity) {
            return true;
        }
        LivingEntity target = context.target();
        return target != null
                && target.isAlive()
                && HelperMethods.checkAbilityTarget(target, context.actor())
                && (context.sourcePlayer() == null
                || target != context.sourcePlayer()
                && HelperMethods.checkFriendlyFire(target, context.sourcePlayer()));
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return StormsEdgeAbilityManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.storms_edge.cooldown;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.storms_edge.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "lightning");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.STORMS_EDGE::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 100;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.4f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.89f;
        @ValidatedFloat.Restrict(min = 0f)
        public float thunderclapDamageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float thunderclapSpellScaling = 3.77f;
        @ValidatedDouble.Restrict(min = 0.1)
        public double dashDistance = 10.0;
        @ValidatedDouble.Restrict(min = 0.1)
        public double dashSpeed = 2.5;
        @ValidatedDouble.Restrict(min = 0.1)
        public double corridorWidth = 3.0;
        @ValidatedDouble.Restrict(min = 0.0)
        public double corridorKnockback = 0.8;
        @ValidatedDouble.Restrict(min = 0.0)
        public double corridorKnockUp = 0.15;
        @ValidatedDouble.Restrict(min = 0.1)
        public double thunderclapRadius = 3.5;
        @ValidatedDouble.Restrict(min = 0.0)
        public double thunderclapKnockback = 1.2;
        @ValidatedDouble.Restrict(min = 0.0)
        public double thunderclapKnockUp = 0.3;
    }
}
