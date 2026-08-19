package net.sweenus.simplyswords.fabric;

import dev.architectury.platform.Platform;
import net.minecraft.entity.LivingEntity;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;
import net.sweenus.simplyswords.SimplySwords;

public class FabricHelperMethods {


    //Compatibility with Spell Power Attributes
    public static String spellSchoolDisplayKey(String magicSchool) {
        String school = magicSchool == null ? "" : magicSchool;
        String name;
        if (school.contains("lightning")) name = "scaleLightning";
        else if (school.contains("fire")) name = "scaleFire";
        else if (school.contains("frost")) name = "scaleFrost";
        else if (school.contains("arcane")) name = "scaleArcane";
        else if (school.contains("soul")) name = "scaleSoul";
        else if (school.contains("healing")) name = "scaleHealing";
        else if (school.contains("nature")) name = "scaleHealing";
        else if (school.contains("evocation")) name = "scaleArcane";
        else if (school.contains("eldritch")) name = "scaleSoul";
        else name = "scaleArcane";
        return "item.simplyswords.compat." + name;
    }

    public static float useSpellAttributeScaling(float damageModifier, LivingEntity player, String magicSchool) {
        if (Platform.isFabric() && SimplySwords.passVersionCheck("spell_power", SimplySwords.minimumSpellPowerVersion)) {
            if (player != null && !player.getWorld().isClient) {

                double attributePower = 0;
                double damageOutput = 0.1;

                // Fetch attributes (crit damage/chance is now handled internally in API via randomValue)

                if (magicSchool.contains("lightning"))
                    attributePower = SpellPower.getSpellPower(SpellSchools.LIGHTNING, player).randomValue();
                else if (magicSchool.contains("fire"))
                    attributePower = SpellPower.getSpellPower(SpellSchools.FIRE, player).randomValue();
                else if (magicSchool.contains("frost"))
                    attributePower = SpellPower.getSpellPower(SpellSchools.FROST, player).randomValue();
                else if (magicSchool.contains("arcane"))
                    attributePower = SpellPower.getSpellPower(SpellSchools.ARCANE, player).randomValue();
                else if (magicSchool.contains("soul"))
                    attributePower = SpellPower.getSpellPower(SpellSchools.SOUL, player).randomValue();
                else if (magicSchool.contains("healing"))
                    attributePower = SpellPower.getSpellPower(SpellSchools.HEALING, player).randomValue();
                else if (magicSchool.contains("nature"))
                    attributePower = SpellPower.getSpellPower(SpellSchools.HEALING, player).randomValue();
                else if (magicSchool.contains("evocation"))
                    attributePower = SpellPower.getSpellPower(SpellSchools.ARCANE, player).randomValue();
                else if (magicSchool.contains("eldritch"))
                    attributePower = SpellPower.getSpellPower(SpellSchools.SOUL, player).randomValue();


                damageOutput = (damageModifier * attributePower);

                return (float) damageOutput;
            }
        }
        return 0;
    }

}
