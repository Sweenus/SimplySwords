package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;
import java.util.Random;

public class MagiscytheSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public MagiscytheSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            ServerWorld world = (ServerWorld) attacker.getWorld();

            if (attacker.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISTORM))) {
                world.playSound(null, attacker.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                        attacker.getSoundCategory(), 0.1f, 1.9f);

                float repairChance = Config.uniqueEffects.magiscythe.repairChance;
                Random random = new Random();
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.getType() == EquipmentSlot.Type.ARMOR || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                        ItemStack item = attacker.getEquippedStack(slot);
                        if (!item.isEmpty() && random.nextFloat() < repairChance && item.getDamage() > 0) {
                            item.setDamage((int) (item.getDamage() - HelperMethods.getEntityAttackDamage(attacker)));
                            break;
                        }
                    }
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        activateMagistorm(world, user);
        SimplySwordsAPI.setWeaponCooldown(user, user.getStackInHand(hand), Config.uniqueEffects.magiscythe.cooldown);

        return super.use(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        activateMagistorm(context.world(), context.actor());
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.magiscythe.cooldown;
    }

    private void activateMagistorm(World world, LivingEntity user) {
        int baseEffectDuration = Config.uniqueEffects.magiscythe.duration;

        world.playSound(null, user.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_NORDIC_22.get(),
                user.getSoundCategory(), 0.2f, 1.1f);
        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.MAGISTORM), baseEffectDuration, 1));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT,
                ParticleTypes.ENCHANT, ParticleTypes.ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magiscythesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.magiscythesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.magiscythesworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magiscythesworditem.tooltip7").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.magiscythe.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "arcane");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MAGISCYTHE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 980;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.24f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 400;
        @ValidatedDouble.Restrict(min = 1f)
        public double radius = 4.0;
        @ValidatedFloat.Restrict(min = 0f, max = 1f)
        public float repairChance = 0.25f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.28f;

    }
}
