package net.sweenus.simplyswords.fabric.mixin;

import net.minecraft.text.Style;
import net.sweenus.simplyswords.fabric.compat.MythicMetalsCompat;
import nourl.mythicmetals.abilities.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mixin to Mythic Metals Abilities.class to add our weapons and gain the special effects
// https://github.com/Noaaan/MythicMetals/blob/cbbb15b9f3dae18b8575e77f52b0fb5cff2f9e5f/src/main/java/nourl/mythicmetals/abilities/Abilities.java


@Mixin(Abilities.class)
public class MythicMetalsAbilityMixin {

    @Inject(at = @At("HEAD"), method = "init", remap = false)
    private static void init(CallbackInfo info) {

        //System.out.println("Adding Simply Swords weapons to Mythic Metals Ability list.");

        //Carmot
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_CHAKRAM, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_CLAYMORE, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_CUTLASS, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_GLAIVE, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_GREATAXE, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_GREATHAMMER, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_KATANA, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_LONGSWORD, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_RAPIER, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_SAI, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_SPEAR, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_TWINBLADE, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_WARGLAIVE, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_SCYTHE, Style.EMPTY.withColor(0xE63E73));
        Abilities.BONUS_LOOTING.addItem(MythicMetalsCompat.CARMOT_HALBERD, Style.EMPTY.withColor(0xE63E73));
        //Aquarium
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_CHAKRAM, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_CLAYMORE, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_CUTLASS, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_GLAIVE, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_GREATAXE, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_GREATHAMMER, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_KATANA, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_LONGSWORD, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_RAPIER, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_SAI, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_SPEAR, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_TWINBLADE, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_WARGLAIVE, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_SCYTHE, Style.EMPTY.withColor(0xE63E73));
        Abilities.AQUA_AFFINITY.addItem(MythicMetalsCompat.AQUARIUM_HALBERD, Style.EMPTY.withColor(0xE63E73));
        //Prometheum
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_CHAKRAM, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_CLAYMORE, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_CUTLASS, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_GLAIVE, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_GREATAXE, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_GREATHAMMER, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_KATANA, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_LONGSWORD, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_RAPIER, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_SAI, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_SPEAR, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_TWINBLADE, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_WARGLAIVE, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_SCYTHE, Style.EMPTY.withColor(0xE63E73));
        Abilities.MENDING.addItem(MythicMetalsCompat.PROMETHEUM_HALBERD, Style.EMPTY.withColor(0xE63E73));


    }
}
