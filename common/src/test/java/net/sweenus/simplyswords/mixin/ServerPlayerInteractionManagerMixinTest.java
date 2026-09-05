package net.sweenus.simplyswords.mixin;

import net.sweenus.simplyswords.item.custom.StormscaleSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponSecondaryAction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerPlayerInteractionManagerMixinTest {

    @Test
    void cooldownPolicyHelperIsMixinSafe() throws NoSuchMethodException {
        Method method = policyMethod();

        assertTrue(Modifier.isPrivate(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    @Test
    void ordinaryItemsRemainBlockedDuringCooldown() throws ReflectiveOperationException {
        assertTrue(shouldBlockItemUse(true, false));
    }

    @Test
    void explicitSecondaryActionsCanReachTheirUsePathDuringCooldown() throws ReflectiveOperationException {
        assertFalse(shouldBlockItemUse(true, true));
        assertTrue(UniqueWeaponSecondaryAction.class.isAssignableFrom(StormscaleSwordItem.class));
    }

    @Test
    void itemsWithoutACooldownAreNeverBlocked() throws ReflectiveOperationException {
        assertFalse(shouldBlockItemUse(false, false));
        assertFalse(shouldBlockItemUse(false, true));
    }

    private static boolean shouldBlockItemUse(boolean coolingDown,
                                              boolean hasSecondaryAction) throws ReflectiveOperationException {
        return (boolean) policyMethod().invoke(null, coolingDown, hasSecondaryAction);
    }

    private static Method policyMethod() throws NoSuchMethodException {
        Method method = ServerPlayerInteractionManagerMixin.class.getDeclaredMethod(
                "simplyswords$shouldBlockItemUse", boolean.class, boolean.class);
        method.setAccessible(true);
        return method;
    }
}
