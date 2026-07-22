package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;

import java.util.List;

public class ChompolotlSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public ChompolotlSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            int skillCooldown = Config.uniqueEffects.chompolotl.cooldown;
            float skillDamage = Config.uniqueEffects.chompolotl.damageScaling;
            HelperMethods.playHitSounds(attacker, target);

            boolean coolingDown = attacker instanceof PlayerEntity player
                    ? player.getItemCooldownManager().isCoolingDown(stack.getItem())
                    : WeaponAbilityCooldownManager.isCoolingDown(serverWorld, attacker, stack);
            if (!coolingDown && target != null && HelperMethods.checkAbilityTarget(target, attacker)) {
                if (spawnAxolotl(serverWorld, attacker, target, skillDamage, false) != null) {
                    if (attacker instanceof PlayerEntity player) {
                        player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
                    } else {
                        WeaponAbilityCooldownManager.setCooldown(serverWorld, attacker, stack, skillCooldown);
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
        if (!world.isClient()) {

            int skillCooldown = Config.uniqueEffects.chompolotl.cooldown;
            ItemStack stack = user.getStackInHand(hand);
            ServerWorld serverWorld = (ServerWorld) world;
            if (user instanceof PlayerEntity player && !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                SimplySwordsAxolotlEntity axolotlEntity = EntityRegistry.SIMPLYAXOLOTLENTITY.get().spawn(
                        serverWorld,
                        user.getBlockPos().up(2).offset(user.getMovementDirection(), 3),
                        SpawnReason.MOB_SUMMONED);
                if (axolotlEntity != null) {
                    axolotlEntity.setTarget(user);
                    axolotlEntity.setOwner(user);
                    axolotlEntity.setVariant(AxolotlEntity.Variant.values()[4]);
                    double attackDamage = 0.5f + HelperMethods.attackScaledDamage(user, stack, Config.uniqueEffects.chompolotl.damageScaling);
                    EntityAttributeInstance attackAttribute = axolotlEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                    if (attackAttribute != null)
                        attackAttribute.setBaseValue(attackDamage);
                    EntityAttributeInstance speedAttribute = axolotlEntity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                    if (speedAttribute != null)
                        speedAttribute.setBaseValue(2.0);
                    world.playSound(null, user.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_WATER_SHOOT_IMPACT_01.get(),
                            user.getSoundCategory(), 0.4f, 1f);
                    player.getItemCooldownManager().set(stack.getItem(), skillCooldown * 10);
                }
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        return spawnAxolotl(context.world(), context.actor(), context.target(), Config.uniqueEffects.chompolotl.damageScaling, true) != null;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.chompolotl.cooldown * 10;
    }

    private static SimplySwordsAxolotlEntity spawnAxolotl(ServerWorld serverWorld, LivingEntity owner, LivingEntity target, float skillDamage, boolean activeSummon) {
        SimplySwordsAxolotlEntity axolotlEntity = EntityRegistry.SIMPLYAXOLOTLENTITY.get().spawn(
                serverWorld,
                owner.getBlockPos().up(2).offset(owner.getMovementDirection(), 3),
                SpawnReason.MOB_SUMMONED);
        if (axolotlEntity == null) {
            return null;
        }
        axolotlEntity.setTarget(target);
        axolotlEntity.setOwner(owner);
        if (activeSummon) {
            axolotlEntity.setVariant(AxolotlEntity.Variant.values()[4]);
        }
        double attackDamage = 0.5f + HelperMethods.attackScaledDamage(owner, owner.getMainHandStack(), skillDamage);
        EntityAttributeInstance attackAttribute = axolotlEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackAttribute != null) {
            attackAttribute.setBaseValue(attackDamage);
        }
        if (activeSummon) {
            EntityAttributeInstance speedAttribute = axolotlEntity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.setBaseValue(2.0);
            }
            serverWorld.playSound(null, owner.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_WATER_SHOOT_IMPACT_01.get(),
                    owner.getSoundCategory(), 0.4f, 1f);
        }
        return axolotlEntity;
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip7").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.chompolotl.cooldown * 10);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.CHOMPOLOTL::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 60;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 20f)
        public int duration = 500;
        @ValidatedFloat.Restrict(min = 0f)
        public float breedChance = 0.0266f;
        public boolean dolphinsGrace = true;

    }
}
