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
import net.sweenus.simplyswords.client.util.TooltipUtils;
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

public class EmberlashSwordItem extends UniqueSwordItem {
    public EmberlashSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    public static float tooltipEffectDamage = 0.20f;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();

            HelperMethods.playHitSounds(attacker, target);

            if (target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING))) {
                target.timeUntilRegen = 0;
                StatusEffectInstance smoulderingEffect = target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
                if (smoulderingEffect != null) {
                    DamageSource damageSource = attacker instanceof PlayerEntity player ? player.getDamageSources().playerAttack(player) : world.getDamageSources().generic();
                    float abilityDamage = Math.max(HelperMethods.commonSpellAttributeScaling(Config.uniqueEffects.smoulder.spellScaling, attacker, "fire"), (float) HelperMethods.getEntityAttackDamage(attacker));
                    float damageMultiplier = 0.15f * smoulderingEffect.getAmplifier();
                    target.damage(damageSource, abilityDamage * damageMultiplier);
                    HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.LAVA, 0.2, smoulderingEffect.getAmplifier());
                    world.playSound(target, target.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),
                            target.getSoundCategory(), 0.1f, 1.5f);
                }
            }
            int maximum_stacks = Config.uniqueEffects.smoulder.maxStacks;
            HelperMethods.incrementStatusEffect(target, EffectRegistry.getReference(EffectRegistry.SMOULDERING), 100, 1, maximum_stacks + 1);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.swingHand(hand);
        world.playSound(null, user.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),
                user.getSoundCategory(), 0.5f, 1.0f);

        user.setVelocity(user.getRotationVector().negate().multiply(+1.5));
        user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent user flying to the heavens
        user.velocityModified = true;
        user.heal(user.getMaxHealth() * Config.uniqueEffects.smoulder.heal / 100f);
        user.getItemCooldownManager().set(this, Config.uniqueEffects.smoulder.cooldown);

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_LAVA,
                ParticleTypes.SMOKE, ParticleTypes.SMOKE, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberlashsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.emberlashsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberlashsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.emberlashsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.emberlashsworditem.tooltip5", tooltipEffectDamage).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.emberlashsworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.emberlashsworditem.tooltip7", Config.uniqueEffects.smoulder.heal).setStyle(Styles.TEXT));
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.EMBERLASH::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 80;
        @ValidatedFloat.Restrict(min = 0f, max = 100f)
        public float heal = 15f;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 5;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.4f;

    }
}