package net.sweenus.simplyswords.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.api.combat.CombatProvenance;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntity.class)
public abstract class CombatAttackProvenanceMixin {
    @WrapMethod(method = "attack")
    private void simplyswords$attack(Entity target, Operation<Void> original) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        try (var ignored = CombatProvenanceApi.origin(player, player.getMainHandStack(), CombatProvenance.MELEE)) {
            original.call(target);
        }
    }
}
