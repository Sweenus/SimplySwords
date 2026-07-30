package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.RevivalWeapon;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.RevivalCandleVisualManager;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;

import java.util.List;

public class WaxweaverSwordItem extends UniqueSwordItem implements RevivalWeapon {
    public WaxweaverSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            int maximum_stacks = Config.uniqueEffects.waxweaver.maxStacks;
            HelperMethods.playHitSounds(attacker, target);

            if (target.isOnFire()) {
                HelperMethods.incrementStatusEffect(attacker, StatusEffects.STRENGTH, 60, 1, maximum_stacks + 1);
                HelperMethods.incrementStatusEffect(attacker, StatusEffects.HASTE, 60, 1, maximum_stacks + 1);
            }

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean canRevive(LivingEntity entity, ItemStack stack, DamageSource source) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (entity instanceof PlayerEntity player) {
            return !player.getItemCooldownManager().isCoolingDown(this);
        }
        return entity.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld
                && !WeaponAbilityCooldownManager.isCoolingDown(serverWorld, entity, stack);
    }

    @Override
    public void postRevive(LivingEntity entity, ItemStack stack, DamageSource source) {
        int skillCooldown = Config.uniqueEffects.waxweaver.cooldown;
        if (entity instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            RevivalCandleVisualManager.activate(serverPlayer, stack);
            serverPlayer.getItemCooldownManager().set(stack.getItem(), skillCooldown);
        } else if (entity.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            WeaponAbilityCooldownManager.setCooldown(serverWorld, entity, stack, skillCooldown);
        }
        HelperMethods.incrementStatusEffect(entity, StatusEffects.RESISTANCE, 100, 2, 3);

        World world = entity.getWorld();
        world.playSound(null, entity.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                entity.getSoundCategory(), 0.7f, 1.0f);
        world.playSound(null, entity.getBlockPos(), SoundRegistry.SPELL_MISC_02.get(),
                entity.getSoundCategory(), 0.8f, 1.0f);
    }

    @Override
    public float getReviveHealth(LivingEntity entity, ItemStack stack, DamageSource source) {
        return entity.getMaxHealth();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.WHITE_ASH,
                ParticleTypes.WHITE_ASH, ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.waxweaver.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WAXWEAVER::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 1200;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 3;

    }
}
