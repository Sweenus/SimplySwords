package net.sweenus.simplyswords.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.combat.CombatProvenance;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class CombatItemProvenanceMixin {
    @WrapMethod(method = "use")
    private TypedActionResult<ItemStack> simplyswords$use(World world, PlayerEntity player, Hand hand,
                                                        Operation<TypedActionResult<ItemStack>> original) {
        try (var ignored = CombatProvenanceApi.origin(player, (ItemStack) (Object) this, CombatProvenance.ABILITY)) {
            return original.call(world, player, hand);
        }
    }

    @WrapMethod(method = "inventoryTick")
    private void simplyswords$inventory(World world, Entity entity, int slot, boolean selected, Operation<Void> original) {
        try (var ignored = CombatProvenanceApi.origin(entity instanceof LivingEntity living ? living : null,
                (ItemStack) (Object) this, CombatProvenance.ABILITY)) {
            original.call(world, entity, slot, selected);
        }
    }

    @WrapMethod(method = "postHit")
    private boolean simplyswords$postHit(LivingEntity target, PlayerEntity attacker, Operation<Boolean> original) {
        try (var ignored = CombatProvenanceApi.origin(attacker, (ItemStack) (Object) this, CombatProvenance.ABILITY)) {
            return original.call(target, attacker);
        }
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void simplyswords$snapshot(CallbackInfoReturnable<ItemStack> cir) {
        CombatProvenance origin = CombatProvenanceApi.current();
        ItemStack result = cir.getReturnValue();
        if (origin != null && !result.isEmpty()
                && CombatProvenanceApi.matches((ItemStack) (Object) this, origin)) {
            result.set(ComponentTypeRegistry.COMBAT_PROVENANCE.get(), origin);
        }
    }
}
