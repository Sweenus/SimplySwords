package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class LivyatanSwordItem extends SwordItem {
    public LivyatanSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
            int fhitchance = (int) SimplySwordsConfig.getFloatValue("ferocity_chance");
            int fduration = (int) SimplySwordsConfig.getFloatValue("ferocity_duration");
            int maximum_stacks = (int) (SimplySwordsConfig.getFloatValue("ferocity_max_stacks"));

            boolean impactsounds_enabled = (SimplySwordsConfig.getBooleanValue("enable_weapon_impact_sounds"));

            if (impactsounds_enabled) {
                int choose_sound = (int) (Math.random() * 30);
                float choose_pitch = (float) Math.random() * 2;
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 20 && choose_sound > 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 30 && choose_sound > 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 40 && choose_sound > 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
            }


            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                if (attacker.hasStatusEffect(StatusEffects.HASTE)) {

                    int a = (attacker.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1);
                    world.playSoundFromEntity(null, attacker, SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.3f, 1f + ( a / 10f));

                    if ((attacker.getStatusEffect(StatusEffects.HASTE).getAmplifier() < maximum_stacks)) {
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, a), attacker);
                    } else {
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, a - 1), attacker);
                    }
                } else {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, 1), attacker);
                }
            }
        }

        return super.postHit(stack, target, attacker);

    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        //Do something

        return super.use(world,user,hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.WHITE_ASH, ParticleTypes.WHITE_ASH, ParticleTypes.WHITE_ASH, true);


        if (entity instanceof PlayerEntity player) {

            if (player.handSwinging) {
                //May need an entrypoint for left_click here - Better Combat breaks isSwinging

                Vec3d userView = player.getRotationVec(1.0F);
                //Vec3d userViewStart = player.getRotationVec(1.0F).normalize().add(0.5,0,0).normalize();

                for (int i = 20; i > 0; i--) {
                    Vec3d userViewStart = player.getRotationVec(1.0F).normalize().add(i / 10,0,0).normalize();
                    world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, player.getX() + userViewStart.x, player.getY() + userView.y + 1.5, player.getZ() + userView.z,
                            userView.x * 2, userView.y * 2, userView.z * 2);
                }

            }

        }


        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }


    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip3"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip6"));

        /*

        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.ferocitysworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.ferocitysworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.ferocitysworditem.tooltip3"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(new TranslatableText("item.simplyswords.ferocitysworditem.tooltip4"));
        tooltip.add(new TranslatableText("item.simplyswords.ferocitysworditem.tooltip5"));
        tooltip.add(new TranslatableText("item.simplyswords.ferocitysworditem.tooltip6"));

         */
    }

}
