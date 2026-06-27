package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.item.component.TargetedLocationComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class LichbladeSwordItem extends UniqueSwordItem implements TwoHandedWeapon {
    public LichbladeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    LivingEntity abilityTarget;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if(hand == Hand.OFF_HAND) {
            return TypedActionResult.fail(itemStack);
        }

        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        if (itemStack.isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get())) {
            return TypedActionResult.pass(itemStack);
        }
        LivingEntity abilityTarget = (LivingEntity) HelperMethods.getTargetedEntity(user, Config.uniqueEffects.lichblade.range);
        if (abilityTarget != null) {
            abilityTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 10, 0), user);
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

            if (stack.isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
                if (abilityTarget.isDead() || abilityTarget == user || remainingUseTicks < maxDuration) {
                    stack.set(ComponentTypeRegistry.TARGETED_LOCATION.get(), targetLocation.setTarget(user));
                    abilityTarget = user;
                    if (user.squaredDistanceTo(targetLocation.lastX(), targetLocation.lastY(), targetLocation.lastZ()) < radius) {
                        int damageTracker = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
                        user.setAbsorptionAmount(Math.min(Config.uniqueEffects.abilityAbsorptionCap, user.getAbsorptionAmount() + Math.min(damageTracker / 2f, Config.uniqueEffects.lichblade.absorptionCap)));
                        user.stopUsingItem();
                        world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_SPELL.get(),
                                user.getSoundCategory(), 0.04f, 0.5f);
                    }
                }
            } else if (stack.isOf(ItemsRegistry.WAKING_LICHBLADE.get()) && (abilityTarget.isDead() || remainingUseTicks < maxDuration)) {
                user.stopUsingItem();
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
            float abilityDamage = HelperMethods.spellScaledDamage("soul", user, Config.uniqueEffects.lichblade.spellScaling, Config.uniqueEffects.lichblade.damage);
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
            player.getItemCooldownManager().set(stack.getItem(), Config.uniqueEffects.lichblade.cooldown);
        }
        stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), null);
        stack.set(ComponentTypeRegistry.TARGETED_LOCATION.get(), null);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity user, int slot, boolean selected) {

        if (!user.getWorld().isClient()
                && user instanceof LivingEntity livingUser
                && livingUser.age % 35 == 0
                && livingUser.getEquippedStack(EquipmentSlot.MAINHAND) == stack
                && !livingUser.isUsingItem()) {

            float abilityDamage = HelperMethods.spellScaledDamage("soul", user, Config.uniqueEffects.lichblade.spellScaling, Config.uniqueEffects.lichblade.damage);
            int radius = Config.uniqueEffects.lichblade.radius;

            //AOE Aura
            Box box = new Box(livingUser.getX() + radius, livingUser.getY() + radius, livingUser.getZ() + radius,
                    livingUser.getX() - radius, livingUser.getY() - radius, livingUser.getZ() - radius);
            for (Entity entity : world.getOtherEntities(livingUser, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire((LivingEntity) entity, livingUser)) {
                    le.damage(livingUser.getDamageSources().indirectMagic(user, user), abilityDamage);
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
        HelperMethods.createFootfalls(user, stack, world, ParticleTypes.SOUL, ParticleTypes.SOUL, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, user, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        if (itemStack.isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1").setStyle(Styles.ABILITY));
        else if (itemStack.isOf(ItemsRegistry.WAKING_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.2").setStyle(Styles.ABILITY));
        else tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.3").setStyle(Styles.ABILITY));

        tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip2").setStyle(Styles.TEXT));

        if (!itemStack.isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get())) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip4").setStyle(Styles.TEXT));

            if (itemStack.isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
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
        public float damage = 6f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 200;
        @ValidatedFloat.Restrict(min = 0f)
        public float heal = 0.5f;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 3;
        @ValidatedDouble.Restrict(min = 1.0)
        public double range = 22.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.6f;
    }
}
