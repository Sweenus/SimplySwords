package net.sweenus.simplyswords.neoforge;

import dev.architectury.platform.Platform;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.SimplySwords;

public class ForgeHelperMethods {
    public static float useSpellAttributeScaling(float damageModifier, PlayerEntity player, String magicSchool) {
        if (Platform.isForgeLike() && SimplySwords.passVersionCheck("irons_spellbooks", SimplySwords.minimumSpellbookVersion)) {
            if (player != null && !player.getWorld().isClient) {
                double spellPower = player.getAttributes().hasAttribute(AttributeRegistry.SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.SPELL_POWER) : 1.f;
                double attributePower = 0.f;
                double damageOutput = 0.1;

                // Fetch attributes (crit damage/chance is now handled internally in API via randomValue)

                if (magicSchool.contains("lightning"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.LIGHTNING_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.LIGHTNING_SPELL_POWER) : 0.f;
                else if (magicSchool.contains("fire"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.FIRE_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.FIRE_SPELL_POWER) : 0.f;
                else if (magicSchool.contains("frost"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.ICE_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.ICE_SPELL_POWER) : 0.f;
                else if (magicSchool.contains("arcane"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.ENDER_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.ENDER_SPELL_POWER) : 0.f;
                else if (magicSchool.contains("soul"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.BLOOD_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.ELDRITCH_SPELL_POWER) : 0.f;
                else if (magicSchool.contains("healing"))
                    attributePower = player.getAttributes().hasAttribute(AttributeRegistry.HOLY_SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.HOLY_SPELL_POWER) : 0.f;

                damageOutput = (damageModifier * (spellPower + attributePower));

                return (float) damageOutput;
            }
        }
        return 0;
    }
}