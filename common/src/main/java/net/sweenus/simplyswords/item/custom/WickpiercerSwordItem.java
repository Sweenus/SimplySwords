package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
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
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class WickpiercerSwordItem extends UniqueSwordItem {
    public WickpiercerSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            float damageModifier = Config.uniqueEffects.wickpiercer.damage;
            HelperMethods.playHitSounds(attacker, target);

            ServerWorld world = (ServerWorld) attacker.getWorld();
            DamageSource damageSource = world.getDamageSources().generic();
            if (attacker instanceof PlayerEntity player)
                damageSource = attacker.getDamageSources().playerAttack(player);

            if (attacker.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FRENZY))) {
                target.timeUntilRegen = 0;
                target.damage(damageSource, (float) (HelperMethods.getEntityAttackDamage(attacker) * damageModifier));
                world.playSound(null, attacker.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),
                        attacker.getSoundCategory(), 0.2f, 1.9f);
            }

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        int skillCooldown = Config.uniqueEffects.wickpiercer.cooldown;
        int baseEffectDuration = Config.uniqueEffects.wickpiercer.duration;
        int effectDuration = baseEffectDuration;
        ItemStack mainhand = user.getMainHandStack();
        ItemStack offhand  = user.getOffHandStack();
        if (mainhand.isOf(this) && offhand.isOf(this))
            effectDuration = baseEffectDuration * 2;

        world.playSound(null, user.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),
                user.getSoundCategory(), 0.5f, 1.0f);

        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FRENZY), effectDuration, 0));
        user.getItemCooldownManager().set(this, skillCooldown);

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
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip8").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip9", Config.uniqueEffects.waxweaver.cooldown / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip6", Config.uniqueEffects.wickpiercer.duration / 20).setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WICKPIERCER::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 220;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 1.0f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 40;

    }
}