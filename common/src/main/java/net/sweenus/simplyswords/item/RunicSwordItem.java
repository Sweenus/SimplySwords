package net.sweenus.simplyswords.item;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.ThrownSpearEntity;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.PowerType;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.TagRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class RunicSwordItem extends SwordItem {

    public RunicSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings.fireproof());
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if(!stack.contains(ComponentTypeRegistry.GEM_POWER.get())) {
            String[] blacklist = {"simplyswords:throwing"}; // Should be replaced with a modular blacklisting system at a later date
            if (TagRegistry.isInTag(TagRegistry.spearsTag, asItem()))
                stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.runic(GemPowerRegistry.gemRandomPower(PowerType.RUNIC, blacklist)));
            else stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.runic(GemPowerRegistry.gemRandomPower(PowerType.RUNIC)));
        }
        return false;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (!attacker.getWorld().isClient) {
            GemPowerComponent component = SimplySwordsAPI.getComponent(stack);
            HelperMethods.playHitSounds(attacker, target);
            component.postHit(stack, target, attacker);
        }

        return super.postHit(stack, target, attacker);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient) {
            GemPowerComponent component = SimplySwordsAPI.getComponent(stack);
            component.onStoppedUsing(stack, world, user, remainingUseTicks);
        }
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient) {
            GemPowerComponent component = SimplySwordsAPI.getComponent(stack);
            component.usageTick(world, user, stack, remainingUseTicks);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        GemPowerComponent component = SimplySwordsAPI.getComponent(stack);
        return component.getMaxUseTime(stack);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, Identifier.of(SimplySwords.MOD_ID, "spears"));
        if (itemStack.getItem().getRegistryEntry().isIn(tagKey)) {
            if (!world.isClient) {
                itemStack = user.getStackInHand(hand);
                ThrownSpearEntity thrownSwordEntity = new ThrownSpearEntity(world, user, itemStack.copy() );
                thrownSwordEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
                thrownSwordEntity.setYaw(user.getYaw());
                thrownSwordEntity.setPitch(user.getPitch()-90);
                double[] doubles = HelperMethods.getAttackFromSlot(user, itemStack, user.getActiveHand());
                thrownSwordEntity.primaryBaseDamage = (float) doubles[0];
                //System.out.println("Returned Attack value: " + (float) doubles[0]);
                thrownSwordEntity.hasLoyalty = MathHelper.clamp(EnchantmentHelper.getTridentReturnAcceleration((ServerWorld) world, itemStack, user), 0, 127);
                if (hand == Hand.OFF_HAND)
                    thrownSwordEntity.offhandThrow = true;
                thrownSwordEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
                world.spawnEntity(thrownSwordEntity);

                GemPowerComponent component = SimplySwordsAPI.getComponent(itemStack);

                if (!user.getAbilities().creativeMode) {
                    itemStack.decrement(1);
                }
                return component.use(world, user, hand);
            }

            user.swingHand(hand);

            user.getItemCooldownManager().set(this, 1);
            return TypedActionResult.success(itemStack, world.isClient());
        }

        if (itemStack.getDamage() < itemStack.getMaxDamage() - 1) {
            GemPowerComponent component = SimplySwordsAPI.getComponent(itemStack);
            return component.use(world, user, hand);
        }
        return TypedActionResult.fail(itemStack);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if ((entity instanceof LivingEntity user) && (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack || user.getEquippedStack(EquipmentSlot.OFFHAND) == stack)) {
            if (entity.age % 4 == 0 && Config.general.enablePassiveParticles) {
                float randomx = (float) (Math.random() * 6);
                float randomz = (float) (Math.random() * 6);

                world.addParticle(ParticleTypes.ENCHANT,
                        user.getX() + user.getHandPosOffset(this).getX(),
                        user.getY() + user.getHandPosOffset(this).getY() + 1.3,
                        user.getZ() + user.getHandPosOffset(this).getZ(),
                        -3 + randomx, 0.0, -3 + randomz);
            }
            if (!world.isClient) {
                GemPowerComponent component = SimplySwordsAPI.getComponent(stack);
                component.inventoryTick(stack, world, user, slot, selected);
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        if (world.isClient) return;

        if(!stack.contains(ComponentTypeRegistry.GEM_POWER.get())) {
            String[] blacklist = {"simplyswords:throwing"};
            if (TagRegistry.isInTag(TagRegistry.spearsTag, asItem()))
                stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.runic(GemPowerRegistry.gemRandomPower(PowerType.RUNIC, blacklist)));
            else stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.runic(GemPowerRegistry.gemRandomPower(PowerType.RUNIC)));
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).setStyle(Styles.RUNIC);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        generateDynamicTooltip(itemStack, tooltipContext, tooltip, type);
    }

    // Override this with your own id & paths
    protected void generateDynamicTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, tooltipContext, tooltip, type,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                null);
    }

}