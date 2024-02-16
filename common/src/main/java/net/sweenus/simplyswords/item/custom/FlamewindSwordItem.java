package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.List;

public class FlamewindSwordItem extends UniqueSwordItem {
    public FlamewindSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {

            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient() && world instanceof  ServerWorld serverWorld) {
            int flameSeedDuration = 101;
            int flameSeedSpreadCap = 6;
            int skillCooldown = 980;

            Box box = HelperMethods.createBox(user, 10);
            Entity closestEntity = world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                    .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(user)))
                    .orElse(null);

            if (closestEntity != null) {
                if ((closestEntity instanceof LivingEntity ee)) {
                    if (HelperMethods.checkFriendlyFire(ee, user) && !ee.hasStatusEffect(EffectRegistry.FLAMESEED.get())) {

                        SoundEvent soundSelect = SoundRegistry.SPELL_FIRE.get();
                        int particleCount = 20; // Number of particles along the line
                        HelperMethods.spawnWaistHeightParticles(serverWorld, ParticleTypes.LAVA, user, ee, particleCount);
                        world.playSound(null, user.getBlockPos(), soundSelect,
                                user.getSoundCategory(), 0.3f, 1.3f);

                        SimplySwordsStatusEffectInstance flamSeedEffect = new SimplySwordsStatusEffectInstance(
                                EffectRegistry.FLAMESEED.get(), flameSeedDuration, 0, false,
                                false, true);
                        flamSeedEffect.setSourceEntity(user);
                        flamSeedEffect.setAdditionalData(flameSeedSpreadCap);
                        ee.addStatusEffect(flamSeedEffect);
                        user.getItemCooldownManager().set(this, skillCooldown);
                    }
                }
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.BUBBLE,
                ParticleTypes.BUBBLE_COLUMN_UP, ParticleTypes.BUBBLE_POP, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip7").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip8").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip9").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip10").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip11").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
