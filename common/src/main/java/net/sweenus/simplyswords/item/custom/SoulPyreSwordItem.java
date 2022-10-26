package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class SoulPyreSwordItem extends SwordItem {
    public SoulPyreSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private int relocationTimer;
    private final int relocationDuration = (int) SimplySwordsConfig.getFloatValue("soultether_duration");
    private boolean canRelocate;
    private LivingEntity relocateTarget;
    private double relocateX;
    private double relocateY;
    private double relocateZ;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld sworld = (ServerWorld) attacker.world;
            boolean impactsounds_enabled = (SimplySwordsConfig.getBooleanValue("enable_weapon_impact_sounds"));

            if (impactsounds_enabled) {
                int choose_sound = (int) (Math.random() * 30);
                float choose_pitch = (float) Math.random() * 2;
                if (choose_sound <= 10)
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 20 && choose_sound > 10)
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 30 && choose_sound > 20)
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 40 && choose_sound > 30)
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
            }

        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {


        if (!user.world.isClient()) {
            int range = (int) SimplySwordsConfig.getFloatValue("soultether_range");
            int radius = (int) (SimplySwordsConfig.getFloatValue("soultether_radius"));
            int ignite_duration = (int) (SimplySwordsConfig.getFloatValue("soultether_ignite_duration")) / 20;
            int resistance_duration = (int) (SimplySwordsConfig.getFloatValue("soultether_resistance_duration"));

            //Position swap target & player
            LivingEntity target = (LivingEntity) HelperMethods.getTargetedEntity(user, range);
            if (target != null) {
                relocateX = user.getX();
                relocateY = user.getY();
                relocateZ = user.getZ();
                relocateTarget = target;
                double rememberx = target.getX();
                double remembery = target.getY();
                double rememberz = target.getZ();
                target.teleport(user.getX(), user.getY(), user.getZ(), true);
                user.teleport(rememberx, remembery, rememberz, true);
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_01.get(), SoundCategory.PLAYERS, 0.3f, 1f);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistance_duration, 0), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, resistance_duration, 0), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, relocationDuration, 3), user);
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), relocationDuration - 10, 0), user);
                canRelocate = true;
                relocationTimer = relocationDuration;

                //AOE ignite & pull
                Box box = new Box(rememberx + radius, remembery + radius, rememberz + radius, rememberx - radius, remembery - radius, rememberz - radius);
                for(Entity entities: world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if (entities != null) {
                        if (entities instanceof LivingEntity le) {
                            le.setVelocity((rememberx - le.getX()) /4,  (remembery - le.getY()) /4, (rememberz - le.getZ()) /4);
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3), user);
                            world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.1f, 3f);
                            le.setOnFireFor(ignite_duration);
                        }
                    }
                }
                user.getItemCooldownManager().set(this, relocationDuration);
            }
        }

        return super.use(world,user,hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && (entity instanceof PlayerEntity player)) {
            if (canRelocate) {
                relocationTimer--;
                if (relocationTimer <= 0) {

                    if (relocateTarget != null)
                        relocateTarget.teleport(player.getX(), player.getY(), player.getZ(), true);

                    player.teleport(relocateX, relocateY, relocateZ, true);
                    world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(), SoundCategory.PLAYERS, 0.3f, 1f);
                    canRelocate = false;

                }

                if (relocationTimer == 40)
                    world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_RECHARGE.get(), SoundCategory.PLAYERS, 0.3f, 0.4f);

            }
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip3"));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip6"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip7", relocationDuration / 20));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip8"));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip9"));
        tooltip.add(Text.literal(""));

        /*
        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip3"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip4"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip5"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip6"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip7"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip8"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip9"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip10"));

 */
    }

}
