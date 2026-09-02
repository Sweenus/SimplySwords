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
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.Phase10WeaponManager;

import java.util.ArrayList;
import java.util.List;

public class DreadtideSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
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
            Phase10WeaponManager.onDreadtideHit(attacker, target, stack);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && !context.stack().isEmpty()
                && Phase10WeaponManager.canActivateDreadtide(context.world(), context.actor(), context.stack());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        ServerWorld world = context.world();
        if (!Phase10WeaponManager.activateDreadtide(world, actor, context.stack())) {
            return false;
        }
        LivingEntity target = context.target();
        StatusEffectInstance voidcloakEffect = actor.getStatusEffect(
                EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
        int amplifier = voidcloakEffect == null ? 0 : voidcloakEffect.getAmplifier();
        List<SoundEvent> sounds = new ArrayList<>();
        sounds.add(SoundRegistry.MAGIC_SHAMANIC_VOICE_04.get());
        sounds.add(SoundRegistry.MAGIC_SHAMANIC_VOICE_12.get());
        sounds.add(SoundRegistry.MAGIC_SHAMANIC_VOICE_15.get());
        sounds.add(SoundRegistry.MAGIC_SHAMANIC_VOICE_20.get());
        sounds.add(SoundRegistry.MAGIC_SHAMANIC_NORDIC_02.get());
        sounds.add(SoundRegistry.MAGIC_SHAMANIC_NORDIC_02.get());
        world.playSound(null, actor.getBlockPos(), sounds.get(Math.clamp(amplifier, 0, sounds.size() - 1)),
                actor.getSoundCategory(), 0.3f, 1.3f);
        if (target != null) HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.SMOKE, actor, target, 20);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return 20;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof LivingEntity livingEntity) Phase10WeaponManager.tickDreadtide(livingEntity, stack);
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
        appendAbilityCooldownTooltip(tooltip, itemStack, 20);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("\uA999 ").append(Text.translatable("item.simplyswords.dreadtidesworditem.tooltip12").setStyle(Styles.CORRUPTED)));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "eldritch");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(() -> Registries.ITEM.get(Identifier.of(SimplySwords.MOD_ID, "dreadtide"))));
        }

        @ValidatedFloat.Restrict(min = 0)
        public float damageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0)
        public float spellScaling = 4.03f;
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
