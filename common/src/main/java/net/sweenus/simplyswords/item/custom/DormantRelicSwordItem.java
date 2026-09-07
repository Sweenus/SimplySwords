package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.AwakeningFormRegistry;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.LongPathFinalFormsMasteryAbilities;
import net.sweenus.simplyswords.api.ability.LongPathFinalFormsMasteryTuning;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class DormantRelicSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public DormantRelicSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

	@Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient() && AwakeningApi.isAbilityUnlocked(stack)) {
            if (net.sweenus.simplyswords.world.LongPathFinalFormsMasteryCombatManager.isSunfire(stack)) {
                net.sweenus.simplyswords.world.LongPathFinalFormsMasteryCombatManager.sunfireMelee(stack, target, attacker);
            } else if (net.sweenus.simplyswords.world.LongPathFinalFormsMasteryCombatManager.isHarbinger(stack)) {
                net.sweenus.simplyswords.world.LongPathFinalFormsMasteryCombatManager.harbingerMelee(stack, target, attacker);
            } else if (isSunForm(stack)
                    && attacker.getRandom().nextInt(100) < AwakeningApi.scaleChance(stack, Config.uniqueEffects.sunfire.chance)) {
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                        attacker.getSoundCategory(), 0.3f, 1.7f);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40,
                        AwakeningApi.getLevel(stack) >= 8 ? 1 : 0), attacker);
            } else if (isHarbingerForm(stack)
                    && attacker.getRandom().nextInt(100) < AwakeningApi.scaleChance(stack, Config.uniqueEffects.harbinger.chance)) {
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                        attacker.getSoundCategory(), 0.3f, 1.6f);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 0), attacker);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        if (context == null || AwakeningApi.getLevel(context.stack()) < 8) {
            return false;
        }
        return (isSunForm(context.stack()) || isHarbingerForm(context.stack()))
                && context.world().getBlockState(getStandardPosition(context.actor())).isAir()
                && context.actor().isAlive()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        BlockPos pos = getStandardPosition(context.actor());
        if (!context.world().getBlockState(pos).isAir()) {
            return false;
        }
        if (isSunForm(context.stack())) {
            UniqueAbilityExecution execution = UniqueAbilityApi.begin(LongPathFinalFormsMasteryAbilities.SUNFIRE_STANDARD,
                    UniqueAbilityContext.active(context), tuning -> tuning
                            .set(LongPathFinalFormsMasteryAbilities.TUNING, LongPathFinalFormsMasteryTuning.EMPTY)
                            .set(LongPathFinalFormsMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.sunfire.cooldown));
            context.world().playSoundFromEntity(null, context.actor(),
                    SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
                    context.actor().getSoundCategory(), 0.4f, 0.8f);
            BattleStandardEntity standard = EntityRegistry.BATTLESTANDARD.get().spawn(
                    context.world(), pos, SpawnReason.MOB_SUMMONED);
            if (standard == null) {
                UniqueAbilityApi.cancel(execution);
                return false;
            }
            standard.setVelocity(0, -1, 0);
            standard.ownerEntity = context.actor();
            standard.decayRate = 3;
            standard.standardType = "sunfire";
            standard.spellScalingOwner = AwakeningApi.getFormId(context.stack())
                    .map(Identifier::getPath)
                    .orElse("sunfire");
            standard.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name",
                    context.actor().getName()));
            if (LongPathFinalFormsMasteryAbilities.tuning(execution).isEmpty()) UniqueAbilityApi.cancel(execution);
            else standard.configureMastery(execution, context.stack());
            return true;
        }
        if (net.sweenus.simplyswords.world.LongPathFinalFormsMasteryCombatManager.isHarbinger(context.stack())) {
            return HarbingerSwordItem.activateStandard(context);
        }
        if (isHarbingerForm(context.stack())) {
            context.world().playSoundFromEntity(null, context.actor(),
                    SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                    context.actor().getSoundCategory(), 0.4f, 0.8f);
            BattleStandardDarkEntity standard = EntityRegistry.BATTLESTANDARDDARK.get().spawn(
                    context.world(), pos, SpawnReason.MOB_SUMMONED);
            if (standard == null) return false;
            standard.setVelocity(0, -1, 0);
            standard.ownerEntity = context.actor();
            standard.decayRate = 3;
            standard.standardType = "harbinger";
            standard.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name",
                    context.actor().getName()));
            return true;
        }
        return false;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return isSunForm(stack)
                ? Config.uniqueEffects.sunfire.cooldown
                : Config.uniqueEffects.harbinger.cooldown;
    }

    private BlockPos getStandardPosition(LivingEntity user) {
        return user.getBlockPos().up(4).offset(user.getMovementDirection(), 3);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && entity instanceof LivingEntity living
                && net.sweenus.simplyswords.world.LongPathFinalFormsMasteryCombatManager.isSunfire(stack)
                && (living.getMainHandStack() == stack || living.getOffHandStack() == stack)) {
            net.sweenus.simplyswords.world.LongPathFinalFormsMasteryCombatManager.tickHeld(stack, living);
        }
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }
    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects");
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        int awakening = AwakeningApi.getLevel(itemStack);
        boolean sunForm = isSunForm(itemStack);
        boolean harbingerForm = isHarbingerForm(itemStack);
        if (awakening < 4 || (!sunForm && !harbingerForm)) {
            tooltip.add(Text.translatable("item.simplyswords.dormantrelicsworditem.tooltip2").setStyle(Styles.TEXT));
        } else {
            String path = sunForm ? "sunfire" : "harbinger";
            tooltip.add(Text.translatable("item.simplyswords." + path + "sworditem.tooltip1").setStyle(Styles.ABILITY));
            tooltip.add(Text.translatable("item.simplyswords." + path + "sworditem.tooltip2").setStyle(Styles.TEXT));
            if (awakening >= 8) {
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
                tooltip.add(Text.translatable("item.simplyswords." + path + "sworditem.tooltip3").setStyle(Styles.TEXT));
                appendAbilityCooldownTooltip(tooltip, itemStack, sunForm
                        ? Config.uniqueEffects.sunfire.cooldown
                        : Config.uniqueEffects.harbinger.cooldown);
                appendAbilityManaCostTooltip(tooltip, itemStack);
            }
        }
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        if (sunForm) {
            TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "healing_fire");
        } else if (harbingerForm) {
            TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "soul");
        }
        if (this.asItem().equals(ItemsRegistry.DECAYING_RELIC.get())) {
            if (Screen.hasAltDown()) {
                tooltip.add(Text.translatable("item.simplyswords.decayingrelicsworditem.tooltip1").formatted(Formatting.GRAY));
            }
        }
    }

    private static boolean isSunForm(ItemStack stack) {
        return AwakeningApi.getFormRoute(stack)
                .filter(AwakeningFormRegistry.SUN_ROUTE::equals)
                .isPresent();
    }

    private static boolean isHarbingerForm(ItemStack stack) {
        return AwakeningApi.getFormRoute(stack)
                .filter(AwakeningFormRegistry.HARBINGER_ROUTE::equals)
                .isPresent();
    }
}
