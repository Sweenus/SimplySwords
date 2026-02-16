package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.FrostfallEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class FrostfallSwordItem extends UniqueSwordItem {
    public FrostfallSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getEntityWorld().isClient()) {
            super.postHit(stack, target, attacker);
            return;
        }
        HelperMethods.playHitSounds(attacker, target);
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user.getEntityWorld().isClient()) return super.use(world, user, hand);

        float abilityDamage = HelperMethods.spellScaledDamage("frost", user, Config.uniqueEffects.frostfall.spellScaling, Config.uniqueEffects.frostfall.damage);
        float pulseDamage = HelperMethods.spellScaledDamage("frost", user, Config.uniqueEffects.frostfall.spellScaling, Config.uniqueEffects.frostfall.pulseDamage);
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient()) {
            itemStack = user.getStackInHand(hand);
            FrostfallEntity frostfallEntity = new FrostfallEntity(world, user, itemStack.copy() );
            frostfallEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            frostfallEntity.setYaw(user.getYaw());
            frostfallEntity.setPitch(user.getPitch());
            frostfallEntity.primaryBaseDamage = abilityDamage;
            frostfallEntity.detonateDamage = pulseDamage;
            frostfallEntity.addedChance = Config.uniqueEffects.frostfall.chance;
            frostfallEntity.detonateRadius = Config.uniqueEffects.frostfall.radius;
            frostfallEntity.duration = Config.uniqueEffects.frostfall.duration;
            if (hand == Hand.OFF_HAND)
                frostfallEntity.offhandThrow = true;
            frostfallEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(frostfallEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }

        user.swingHand(hand);


        user.getItemCooldownManager().set(this.getDefaultStack(), Config.uniqueEffects.frostfall.cooldown);
        return ActionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE,
                ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Environment(EnvType.CLIENT)
    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip5").setStyle(Styles.TEXT));
        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "frost");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.FROSTFALL::get));
        }

        @ValidatedInt.Restrict(min = 1, max = 100)
        public int chance = 25;
        @ValidatedInt.Restrict(min = 1)
        public int cooldown = 3;
        @ValidatedFloat.Restrict(min = 1f)
        public float damage = 11f;
        @ValidatedFloat.Restrict(min = 0f)
        public float pulseDamage = 11f;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 80;
        @ValidatedDouble.Restrict(min = 6.0)
        public double radius = 8.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.4f;
    }
}