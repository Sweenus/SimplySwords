package net.sweenus.simplyswords.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_power.api.MagicSchool;
import net.spell_power.api.attributes.SpellAttributes;

import java.util.Random;

public class FabricHelperMethods {


    //Compatibility with Spell Power Attributes
    public static float useSpellAttributeScaling(float damageModifier, PlayerEntity player, String magicSchool) {
        if (FabricLoader.getInstance().isModLoaded("spell_power")) {
            if (player != null && !player.getWorld().isClient) {

                double attributePower = 0;
                double damageOutput = 0.1;

                // Fetch attributes
                double lightningPower = player.getAttributeValue(SpellAttributes.POWER.get(MagicSchool.LIGHTNING).attribute);
                double firePower =      player.getAttributeValue(SpellAttributes.POWER.get(MagicSchool.FIRE).attribute);
                double frostPower =     player.getAttributeValue(SpellAttributes.POWER.get(MagicSchool.FROST).attribute);
                double arcanePower =    player.getAttributeValue(SpellAttributes.POWER.get(MagicSchool.ARCANE).attribute);
                double soulPower =      player.getAttributeValue(SpellAttributes.POWER.get(MagicSchool.SOUL).attribute);
                double healingPower =   player.getAttributeValue(SpellAttributes.POWER.get(MagicSchool.HEALING).attribute);
                double critChance =     player.getAttributeValue(SpellAttributes.CRITICAL_CHANCE.attribute) - 100;
                double critDamage =     player.getAttributeValue(SpellAttributes.CRITICAL_DAMAGE.attribute) / 100;

                if (magicSchool.contains("lightning"))
                    attributePower = lightningPower;
                else if (magicSchool.contains("fire"))
                    attributePower = firePower;
                else if (magicSchool.contains("frost"))
                    attributePower = frostPower;
                else if (magicSchool.contains("arcane"))
                    attributePower = arcanePower;
                else if (magicSchool.contains("soul"))
                    attributePower = soulPower;
                else if (magicSchool.contains("healing"))
                    attributePower = healingPower;

                int random = new Random().nextInt(100);

                // Do math things
                if (random < critChance)
                    damageOutput = ((damageModifier * attributePower) * critDamage);
                else
                    damageOutput = (damageModifier * attributePower);

                return (float) damageOutput;
            }
        }
        return 0;
    }
}
