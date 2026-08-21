package net.sweenus.simplyswords.gametest;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpellPowerWeaponAttributeGameTestSuite {
    private static final Identifier EMBERBLADE = id("emberblade");

    private SpellPowerWeaponAttributeGameTestSuite() {
    }

    public static void run(TestContext context) {
        var settings = Config.compatibility.spellPowerApi.getUnconditional();
        boolean previousToggle = settings.enableWeaponAttributes.get();
        Map<Identifier, Double> previousBonuses = new LinkedHashMap<>(
                settings.weaponSpellPowerBonuses);
        try {
            settings.enableWeaponAttributes.accept(true);
            setEmberbladeBonus(previousBonuses, 2.0D);

            List<Modifier> emberMain = modifiers(new ItemStack(ItemsRegistry.EMBERBLADE.get()), EquipmentSlot.MAINHAND);
            List<Modifier> emberOff = modifiers(new ItemStack(ItemsRegistry.EMBERBLADE.get()), EquipmentSlot.OFFHAND);
            assertModifier(context, emberMain, "fire", 2.0D);
            assertModifier(context, emberOff, "fire", 2.0D);
            context.assertTrue(!emberMain.getFirst().modifier().id().equals(emberOff.getFirst().modifier().id()),
                    "Main-hand and offhand modifiers must have different ids so dual wielding stacks");

            assertModifier(context,
                    modifiers(new ItemStack(ItemsRegistry.BRIMSTONE_CLAYMORE.get()), EquipmentSlot.MAINHAND),
                    "fire", 4.0D);

            List<Modifier> tempest = modifiers(
                    new ItemStack(ItemsRegistry.TEMPEST.get()), EquipmentSlot.OFFHAND);
            assertModifier(context, tempest, "frost", 2.0D);
            assertModifier(context, tempest, "fire", 2.0D);

            context.assertTrue(
                    modifiers(new ItemStack(ItemsRegistry.RIBBONCLEAVER.get()), EquipmentSlot.MAINHAND).isEmpty(),
                    "Physical unique received a Spell Power modifier");

            setEmberbladeBonus(previousBonuses, 0.0D);
            context.assertTrue(
                    modifiers(new ItemStack(ItemsRegistry.EMBERBLADE.get()), EquipmentSlot.MAINHAND).isEmpty(),
                    "Per-weapon zero did not disable the Spell Power modifier");

            setEmberbladeBonus(previousBonuses, 2.0D);
            settings.enableWeaponAttributes.accept(false);
            context.assertTrue(
                    modifiers(new ItemStack(ItemsRegistry.EMBERBLADE.get()), EquipmentSlot.MAINHAND).isEmpty(),
                    "Global toggle did not disable Spell Power weapon modifiers");

            context.complete();
        } finally {
            settings.enableWeaponAttributes.accept(previousToggle);
            settings.weaponSpellPowerBonuses.accept(previousBonuses);
        }
    }

    private static void setEmberbladeBonus(Map<Identifier, Double> base, double amount) {
        Map<Identifier, Double> updated = new LinkedHashMap<>(base);
        updated.put(EMBERBLADE, amount);
        Config.compatibility.spellPowerApi.getUnconditional().weaponSpellPowerBonuses.accept(updated);
    }

    private static List<Modifier> modifiers(ItemStack stack, EquipmentSlot slot) {
        List<Modifier> modifiers = new ArrayList<>();
        stack.applyAttributeModifiers(slot, (attribute, modifier) -> {
            Identifier attributeId = Registries.ATTRIBUTE.getId(attribute.value());
            if ("spell_power".equals(attributeId.getNamespace())) {
                modifiers.add(new Modifier(attributeId, modifier));
            }
        });
        return modifiers;
    }

    private static void assertModifier(TestContext context, List<Modifier> modifiers,
                                       String school, double amount) {
        Identifier expected = Identifier.of("spell_power", school);
        Modifier modifier = modifiers.stream()
                .filter(entry -> entry.attributeId().equals(expected))
                .findFirst()
                .orElse(null);
        context.assertTrue(modifier != null, "Missing " + expected + " weapon modifier");
        if (modifier != null) {
            context.assertTrue(Double.compare(modifier.modifier().value(), amount) == 0,
                    "Expected " + amount + " for " + expected + " but found " + modifier.modifier().value());
        }
    }

    private static Identifier id(String path) {
        return Identifier.of("simplyswords", path);
    }

    private record Modifier(Identifier attributeId, EntityAttributeModifier modifier) {
    }
}
