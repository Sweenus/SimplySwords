package net.sweenus.simplyswords.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerWorld.class)
public abstract class CombatTickProvenanceMixin {
    @WrapMethod(method = "tickEntity")
    private void simplyswords$provenanceTick(Entity entity, Operation<Void> original) {
        try (var ignored = CombatProvenanceApi.scope(CombatProvenanceApi.entity(entity))) {
            original.call(entity);
        }
    }
}
