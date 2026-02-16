package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ChompolotlSwordItem extends UniqueSwordItem {
    public ChompolotlSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) attacker.getEntityWorld();
            int skillCooldown = Config.uniqueEffects.chompolotl.cooldown;
            float skillDamage = Config.uniqueEffects.chompolotl.damage;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker instanceof PlayerEntity player && !player.getItemCooldownManager().isCoolingDown(stack)) {
            SimplySwordsAxolotlEntity axolotlEntity = EntityRegistry.SIMPLYAXOLOTLENTITY.get().spawn(
                    serverWorld,
                    attacker.getBlockPos().up(2).offset(attacker.getMovementDirection(), 3),
                    SpawnReason.MOB_SUMMONED);
                if (axolotlEntity != null && target != null) {
                    axolotlEntity.setTarget(target);
                    axolotlEntity.setOwner(attacker);
                    double attackDamage = (0.5f + skillDamage * HelperMethods.getEntityAttackDamage(attacker));
                    EntityAttributeInstance attackAttribute = axolotlEntity.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
                    if (attackAttribute != null)
                        attackAttribute.setBaseValue(attackDamage);
                    player.getItemCooldownManager().set(stack, skillCooldown);
                }
            }
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {

            int skillCooldown = Config.uniqueEffects.chompolotl.cooldown;
            float skillDamage = Config.uniqueEffects.chompolotl.damage;
            ItemStack stack = user.getStackInHand(hand);
            ServerWorld serverWorld = (ServerWorld) world;
            if (user instanceof PlayerEntity player && !player.getItemCooldownManager().isCoolingDown(stack)) {
                SimplySwordsAxolotlEntity axolotlEntity = EntityRegistry.SIMPLYAXOLOTLENTITY.get().spawn(
                        serverWorld,
                        user.getBlockPos().up(2).offset(user.getMovementDirection(), 3),
                        SpawnReason.MOB_SUMMONED);
                if (axolotlEntity != null) {
                    axolotlEntity.setTarget(user);
                    axolotlEntity.setOwner(user);
                    double attackDamage = (0.5f + skillDamage * HelperMethods.getEntityAttackDamage(user));
                    EntityAttributeInstance attackAttribute = axolotlEntity.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
                    if (attackAttribute != null)
                        attackAttribute.setBaseValue(attackDamage);
                    EntityAttributeInstance speedAttribute = axolotlEntity.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                    if (speedAttribute != null)
                        speedAttribute.setBaseValue(2.0);
                    world.playSound(null, user.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_WATER_SHOOT_IMPACT_01.get(),
                            user.getSoundCategory(), 0.4f, 1f);
                    player.getItemCooldownManager().set(stack, skillCooldown * 10);
                }
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip7").setStyle(Styles.TEXT));
        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.CHOMPOLOTL::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 60;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 1.0f;
        @ValidatedFloat.Restrict(min = 20f)
        public int duration = 500;
        @ValidatedFloat.Restrict(min = 0f)
        public float breedChance = 0.0266f;
        public boolean dolphinsGrace = true;

    }
}
