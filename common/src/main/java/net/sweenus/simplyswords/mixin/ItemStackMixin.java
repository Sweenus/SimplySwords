package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AdditionalGemSocketApi;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.GemPowerFiller;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "onClicked", at = @At("HEAD"))
    private void simplyswords$socketAdditionalGem(
            ItemStack otherStack,
            Slot slot,
            ClickType clickType,
            PlayerEntity player,
            StackReference cursorStackReference,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Runs on both sides on purpose: onClickedGemSocketLogic has a client-side branch
        // that keeps the displaced gem on the cursor in Creative mode. The GemPowerFiller
        // check keeps ensureInitialized off every unrelated click, and its writes are
        // conditional, so this does not churn components.
        ItemStack stack = (ItemStack) (Object) this;
        if (!(otherStack.getItem() instanceof GemPowerFiller)
                || !AdditionalGemSocketApi.ensureInitialized(stack)) {
            return;
        }
        SimplySwordsAPI.onClickedGemSocketLogic(
                stack,
                otherStack,
                player,
                cursorStackReference
        );
    }

    @Inject(method = "postHit", at = @At("RETURN"))
    private void simplyswords$triggerAdditionalGemPostHit(
            LivingEntity target,
            PlayerEntity attacker,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        if (attacker.getWorld().isClient()
                || !AdditionalGemSocketApi.ensureInitialized(stack)) {
            return;
        }
        SimplySwordsAPI.postHitGemSocketLogic(stack, target, attacker);
    }

    @Inject(method = "inventoryTick", at = @At("TAIL"))
    private void simplyswords$tickAdditionalGemPowers(
            World world,
            Entity entity,
            int slot,
            boolean selected,
            CallbackInfo ci
    ) {
        if (world.isClient() || !(entity instanceof LivingEntity user)) {
            return;
        }

        // Check that the stack is actually held before ensureInitialized, which writes
        // components. Stacks resting in chests, Refined Storage disks, Create vaults etc.
        // still get an inventory tick, and must not be touched.
        ItemStack stack = (ItemStack) (Object) this;
        if ((user.getMainHandStack() != stack && user.getOffHandStack() != stack)
                || !AdditionalGemSocketApi.ensureInitialized(stack)) {
            return;
        }

        GemPowerComponent component = stack.getOrDefault(
                ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.DEFAULT);
        component.inventoryTick(stack, world, user, slot, selected);
    }

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void simplyswords$appendAdditionalGemTooltip(
            Item.TooltipContext context,
            PlayerEntity player,
            TooltipType type,
            CallbackInfoReturnable<List<Text>> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!AdditionalGemSocketApi.isManaged(stack)) {
            return;
        }

        GemPowerComponent component = AdditionalGemSocketApi.getTooltipComponent(stack);
        List<Text> socketLines = new ArrayList<>();
        socketLines.add(Text.literal(""));
        component.appendTooltip(stack, context, socketLines, type);

        List<Text> tooltip = new ArrayList<>(cir.getReturnValue());
        String mainHandHeader = Text.translatable("item.modifiers.mainhand").getString();
        String offHandHeader = Text.translatable("item.modifiers.offhand").getString();
        int insertionIndex = tooltip.size();
        for (int i = 0; i < tooltip.size(); i++) {
            String line = tooltip.get(i).getString();
            if (mainHandHeader.equals(line) || offHandHeader.equals(line)) {
                insertionIndex = i;
                break;
            }
        }
        tooltip.addAll(insertionIndex, socketLines);
        cir.setReturnValue(tooltip);
    }
}
