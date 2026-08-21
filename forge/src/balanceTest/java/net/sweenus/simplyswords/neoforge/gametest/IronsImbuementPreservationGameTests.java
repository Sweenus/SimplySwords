package net.sweenus.simplyswords.neoforge.gametest;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.StackReplacement;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public final class IronsImbuementPreservationGameTests {
    @GameTest(templateName = "balance_empty", tickLimit = 20)
    @EmptyTemplate(value = "4x4x4")
    @TestHolder("simplyswords.irons_imbuement_preservation")
    public static void componentPreservingReplacementKeepsImbuement(TestContext context) {
        ItemStack source = new ItemStack(ItemsRegistry.DORMANT_RELIC.get());
        AwakeningApi.setLevel(source, 4);
        GemPowerComponent gems = GemPowerComponent.createEmpty(true, true);
        source.set(ComponentTypeRegistry.GEM_POWER.get(), gems);
        ISpellContainer before = ISpellContainer.createImbuedContainer(
                SpellRegistry.FIREBALL_SPELL.get(), 3, source);
        SpellSlot beforeSpell = before.getActiveSpells().getFirst();

        ItemStack result = StackReplacement.copyTo(source, ItemsRegistry.MAGIBLADE.get());
        ISpellContainer after = ISpellContainer.get(result);

        context.assertTrue(ISpellContainer.isSpellContainer(result), "Iron's spell container was lost");
        context.assertTrue(after != null, "Iron's spell container could not be read");
        context.assertTrue(after.isSpellWheel() == before.isSpellWheel(), "Spell-wheel behavior changed");
        context.assertTrue(after.mustEquip() == before.mustEquip(), "Equip behavior changed");
        context.assertTrue(after.getActiveSpellCount() == 1, "Active spell count changed");
        SpellSlot afterSpell = after.getActiveSpells().getFirst();
        context.assertTrue(afterSpell.getSpell() == beforeSpell.getSpell(), "Active spell changed");
        context.assertTrue(afterSpell.getLevel() == beforeSpell.getLevel(), "Spell level changed");
        context.assertTrue(AwakeningApi.getLevel(result) == 4, "Awakening level changed");
        context.assertTrue(gems.equals(result.get(ComponentTypeRegistry.GEM_POWER.get())), "Gem data changed");
        context.complete();
    }
}
