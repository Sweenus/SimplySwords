package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.item.component.TargetedLocationComponent;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityChannelManager;

import java.util.List;

public class LichbladeSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public LichbladeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        HelperMethods.playHitSounds(attacker, target);
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if(hand == Hand.OFF_HAND) {
            return TypedActionResult.fail(itemStack);
        }

        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        if (!AwakeningApi.isAbilityUnlocked(itemStack)) {
            return TypedActionResult.pass(itemStack);
        }
        if (!world.isClient()) {
            LivingEntity abilityTarget = StealSwordItem.findLenientTarget(user, Config.uniqueEffects.lichblade.range);
            if (abilityTarget == null) {
                return TypedActionResult.fail(itemStack);
            }
            world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ENCHANT.get(),
                    user.getSoundCategory(), 0.5f, 0.5f);
            itemStack.set(ComponentTypeRegistry.TARGETED_LOCATION.get(), new TargetedLocationComponent(abilityTarget.getUuid(), user.getX(), user.getY(), user.getZ()));
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient) return;
        TargetedLocationComponent targetLocation = stack.getOrDefault(ComponentTypeRegistry.TARGETED_LOCATION.get(), TargetedLocationComponent.DEFAULT);
        LivingEntity abilityTarget = targetLocation.getEntity((ServerWorld) world);
        if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && abilityTarget != null) {
            //Return to user after the duration or after the enemy dies & buff user
            int maxDuration = Config.uniqueEffects.lichblade.duration;
            int radius = Config.uniqueEffects.lichblade.radius;

            if (AwakeningApi.getLevel(stack) >= 8) {
                if (abilityTarget.isDead() || abilityTarget == user || remainingUseTicks < maxDuration) {
                    stack.set(ComponentTypeRegistry.TARGETED_LOCATION.get(), targetLocation.setTarget(user));
                    abilityTarget = user;
                    if (user.squaredDistanceTo(targetLocation.lastX(), targetLocation.lastY(), targetLocation.lastZ()) < radius) {
                        int damageTracker = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
                        user.setAbsorptionAmount(Math.min(Config.uniqueEffects.abilityAbsorptionCap, user.getAbsorptionAmount() + Math.min(damageTracker / 2f, Config.uniqueEffects.lichblade.absorptionCap)));
                        if (!(user instanceof ServerPlayerEntity serverPlayer) || !PlayerWeaponAbilityChannelManager.finishEarly(serverPlayer, stack)) {
                            user.stopUsingItem();
                        }
                        world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_SPELL.get(),
                                user.getSoundCategory(), 0.04f, 0.5f);
                    }
                }
            } else if (AwakeningApi.isAbilityUnlocked(stack) && (abilityTarget.isDead() || remainingUseTicks < maxDuration)) {
                if (!(user instanceof ServerPlayerEntity serverPlayer) || !PlayerWeaponAbilityChannelManager.finishEarly(serverPlayer, stack)) {
                    user.stopUsingItem();
                }
            }

            //Move aura to target
            double lastX = targetLocation.lastX();
            double lastY = targetLocation.lastY();
            double lastZ = targetLocation.lastZ();
            if (user.age % 5 == 0) {
                double targetX = abilityTarget.getX();
                double targetY = abilityTarget.getY();
                double targetZ = abilityTarget.getZ();

                if (targetX > lastX) lastX += 1;
                if (targetX < lastX) lastX -= 1;
                if (targetZ > lastZ) lastZ += 1;
                if (targetZ < lastZ) lastZ -= 1;
                if (targetY > lastY) lastY += 1;
                if (targetY < lastY) lastY -= 1;
            }
            stack.set(ComponentTypeRegistry.TARGETED_LOCATION.get(), new TargetedLocationComponent(abilityTarget.getUuid(), lastX, lastY, lastZ));
            float abilityDamage = HelperMethods.abilityScaledDamage("soul", user, stack,
                    Config.uniqueEffects.lichblade.damageScaling, Config.uniqueEffects.lichblade.spellScaling);
            float healAmount = Config.uniqueEffects.lichblade.heal;
            AbilityMethods.tickAbilitySoulAnguish(stack, world, user, abilityDamage, radius, lastX, lastY, lastZ, healAmount, abilityTarget);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return Config.uniqueEffects.lichblade.duration * 2;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        TargetedLocationComponent targetLocation = stack.get(ComponentTypeRegistry.TARGETED_LOCATION.get());
        if (!world.isClient && (user instanceof PlayerEntity player) && targetLocation != null && ((ServerWorld)world).getEntity(targetLocation.uuid()) != null) {
            SimplySwordsAPI.setWeaponCooldown(player, stack, Config.uniqueEffects.lichblade.cooldown);
        }
        stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), null);
        stack.set(ComponentTypeRegistry.TARGETED_LOCATION.get(), null);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && AwakeningApi.isAbilityUnlocked(context.stack())
                && UniqueWeaponActiveAbility.super.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        LivingEntity target = context.target();
        ItemStack stack = context.stack();
        if (target == null || !HelperMethods.checkAbilityTarget(target, actor) || !AwakeningApi.isAbilityUnlocked(stack)) {
            return false;
        }
        float abilityDamage = HelperMethods.abilityScaledDamage("soul", actor, stack,
                Config.uniqueEffects.lichblade.damageScaling, Config.uniqueEffects.lichblade.spellScaling);
        float healAmount = Config.uniqueEffects.lichblade.heal;
        int radius = Config.uniqueEffects.lichblade.radius;
        stack.set(ComponentTypeRegistry.TARGETED_LOCATION.get(), new TargetedLocationComponent(target.getUuid(), target.getX(), target.getY(), target.getZ()));
        AbilityMethods.tickAbilitySoulAnguish(stack, context.world(), actor, abilityDamage, radius, target.getX(), target.getY(), target.getZ(), healAmount, target);
        if (AwakeningApi.getLevel(stack) >= 8) {
            int damageTracker = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
            actor.setAbsorptionAmount(Math.min(Config.uniqueEffects.abilityAbsorptionCap,
                    actor.getAbsorptionAmount() + Math.min(damageTracker / 2f, Config.uniqueEffects.lichblade.absorptionCap)));
        }
        stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), null);
        stack.set(ComponentTypeRegistry.TARGETED_LOCATION.get(), null);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.lichblade.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity user, int slot, boolean selected) {

        if (!user.getWorld().isClient()
                && user instanceof LivingEntity livingUser
                && livingUser.getEquippedStack(EquipmentSlot.MAINHAND) == stack
                && AwakeningApi.isAbilityUnlocked(stack)
                && !livingUser.isUsingItem()) {
            tickPassiveAura((ServerWorld) world, livingUser, stack);
        }
        HelperMethods.createFootfalls(user, stack, world, ParticleTypes.SOUL, ParticleTypes.SOUL, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, user, slot, selected);
    }

    public static void tickPassiveAura(ServerWorld world, LivingEntity livingUser, ItemStack stack) {
        if (livingUser == null
                || stack == null
                || stack.isEmpty()
                || !AwakeningApi.isAbilityUnlocked(stack)
                || livingUser.age % 35 != 0
                || livingUser.getEquippedStack(EquipmentSlot.MAINHAND) != stack
                || livingUser.isUsingItem()) {
            return;
        }

        float abilityDamage = HelperMethods.abilityScaledDamage("soul", livingUser, stack,
                Config.uniqueEffects.lichblade.damageScaling, Config.uniqueEffects.lichblade.spellScaling);
        int radius = Config.uniqueEffects.lichblade.radius;

        Box box = new Box(livingUser.getX() + radius, livingUser.getY() + radius, livingUser.getZ() + radius,
                livingUser.getX() - radius, livingUser.getY() - radius, livingUser.getZ() - radius);
        for (Entity entity : world.getOtherEntities(livingUser, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (entity instanceof LivingEntity le && HelperMethods.checkAbilityTarget(le, livingUser)) {
                DamageSource damageSource = livingUser.getDamageSources().indirectMagic(livingUser, livingUser);
                le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, le, damageSource, abilityDamage));
            }
        }
        world.playSoundFromEntity(null, livingUser, SoundRegistry.DARK_SWORD_BLOCK.get(),
                livingUser.getSoundCategory(), 0.1f, 0.2f);
        double xPos = livingUser.getX() - (radius + 1);
        double yPos = livingUser.getY();
        double zPos = livingUser.getZ() - (radius + 1);

        for (int i = radius * 2; i > 0; i--) {
            for (int j = radius * 2; j > 0; j--) {
                float choose = (float) (Math.random() * 1);
                HelperMethods.spawnParticle(world, ParticleTypes.SCULK_SOUL,
                        xPos + i + choose, yPos, zPos + j + choose,
                        0, 0.1, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.SOUL,
                        xPos + i + choose, yPos + 0.1, zPos + j + choose,
                        0, 0, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM,
                        xPos + i + choose, yPos + 2, zPos + j + choose,
                        0, 0, 0);
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        int awakening = AwakeningApi.getLevel(itemStack);
        if (awakening < 4)
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1").setStyle(Styles.ABILITY));
        else if (awakening < 8)
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.2").setStyle(Styles.ABILITY));
        else tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.3").setStyle(Styles.ABILITY));

        tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip2").setStyle(Styles.TEXT));

        if (awakening >= 4) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip4").setStyle(Styles.TEXT));
            appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.lichblade.cooldown);
            appendAbilityManaCostTooltip(tooltip, itemStack);

            if (awakening >= 8) {
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip7").setStyle(Styles.TEXT));
            }
        }
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "soul");
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.lichblade");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SLUMBERING_LICHBLADE::get, ItemsRegistry.WAKING_LICHBLADE::get, ItemsRegistry.AWAKENED_LICHBLADE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int absorptionCap = 8;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 700;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.37f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 200;
        @ValidatedFloat.Restrict(min = 0f)
        public float heal = 0.5f;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 3;
        @ValidatedDouble.Restrict(min = 1.0)
        public double range = 22.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 2.52f;
    }
}
