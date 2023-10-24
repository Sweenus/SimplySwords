package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class LichbladeSwordItem extends UniqueSwordItem {
    public LichbladeSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;
    public static boolean scalesWithSpellPower;
    float overallAbsorptionCap = Config.getFloat("abilityAbsorptionCap", "UniqueEffects", ConfigDefaultValues.abilityAbsorptionCap);
    int radius = (int) Config.getFloat("soulAnguishRadius", "UniqueEffects", ConfigDefaultValues.soulAnguishRadius);
    float absorptionCap = Config.getFloat("soulAnguishAbsorptionCap", "UniqueEffects", ConfigDefaultValues.soulAnguishAbsorptionCap);
    float abilityDamage = Config.getFloat("soulAnguishDamage", "UniqueEffects", ConfigDefaultValues.soulAnguishDamage);
    int maxDuration = (int) Config.getFloat("soulAnguishDuration", "UniqueEffects", ConfigDefaultValues.soulAnguishDuration);
    int skillCooldown = (int) Config.getFloat("soulAnguishCooldown", "UniqueEffects", ConfigDefaultValues.soulAnguishCooldown);
    float healAmount = Config.getFloat("soulAnguishHeal", "UniqueEffects", ConfigDefaultValues.soulAnguishHeal);
    int range = (int) Config.getFloat("soulAnguishRange", "UniqueEffects", ConfigDefaultValues.soulAnguishRange);
    float spellScalingModifier = Config.getFloat("soulAnguishSpellScaling", "UniqueEffects", ConfigDefaultValues.soulAnguishSpellScaling);
    public int damageTracker = 0;
    double lastX;
    double lastY;
    double lastZ;
    double targetX;
    double targetY;
    double targetZ;

    LivingEntity abilityTarget;

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
        if (itemStack.isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get())) {
            return TypedActionResult.pass(itemStack);
        }
        abilityTarget = (LivingEntity) HelperMethods.getTargetedEntity(user, range);
        if (abilityTarget != null) {
            abilityTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 10, 0), user);
            world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ENCHANT.get(),
                    user.getSoundCategory(), 0.5f, 0.5f);
            lastX = user.getX();
            lastY = user.getY();
            lastZ = user.getZ();
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient() && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && abilityTarget != null) {
            //Return to user after the duration or after the enemy dies & buff user
            if (stack.isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
                if (abilityTarget.isDead() || abilityTarget == user || remainingUseTicks < maxDuration) {
                    abilityTarget = user;
                    if (user.squaredDistanceTo(lastX, lastY, lastZ) < radius) {
                        user.setAbsorptionAmount(Math.min(overallAbsorptionCap, user.getAbsorptionAmount() + Math.min(damageTracker / 2f, absorptionCap)));
                        user.stopUsingItem();
                        world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_SPELL.get(),
                                user.getSoundCategory(), 0.04f, 0.5f);
                    }
                }
            } else if (stack.isOf(ItemsRegistry.WAKING_LICHBLADE.get()) && (abilityTarget.isDead() || remainingUseTicks < maxDuration)) {
                user.stopUsingItem();
            }
            //Move aura to target
            if (user.age % 5 == 0) {
                targetX = abilityTarget.getX();
                targetY = abilityTarget.getY();
                targetZ = abilityTarget.getZ();

                if (targetX > lastX) lastX += 1;
                if (targetX < lastX) lastX -= 1;
                if (targetZ > lastZ) lastZ += 1;
                if (targetZ < lastZ) lastZ -= 1;
                if (targetY > lastY) lastY += 1;
                if (targetY < lastY) lastY -= 1;
            }
            AbilityMethods.tickAbilitySoulAnguish(stack, world, user, abilityDamage, radius, lastX, lastY, lastZ,
                    healAmount, abilityTarget);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return maxDuration * 2;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        damageTracker = 0;
        if (!world.isClient && (user instanceof PlayerEntity player) && abilityTarget != null) {
            player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity user, int slot, boolean selected) {
        if (HelperMethods.commonSpellAttributeScaling(spellScalingModifier, user, "soul") > 0) {
            abilityDamage = HelperMethods.commonSpellAttributeScaling(spellScalingModifier, user, "soul");
            scalesWithSpellPower = true;
        }

        if (!user.getWorld().isClient() && user instanceof LivingEntity livingUser) {
            //AOE Aura
            if (livingUser.age % 35 == 0 && livingUser.getEquippedStack(EquipmentSlot.MAINHAND) == stack && !livingUser.isUsingItem()) {
                Box box = new Box(livingUser.getX() + radius, livingUser.getY() + radius, livingUser.getZ() + radius,
                        livingUser.getX() - radius, livingUser.getY() - radius, livingUser.getZ() - radius);
                for (Entity entity : world.getOtherEntities(livingUser, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire((LivingEntity) entity, livingUser)) {
                        le.damage(livingUser.getDamageSources().indirectMagic(user, user), abilityDamage);
                    }
                }
                world.playSoundFromEntity(null, livingUser, SoundRegistry.DARK_SWORD_BLOCK.get(),
                        livingUser.getSoundCategory(), 0.1f, 0.2f);
                double xpos = livingUser.getX() - (radius + 1);
                double ypos = livingUser.getY();
                double zpos = livingUser.getZ() - (radius + 1);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.SCULK_SOUL,
                                xpos + i + choose, ypos, zpos + j + choose,
                                0, 0.1, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.SOUL,
                                xpos + i + choose, ypos + 0.1, zpos + j + choose,
                                0, 0, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM,
                                xpos + i + choose, ypos + 2, zpos + j + choose,
                                0, 0, 0);
                    }
                }
            }
        }
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(user, stack, world, stepMod, ParticleTypes.SOUL, ParticleTypes.SOUL,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, user, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        if (itemStack.isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1").setStyle(ABILITY));
        else if (itemStack.isOf(ItemsRegistry.WAKING_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.2").setStyle(ABILITY));
        else tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.3").setStyle(ABILITY));

        tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip3", radius).setStyle(TEXT));

        if (!itemStack.isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get())) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(RIGHTCLICK));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip4").setStyle(TEXT));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip5").setStyle(TEXT));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip6").setStyle(TEXT));

            if (itemStack.isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip7").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip8").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip9").setStyle(TEXT));
            }
        }
        if (scalesWithSpellPower) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.compat.scaleSoul"));
        }
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
