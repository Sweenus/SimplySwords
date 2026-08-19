package net.sweenus.simplyswords.neoforge;

import dev.architectury.platform.Platform;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.entity.LivingEntity;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;

public class ForgeHelperMethods {
    public static String spellSchoolDisplayKey(String magicSchool) {
        String school = magicSchool == null ? "" : magicSchool;
        String name;
        if (school.contains("lightning")) name = "scaleLightning";
        else if (school.contains("fire")) name = "scaleFire";
        else if (school.contains("frost")) name = "scaleIce";
        else if (school.contains("arcane")) name = "scaleEnder";
        else if (school.contains("soul")) name = "scaleBlood";
        else if (school.contains("healing")) name = "scaleHoly";
        else if (school.contains("nature")) name = "scaleNature";
        else if (school.contains("evocation")) name = "scaleEvocation";
        else if (school.contains("eldritch")) name = "scaleEldritch";
        else name = "scaleEnder";
        return "item.simplyswords.compat." + name;
    }

    public static float useSpellAttributeScaling(float damageModifier, LivingEntity player, String magicSchool) {
        if (Platform.isForgeLike() && SimplySwords.passVersionCheck("irons_spellbooks", SimplySwords.minimumSpellbookVersion)) {
            if (player != null && !player.getWorld().isClient) {
                double spellPower = player.getAttributes().hasAttribute(AttributeRegistry.SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.SPELL_POWER) : 1.f;
                double attributePower = 1.f;

                // Iron's spell-power attributes are multiplicative and have a neutral value of 1.

                if (magicSchool.contains("lightning"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.LIGHTNING_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.LIGHTNING_SPELL_POWER) : 1.f;
                else if (magicSchool.contains("fire"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.FIRE_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.FIRE_SPELL_POWER) : 1.f;
                else if (magicSchool.contains("frost"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.ICE_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.ICE_SPELL_POWER) : 1.f;
                else if (magicSchool.contains("arcane"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.ENDER_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.ENDER_SPELL_POWER) : 1.f;
                else if (magicSchool.contains("soul")) //there is no equivalent to soul school in spellbook, blood is closest
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.BLOOD_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.BLOOD_SPELL_POWER) : 1.f;
                else if (magicSchool.contains("healing"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.HOLY_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.HOLY_SPELL_POWER) : 1.f;
                else if (magicSchool.contains("nature"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.NATURE_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.NATURE_SPELL_POWER) : 1.f;
                else if (magicSchool.contains("evocation"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.EVOCATION_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.EVOCATION_SPELL_POWER) : 1.f;
                else if (magicSchool.contains("eldritch"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.ELDRITCH_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.ELDRITCH_SPELL_POWER) : 1.f;

                return (float) (damageModifier
                        * Math.max(0.f, Config.general.ironsSpellBasePower)
                        * spellPower
                        * attributePower);
            }
        }
        return 0;
    }
}
