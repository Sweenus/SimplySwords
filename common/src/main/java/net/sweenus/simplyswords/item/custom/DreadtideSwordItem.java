package net.sweenus.simplyswords.item.custom;

import elocindev.necronomicon.api.text.TextAPI;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DreadtideSwordItem extends UniqueSwordItem {
    public DreadtideSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return TypedActionResult.pass(stack);
        }
        if (!user.getWorld().isClient() && world instanceof  ServerWorld serverWorld) {
            int voidcallerDuration = Config.uniqueEffects.dreadtide.get().duration;
            float voidcallerDamageModifier = Config.uniqueEffects.dreadtide.get().damageScaling;
            int skillCooldown = 20;

            Box box = HelperMethods.createBox(user, 10);
            Entity closestEntity = world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                    .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(user)))
                    .orElse(null);

            if (closestEntity != null) {
                if ((closestEntity instanceof LivingEntity ee)) {
                    if (HelperMethods.checkFriendlyFire(ee, user)) {

                        StatusEffectInstance voidcloakEffect = user.getStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
                        if (voidcloakEffect != null) {
                            SoundEvent soundSelect = SoundRegistry.MAGIC_SHAMANIC_VOICE_04.get();
                            List<SoundEvent> sounds = new ArrayList<>();
                            sounds.add(SoundRegistry.MAGIC_SHAMANIC_VOICE_04.get());
                            sounds.add(SoundRegistry.MAGIC_SHAMANIC_VOICE_12.get());
                            sounds.add(SoundRegistry.MAGIC_SHAMANIC_VOICE_15.get());
                            sounds.add(SoundRegistry.MAGIC_SHAMANIC_VOICE_20.get());
                            sounds.add(SoundRegistry.MAGIC_SHAMANIC_NORDIC_02.get());
                            sounds.add(SoundRegistry.MAGIC_SHAMANIC_NORDIC_02.get());
                            sounds.add(SoundRegistry.MAGIC_SHAMANIC_NORDIC_02.get());
                            if (sounds.get(voidcloakEffect.getAmplifier()) != null)
                                soundSelect = sounds.get(Math.min(5, voidcloakEffect.getAmplifier()));

                            int particleCount = 20; // Number of particles along the line

                            HelperMethods.spawnWaistHeightParticles(serverWorld, ParticleTypes.SMOKE, user, ee, particleCount);


                            world.playSound(null, user.getBlockPos(), soundSelect,
                                    user.getSoundCategory(), 0.3f, 1.3f);

                            SimplySwordsStatusEffectInstance voidAssaultEffect = new SimplySwordsStatusEffectInstance(
                                    EffectRegistry.getReference(EffectRegistry.VOIDASSAULT), voidcallerDuration, voidcloakEffect.getAmplifier(), false,
                                    false, true);
                            voidAssaultEffect.setSourceEntity(user);
                            voidAssaultEffect.setAdditionalData((int) HelperMethods.abilityScaledDamage(
                                    "eldritch", user, stack, voidcallerDamageModifier,
                                    Config.uniqueEffects.dreadtide.get().spellScaling));
                            ee.addStatusEffect(voidAssaultEffect);
                            user.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
                            user.getItemCooldownManager().set(this, skillCooldown);
                        }
                    }
                }
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }
    @Override
    public Text getName(ItemStack stack) {
        MutableText name = Text.translatable(stack.getTranslationKey());
        Style bold = name.getStyle().withBold(true);
        return TextAPI.Styles.getGradient(Text.translatable(this.getTranslationKey(stack)).setStyle(bold), 1, 6043781, 12088090, 1.0F);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        MutableText ability_icon = Text.empty().append("\uA996 ");
        MutableText types = TextAPI.Styles.getGradient(Text.translatable("item.eldritch_end.corrupted_item.type"), 1, 6043781, 9326287, 1.0F);

        tooltip.add(Text.literal("\uA999 ").append(types.fillStyle(types.getStyle().withUnderline(true))));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.dreadtidesworditem.tooltip1").setStyle(Styles.CORRUPTED_ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.dreadtidesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.dreadtidesworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(ability_icon.append(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.CORRUPTED_LIGHT)));
        tooltip.add(Text.translatable("item.simplyswords.dreadtidesworditem.tooltip8").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, 20);
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("\uA999 ").append(Text.translatable("item.simplyswords.dreadtidesworditem.tooltip12").setStyle(Styles.CORRUPTED)));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "eldritch");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(() -> Registries.ITEM.get(Identifier.of(SimplySwords.MOD_ID, "dreadtide"))));
        }

        @ValidatedFloat.Restrict(min = 0)
        public float damageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0)
        public float spellScaling = 1.6f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 250;
        @ValidatedInt.Restrict(min = 1)
        public int corruptionFrequency = 60;
        @ValidatedFloat.Restrict(min = 0)
        public float corruptionPerTick = 1.0f;
        @ValidatedInt.Restrict(min = 0)
        public int corruptionDuration = 1200;
        @ValidatedFloat.Restrict(min = 0)
        public float corruptionMax = 100f;
        @ValidatedInt.Restrict(min = 1)
        public int startingTickFrequency = 12;

    }
}
