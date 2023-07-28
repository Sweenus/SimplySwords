package net.sweenus.simplyswords.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class SunfireSwordItem extends UniqueSwordItem {
    private static int stepMod = 0;
    public static boolean scalesWithSpellPower;
    int skillCooldown = (int) SimplySwords.uniqueEffectsConfig.righteousStandardCooldown;
    int abilityChance = (int) SimplySwords.uniqueEffectsConfig.righteousStandardChance;

    public SunfireSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient() && attacker.getRandom().nextInt(100) <= abilityChance && attacker instanceof PlayerEntity) {
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.3f, 1.7f);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 1), attacker);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) user.getWorld();
            BlockState currentState = world.getBlockState(user.getBlockPos().up(4).offset(user.getMovementDirection(), 3));
            BlockState state = Blocks.AIR.getDefaultState();
            if (currentState == state) {
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
                        user.getSoundCategory(), 0.4f, 0.8f);
                BattleStandardEntity banner = EntityRegistry.BATTLESTANDARD.get().spawn(
                        serverWorld,
                        user.getBlockPos().up(4).offset(user.getMovementDirection(), 3),
                        SpawnReason.MOB_SUMMONED);
                if (banner != null) {
                    banner.setVelocity(0, -1, 0);
                    banner.ownerEntity = user;
                    banner.decayRate = 3;
                    banner.standardType = "sunfire";
                    banner.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name", user.getName()));
                }
                user.getItemCooldownManager().set(this.getDefaultStack().getItem(), skillCooldown);
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (HelperMethods.commonSpellAttributeScaling(2, entity, "fire") > 0) {
            scalesWithSpellPower = true;
        }
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.sunfiresworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.sunfiresworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.sunfiresworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.sunfiresworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.sunfiresworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.sunfiresworditem.tooltip6").setStyle(TEXT));
        if (scalesWithSpellPower) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.compat.scaleFire"));
            tooltip.add(Text.translatable("item.simplyswords.compat.scaleHealing"));
        }
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
