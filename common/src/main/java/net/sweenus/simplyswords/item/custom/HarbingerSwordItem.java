package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class HarbingerSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public HarbingerSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

	@Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient() && attacker.getRandom().nextInt(100) <= Config.uniqueEffects.harbinger.chance) {
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.3f, 1.6f);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 0), attacker);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            if (spawnHarbingerStandard((ServerWorld) user.getWorld(), user) != null) {
                SimplySwordsAPI.setWeaponCooldown(user, user.getStackInHand(hand), Config.uniqueEffects.harbinger.cooldown);
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
                && context.world().getBlockState(getStandardPosition(context.actor())).isAir();
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return spawnHarbingerStandard(context.world(), context.actor()) != null;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.harbinger.cooldown;
    }

    private BlockPos getStandardPosition(LivingEntity user) {
        return user.getBlockPos().up(4).offset(user.getMovementDirection(), 3);
    }

    private BattleStandardDarkEntity spawnHarbingerStandard(ServerWorld world, LivingEntity user) {
        BlockPos pos = getStandardPosition(user);
        if (!world.getBlockState(pos).isAir()) {
            return null;
        }
        world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                user.getSoundCategory(), 0.4f, 0.8f);
        BattleStandardDarkEntity banner = EntityRegistry.BATTLESTANDARDDARK.get().spawn(
                world,
                pos,
                SpawnReason.MOB_SUMMONED);
        if (banner != null) {
            banner.setVelocity(0, -1, 0);
            banner.ownerEntity = user;
            banner.decayRate = 3;
            banner.standardType = "harbinger";
            banner.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name", user.getName()));
        }
        return banner;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip3").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.harbinger.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "soul");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.HARBINGER::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 700;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.26f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.25f;

    }
}
