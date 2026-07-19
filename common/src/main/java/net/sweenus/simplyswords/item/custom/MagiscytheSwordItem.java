package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;
import java.util.Random;

public class MagiscytheSwordItem extends UniqueSwordItem {
    public MagiscytheSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            ServerWorld world = (ServerWorld) attacker.getWorld();

            if (attacker.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISTORM))) {
                world.playSound(null, attacker.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                        attacker.getSoundCategory(), 0.1f, 1.9f);

                float repairChance = Config.uniqueEffects.magiscythe.repairChance;
                Random random = new Random();
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                        ItemStack item = attacker.getEquippedStack(slot);
                        if (!item.isEmpty() && random.nextFloat() < repairChance && item.getDamage() > 0) {
                            item.setDamage((int) (item.getDamage() - HelperMethods.getEntityAttackDamage(attacker)));
                            break;
                        }
                    }
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        int skillCooldown = Config.uniqueEffects.magiscythe.cooldown;
        int baseEffectDuration = Config.uniqueEffects.magiscythe.duration;

        world.playSound(null, user.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_NORDIC_22.get(),
                user.getSoundCategory(), 0.2f, 1.1f);
        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.MAGISTORM), baseEffectDuration, 1));
        user.getItemCooldownManager().set(this, skillCooldown);

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT,
                ParticleTypes.ENCHANT, ParticleTypes.ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magiscythesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.magiscythesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.magiscythesworditem.tooltip6").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.magiscythe.cooldown);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "arcane");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MAGISCYTHE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 980;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 3f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 400;
        @ValidatedDouble.Restrict(min = 1f)
        public double radius = 4.0;
        @ValidatedFloat.Restrict(min = 0f, max = 1f)
        public float repairChance = 0.25f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.5f;

    }
}
