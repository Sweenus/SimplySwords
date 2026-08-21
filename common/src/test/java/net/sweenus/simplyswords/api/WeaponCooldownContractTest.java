package net.sweenus.simplyswords.api;

import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.item.UniqueWeaponItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponCooldownContractTest {

    @Test
    void activeAbilitiesUseSpellCooldownReductionByDefault() {
        UniqueWeaponActiveAbility ability = new UniqueWeaponActiveAbility() {
        };

        assertTrue(ability.usesSpellCooldownReduction(null));
    }

    @Test
    void activeAbilitiesCanExplicitlyOptOut() {
        UniqueWeaponActiveAbility ability = new UniqueWeaponActiveAbility() {
            @Override
            public boolean usesSpellCooldownReduction(ItemStack stack) {
                return false;
            }
        };

        assertFalse(ability.usesSpellCooldownReduction(null));
    }

    @Test
    void cooldownTooltipHelpersKeepLegacyAndStackAwareDescriptors() {
        assertDoesNotThrow(() -> {
            var legacy = UniqueWeaponItem.class.getDeclaredMethod(
                    "appendAbilityCooldownTooltip", List.class, int.class);
            var stackAware = UniqueWeaponItem.class.getDeclaredMethod(
                    "appendAbilityCooldownTooltip", List.class, ItemStack.class, int.class);
            assertTrue(Modifier.isProtected(legacy.getModifiers()));
            assertTrue(Modifier.isStatic(legacy.getModifiers()));
            assertTrue(Modifier.isProtected(stackAware.getModifiers()));
            assertTrue(Modifier.isStatic(stackAware.getModifiers()));
        });
    }
}
