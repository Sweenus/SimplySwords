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
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
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
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.FlamewindVisualManager;

import java.util.Comparator;
import java.util.List;

public class FlamewindSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public FlamewindSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {

            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient() && world instanceof  ServerWorld serverWorld) {
            LivingEntity target = findFlamewindTarget(serverWorld, user);
            if (target != null && activateFlamewind(serverWorld, user, target)) {
                user.getItemCooldownManager().set(this, Config.uniqueEffects.flamewind.cooldown);
            }
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
                && getContextTarget(context) != null;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        LivingEntity target = getContextTarget(context);
        return target != null && activateFlamewind(context.world(), context.actor(), target);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.flamewind.cooldown;
    }

    private LivingEntity getContextTarget(WeaponAbilityContext context) {
        LivingEntity target = context.target();
        if (target != null
                && target.isAlive()
                && HelperMethods.checkAbilityTarget(target, context.actor())
                && !target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED))) {
            return target;
        }
        return findFlamewindTarget(context.world(), context.actor());
    }

    private LivingEntity findFlamewindTarget(World world, LivingEntity user) {
        Box box = HelperMethods.createBox(user, 10);
        Entity closestEntity = world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(entity -> entity instanceof LivingEntity le
                        && HelperMethods.checkAbilityTarget(le, user)
                        && !le.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED)))
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(user)))
                .orElse(null);
        return closestEntity instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    private boolean activateFlamewind(ServerWorld serverWorld, LivingEntity user, LivingEntity target) {
        int flameSeedDuration = 101;
        int flameSeedSpreadCap = Config.uniqueEffects.flamewind.spreadCap;

        SoundEvent soundSelect = SoundRegistry.SPELL_FIRE.get();
        int particleCount = 20;
        HelperMethods.spawnWaistHeightParticles(serverWorld, ParticleTypes.LAVA, user, target, particleCount);
        serverWorld.playSound(null, user.getBlockPos(), soundSelect, user.getSoundCategory(), 0.3f, 1.3f);

        SimplySwordsStatusEffectInstance flameSeedEffect = new SimplySwordsStatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.FLAMESEED), flameSeedDuration, 0, false,
                false, true);
        flameSeedEffect.setSourceEntity(user);
        flameSeedEffect.setAdditionalData(flameSeedSpreadCap);
        target.addStatusEffect(flameSeedEffect);
        FlamewindVisualManager.refreshSeed(serverWorld, target);
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FLAME,
                ParticleTypes.FLAME, ParticleTypes.ASH, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip11", Config.uniqueEffects.flamewind.spreadCap).setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.flamewind.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.FLAMEWIND::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 350;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.26f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.25f;
        @ValidatedFloat.Restrict(min = 0f)
        public float detonationDamageScaling = 0.45f;
        @ValidatedFloat.Restrict(min = 0f)
        public float detonationSpellScaling = 2.16f;
        @ValidatedInt.Restrict(min = 0)
        public int maxHaste = 10;
        @ValidatedFloat.Restrict(min = 0f)
        public float spreadDistance = 5f;
        @ValidatedInt.Restrict(min = 1)
        public int spreadCap = 6;

    }
}
