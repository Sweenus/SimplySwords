package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.SoulrenderMarkVisualManager;

import java.util.List;

public class SoulrenderSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public SoulrenderSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            int hitChance = Config.uniqueEffects.soulrender.chance;
            int duration = Config.uniqueEffects.soulrender.duration;
            int maxStacks = Config.uniqueEffects.soulrender.maxStacks;
            ParticleEffect particleSelect  = ParticleTypes.ASH;
            int particleCount = 8; // Number of particles along the line

            if (attacker.getRandom().nextInt(100) <= hitChance) {
                particleSelect  = ParticleTypes.SMOKE;
                HelperMethods.spawnOrbitParticles(world, target.getPos(), particleSelect, 0.5f, particleCount);
                HelperMethods.spawnOrbitParticles(world, target.getPos().add(0,0.2,0), ParticleTypes.SOUL, 0.4f, 5);

                int choose_sound = (int) (Math.random() * 30);
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_01.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);
                else if (choose_sound <= 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);
                else if (choose_sound <= 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_03.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);

                StatusEffectInstance weakness = target.getStatusEffect(StatusEffects.WEAKNESS);
                if (weakness != null) {
                    int a = (weakness.getAmplifier() + 1);

                    if ((weakness.getAmplifier() <= 0)) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, a), attacker);
                    }
                } else {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0), attacker);
                }

                StatusEffectInstance slowness = target.getStatusEffect(StatusEffects.SLOWNESS);
                if (slowness != null) {
                    int a = (slowness.getAmplifier() + 1);

                    if ((slowness.getAmplifier() < maxStacks)) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, a), attacker);
                    }
                } else {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 0), attacker);
                }

                SoulrenderMarkVisualManager.refreshMark(world, target, duration);
            }
            HelperMethods.spawnWaistHeightParticles(world, particleSelect, attacker, target, particleCount);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            consumeSoulrenderMarks((ServerWorld) user.getWorld(), user, user.getStackInHand(hand));
        }
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
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && hasSoulrenderMarks(context.world(), context.actor());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return consumeSoulrenderMarks(context.world(), context.actor(), context.stack()) > 0;
    }

    private boolean hasSoulrenderMarks(ServerWorld world, LivingEntity user) {
        double hradius = Config.uniqueEffects.soulrender.radius;
        double vradius = Config.uniqueEffects.soulrender.radius / 2.0;
        Box box = new Box(user.getX() + hradius, user.getY() + vradius, user.getZ() + hradius,
                user.getX() - hradius, user.getY() - vradius, user.getZ() - hradius);
        return world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .anyMatch(entity -> entity instanceof LivingEntity le
                        && HelperMethods.checkAbilityTarget(le, user)
                        && le.hasStatusEffect(StatusEffects.SLOWNESS)
                        && le.hasStatusEffect(StatusEffects.WEAKNESS));
    }

    private int consumeSoulrenderMarks(ServerWorld world, LivingEntity user, ItemStack stack) {
        float healAmount = Config.uniqueEffects.soulrender.healMulti;
        int healAmp = 0;
        int consumed = 0;
        double hradius = Config.uniqueEffects.soulrender.radius;
        double vradius = Config.uniqueEffects.soulrender.radius / 2.0;
        Box box = new Box(user.getX() + hradius, user.getY() + vradius, user.getZ() + hradius,
                user.getX() - hradius, user.getY() - vradius, user.getZ() - hradius);

        for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if ((entity instanceof LivingEntity le) && HelperMethods.checkAbilityTarget(le, user)) {
                StatusEffectInstance slowness = le.getStatusEffect(StatusEffects.SLOWNESS);
                StatusEffectInstance weakness = le.getStatusEffect(StatusEffects.WEAKNESS);
                if (slowness == null || weakness == null) {
                    continue;
                }

                healAmp += slowness.getAmplifier();
                float damage = HelperMethods.abilityScaledDamage("soul", user, stack,
                        Config.uniqueEffects.soulrender.damageScaling, Config.uniqueEffects.soulrender.spellScaling);
                SoulrenderMarkVisualManager.consumeMark(world, le, user);
                var damageSource = user.getDamageSources().indirectMagic(user, user);
                le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, le, damageSource, slowness.getAmplifier() * damage));
                le.removeStatusEffect(StatusEffects.WEAKNESS);
                le.removeStatusEffect(StatusEffects.SLOWNESS);
                world.playSoundFromEntity(null, entity, SoundRegistry.DARK_SWORD_SPELL.get(),
                        entity.getSoundCategory(), 0.1f, 2f);
                consumed++;
            }
        }
        if (healAmp > 0) {
            float heal = (float) healAmp * healAmount;
            if (heal < 1f) heal = 1f;
            else if (heal > 6f) heal = 6f;
            user.heal(heal);
        }
        return consumed;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SOUL, ParticleTypes.SCULK_SOUL,
                ParticleTypes.WARPED_SPORE, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip4").setStyle(Styles.TEXT));
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "soul");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULRENDER::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 85;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 500;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 8;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 10.0;

        @ValidatedFloat.Restrict(min = 0f)
        public float healMulti = 0.5f;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.33f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.4f;
    }
}
