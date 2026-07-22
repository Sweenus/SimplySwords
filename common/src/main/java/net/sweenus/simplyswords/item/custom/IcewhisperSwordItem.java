package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
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
import net.sweenus.simplyswords.world.FrostfallIceSpikeFieldManager;
import net.sweenus.simplyswords.world.IcewhisperCometManager;

import java.util.List;

public class IcewhisperSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public IcewhisperSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }

        if (world instanceof ServerWorld serverWorld) {
            activateIcewhisper(serverWorld, user, itemStack);
            user.getItemCooldownManager().set(itemStack.getItem(), Config.uniqueEffects.icewhisper.cooldown);
        }
        user.swingHand(hand);
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        activateIcewhisper(context.world(), context.actor(), context.stack());
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.icewhisper.cooldown;
    }

    private static void activateIcewhisper(ServerWorld serverWorld, LivingEntity actor, ItemStack stack) {
        int radius = Config.uniqueEffects.icewhisper.radius * 2;
        float abilityDamage = HelperMethods.abilityScaledDamage("frost", actor, stack,
                Config.uniqueEffects.icewhisper.damageScaling, Config.uniqueEffects.icewhisper.spellScaling);
        IcewhisperCometManager.startStorm(serverWorld, actor, stack, radius, abilityDamage * Config.uniqueEffects.icewhisper.cometDamageMultiplier, Config.uniqueEffects.icewhisper.duration);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && world instanceof ServerWorld serverWorld && entity instanceof LivingEntity user
                && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            tickPassiveAura(serverWorld, user, stack);
        }
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE,
                ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public static void tickPassiveAura(ServerWorld world, LivingEntity user, ItemStack stack) {
        if (world == null || user == null || stack == null || stack.isEmpty() || user.age % 35 != 0) {
            return;
        }
        int radius = Config.uniqueEffects.icewhisper.radius;
        Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                user.getX() - radius, user.getY() - radius, user.getZ() - radius);
        for (Entity otherEntity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if ((otherEntity instanceof LivingEntity le) && HelperMethods.checkAbilityTarget(le, user)) {
                StatusEffectInstance slowness = le.getStatusEffect(StatusEffects.SLOWNESS);
                if (slowness != null) {
                    int a = (slowness.getAmplifier() + 1);
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, Math.max(a, 3)), user);
                } else {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 0), user);
                }
                float choose = (float) (Math.random() * 1);
                world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(), le.getSoundCategory(), 0.1f, choose);
                float abilityDamage = HelperMethods.abilityScaledDamage("frost", user, stack,
                        Config.uniqueEffects.icewhisper.damageScaling, Config.uniqueEffects.icewhisper.spellScaling);
                var damageSource = user.getDamageSources().indirectMagic(user, user);
                le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, le, damageSource, abilityDamage));
                FrostfallIceSpikeFieldManager.createTargetBurst(world, le.getPos(), 4, 0.9F);
            }
        }
        world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_ICE_ATTACK_02.get(),
                user.getSoundCategory(), 0.1f, 0.6f);
        double xpos = user.getX() - (radius + 1);
        double ypos = user.getY();
        double zpos = user.getZ() - (radius + 1);

        for (int i = radius * 2; i > 0; i--) {
            for (int j = radius * 2; j > 0; j--) {
                float choose = (float) (Math.random() * 1);
                HelperMethods.spawnParticle(world, ParticleTypes.SNOWFLAKE,
                        xpos + i + choose, ypos + 0.4, zpos + j + choose,
                        0, 0.1, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.CLOUD,
                        xpos + i + choose, ypos + 0.1, zpos + j + choose,
                        0, 0, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.WHITE_ASH,
                        xpos + i + choose, ypos + 2, zpos + j + choose,
                        0, 0, 0);
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        double radius = Config.uniqueEffects.icewhisper.radius;
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip2", radius).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.icewhisper.cooldown);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "frost");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.ICEWHISPER::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 450;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.08f;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 200;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 4;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.9f;
        @ValidatedInt.Restrict(min = 1)
        public int cometInterval = 14;
        @ValidatedInt.Restrict(min = 0)
        public int cometsPerWave = 2;
        @ValidatedInt.Restrict(min = 1)
        public int cometFallTicks = 20;
        @ValidatedFloat.Restrict(min = 0f)
        public float cometSplashRadius = 2.5f;
        @ValidatedFloat.Restrict(min = 0f)
        public float cometDamageMultiplier = 16.0f;
    }
}
