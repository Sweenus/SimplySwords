package net.sweenus.simplyswords.forge.compat;

import net.minecraft.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.forge.item.PrometheumSwordItem;
import net.sweenus.simplyswords.item.ModToolMaterial;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;

public class MythicMetalsCompat {
   static float longsword_positive_modifier;
   static float twinblade_positive_modifier;
   static float rapier_positive_modifier;
   static float katana_positive_modifier;
   static float sai_positive_modifier;
   static float spear_positive_modifier;
   static float glaive_positive_modifier;
   static float warglaive_positive_modifier;
   static float cutlass_positive_modifier;
   static float claymore_positive_modifier;
   static float greataxe_positive_modifier;
   static float greathammer_positive_modifier;
   static float chakram_positive_modifier;
   static float scythe_positive_modifier;
   static float halberd_positive_modifier;
   static float longsword_negative_modifier;
   static float twinblade_negative_modifier;
   static float rapier_negative_modifier;
   static float sai_negative_modifier;
   static float spear_negative_modifier;
   static float katana_negative_modifier;
   static float glaive_negative_modifier;
   static float warglaive_negative_modifier;
   static float cutlass_negative_modifier;
   static float claymore_negative_modifier;
   static float greataxe_negative_modifier;
   static float greathammer_negative_modifier;
   static float chakram_negative_modifier;
   static float scythe_negative_modifier;
   static float halberd_negative_modifier;
   static float longsword_attackspeed;
   static float twinblade_attackspeed;
   static float rapier_attackspeed;
   static float sai_attackspeed;
   static float spear_attackspeed;
   static float katana_attackspeed;
   static float glaive_attackspeed;
   static float warglaive_attackspeed;
   static float cutlass_attackspeed;
   static float claymore_attackspeed;
   static float greataxe_attackspeed;
   static float greathammer_attackspeed;
   static float chakram_attackspeed;
   static float scythe_attackspeed;
   static float halberd_attackspeed;
   static int longsword_modifier;
   static int twinblade_modifier;
   static int rapier_modifier;
   static int sai_modifier;
   static int spear_modifier;
   static int katana_modifier;
   static int glaive_modifier;
   static int warglaive_modifier;
   static int cutlass_modifier;
   static int chakram_modifier;
   static int scythe_modifier;
   static int claymore_modifier;
   static int greathammer_modifier;
   static int greataxe_modifier;
   static int halberd_modifier;
   static int adamantite_modifier;
   static int aquarium_modifier;
   static int banglum_modifier;
   static int carmot_modifier;
   static int kyber_modifier;
   static int mythril_modifier;
   static int orichalcum_modifier;
   static int durasteel_modifier;
   static int osmium_modifier;
   static int prometheum_modifier;
   static int quadrillum_modifier;
   static int runite_modifier;
   static int star_platinum_modifier;
   static int bronze_modifier;
   static int copper_modifier;
   static int steel_modifier;
   static int palladium_modifier;
   static int stormyx_modifier;
   static int celestium_modifier;
   static int metallurgium_modifier;
   
   public static final DeferredRegister<Item> MYTHICMETALS_ITEM;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> ADAMANTITE_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> AQUARIUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> BANGLUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> CARMOT_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> KYBER_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> MYTHRIL_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> ORICHALCUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> OSMIUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> PROMETHEUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> QUADRILLUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> RUNITE_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> STAR_PLATINUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> BRONZE_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> STEEL_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> STORMYX_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> PALLADIUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> METALLURGIUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_TWINBLADE;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_RAPIER;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_KATANA;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_SAI;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_SPEAR;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_GLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_WARGLAIVE;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_CUTLASS;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_CLAYMORE;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_GREATHAMMER;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_GREATAXE;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_CHAKRAM;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_SCYTHE;
   public static final RegistryObject<SimplySwordsSwordItem> CELESTIUM_HALBERD;
   public static final RegistryObject<SimplySwordsSwordItem> COPPER_LONGSWORD;
   public static final RegistryObject<SimplySwordsSwordItem> DURASTEEL_GREATHAMMER;

   //private static RegistryObject<SimplySwordsSwordItem> MYTHICMETALS_ITEM.register(String name, RegistryObject<SimplySwordsSwordItem> item) {
   //   return (RegistryObject<SimplySwordsSwordItem>)class_2378.method_10230(class_7923.field_41178, () -> {
   //      return new class_2960("simplyswords", name), item);
   //}

   public static void registerModItems() {
      SimplySwords.LOGGER.info("Registering Mythic Metals compat Items for simplyswords");
   }

   static {
      longsword_positive_modifier = SimplySwords.weaponAttributesConfig.longsword_positiveDamageModifier;
      twinblade_positive_modifier = SimplySwords.weaponAttributesConfig.twinblade_positiveDamageModifier;
      rapier_positive_modifier = SimplySwords.weaponAttributesConfig.rapier_positiveDamageModifier;
      katana_positive_modifier = SimplySwords.weaponAttributesConfig.katana_positiveDamageModifier;
      sai_positive_modifier = SimplySwords.weaponAttributesConfig.sai_positiveDamageModifier;
      spear_positive_modifier = SimplySwords.weaponAttributesConfig.spear_positiveDamageModifier;
      glaive_positive_modifier = SimplySwords.weaponAttributesConfig.glaive_positiveDamageModifier;
      warglaive_positive_modifier = SimplySwords.weaponAttributesConfig.warglaive_positiveDamageModifier;
      cutlass_positive_modifier = SimplySwords.weaponAttributesConfig.cutlass_positiveDamageModifier;
      claymore_positive_modifier = SimplySwords.weaponAttributesConfig.claymore_positiveDamageModifier;
      greataxe_positive_modifier = SimplySwords.weaponAttributesConfig.greataxe_positiveDamageModifier;
      greathammer_positive_modifier = SimplySwords.weaponAttributesConfig.greathammer_positiveDamageModifier;
      chakram_positive_modifier = SimplySwords.weaponAttributesConfig.chakram_positiveDamageModifier;
      scythe_positive_modifier = SimplySwords.weaponAttributesConfig.scythe_positiveDamageModifier;
      halberd_positive_modifier = SimplySwords.weaponAttributesConfig.halberd_positiveDamageModifier;
      longsword_negative_modifier = SimplySwords.weaponAttributesConfig.longsword_negativeDamageModifier;
      twinblade_negative_modifier = SimplySwords.weaponAttributesConfig.twinblade_negativeDamageModifier;
      rapier_negative_modifier = SimplySwords.weaponAttributesConfig.rapier_negativeDamageModifier;
      sai_negative_modifier = SimplySwords.weaponAttributesConfig.sai_negativeDamageModifier;
      spear_negative_modifier = SimplySwords.weaponAttributesConfig.spear_negativeDamageModifier;
      katana_negative_modifier = SimplySwords.weaponAttributesConfig.katana_negativeDamageModifier;
      glaive_negative_modifier = SimplySwords.weaponAttributesConfig.glaive_negativeDamageModifier;
      warglaive_negative_modifier = SimplySwords.weaponAttributesConfig.warglaive_negativeDamageModifier;
      cutlass_negative_modifier = SimplySwords.weaponAttributesConfig.cutlass_negativeDamageModifier;
      claymore_negative_modifier = SimplySwords.weaponAttributesConfig.claymore_negativeDamageModifier;
      greataxe_negative_modifier = SimplySwords.weaponAttributesConfig.greataxe_negativeDamageModifier;
      greathammer_negative_modifier = SimplySwords.weaponAttributesConfig.greathammer_negativeDamageModifier;
      chakram_negative_modifier = SimplySwords.weaponAttributesConfig.chakram_negativeDamageModifier;
      scythe_negative_modifier = SimplySwords.weaponAttributesConfig.scythe_negativeDamageModifier;
      halberd_negative_modifier = SimplySwords.weaponAttributesConfig.halberd_negativeDamageModifier;
      longsword_attackspeed = SimplySwords.weaponAttributesConfig.longsword_attackSpeed;
      twinblade_attackspeed = SimplySwords.weaponAttributesConfig.twinblade_attackSpeed;
      rapier_attackspeed = SimplySwords.weaponAttributesConfig.rapier_attackSpeed;
      sai_attackspeed = SimplySwords.weaponAttributesConfig.sai_attackSpeed;
      spear_attackspeed = SimplySwords.weaponAttributesConfig.spear_attackSpeed;
      katana_attackspeed = SimplySwords.weaponAttributesConfig.katana_attackSpeed;
      glaive_attackspeed = SimplySwords.weaponAttributesConfig.glaive_attackSpeed;
      warglaive_attackspeed = SimplySwords.weaponAttributesConfig.warglaive_attackSpeed;
      cutlass_attackspeed = SimplySwords.weaponAttributesConfig.cutlass_attackSpeed;
      claymore_attackspeed = SimplySwords.weaponAttributesConfig.claymore_attackSpeed;
      greataxe_attackspeed = SimplySwords.weaponAttributesConfig.greataxe_attackSpeed;
      greathammer_attackspeed = SimplySwords.weaponAttributesConfig.greathammer_attackSpeed;
      chakram_attackspeed = SimplySwords.weaponAttributesConfig.chakram_attackSpeed;
      scythe_attackspeed = SimplySwords.weaponAttributesConfig.scythe_attackSpeed;
      halberd_attackspeed = SimplySwords.weaponAttributesConfig.halberd_attackSpeed;
      
	  longsword_modifier = (int)(longsword_positive_modifier - longsword_negative_modifier);
      twinblade_modifier = (int)(twinblade_positive_modifier - twinblade_negative_modifier);
      rapier_modifier = (int)(rapier_positive_modifier - rapier_negative_modifier);
      sai_modifier = (int)(sai_positive_modifier - sai_negative_modifier);
      spear_modifier = (int)(spear_positive_modifier - spear_negative_modifier);
      katana_modifier = (int)(katana_positive_modifier - katana_negative_modifier);
      glaive_modifier = (int)(glaive_positive_modifier - glaive_negative_modifier);
      warglaive_modifier = (int)(warglaive_positive_modifier - warglaive_negative_modifier);
      cutlass_modifier = (int)(cutlass_positive_modifier - cutlass_negative_modifier);
      chakram_modifier = (int)(chakram_positive_modifier - chakram_negative_modifier);
      scythe_modifier = (int)(scythe_positive_modifier - scythe_negative_modifier);
      claymore_modifier = (int)(claymore_positive_modifier - claymore_negative_modifier);
      greathammer_modifier = (int)(greathammer_positive_modifier - greathammer_negative_modifier);
      greataxe_modifier = (int)(greataxe_positive_modifier - greataxe_negative_modifier);
      halberd_modifier = (int)(halberd_positive_modifier - halberd_negative_modifier);
      adamantite_modifier = (int)Config.getFloat("adamantite_damageModifier", "WeaponAttributes", ConfigDefaultValues.adamantite_damageModifier);
      aquarium_modifier = (int)Config.getFloat("aquarium_damageModifier", "WeaponAttributes", ConfigDefaultValues.aquarium_damageModifier);
      banglum_modifier = (int)Config.getFloat("banglum_damageModifier", "WeaponAttributes", ConfigDefaultValues.banglum_damageModifier);
      carmot_modifier = (int)Config.getFloat("carmot_damageModifier", "WeaponAttributes", ConfigDefaultValues.carmot_damageModifier);
      kyber_modifier = (int)Config.getFloat("kyber_damageModifier", "WeaponAttributes", ConfigDefaultValues.kyber_damageModifier);
      mythril_modifier = (int)Config.getFloat("mythril_damageModifier", "WeaponAttributes", ConfigDefaultValues.mythril_damageModifier);
      orichalcum_modifier = (int)Config.getFloat("orichalcum_damageModifier", "WeaponAttributes", ConfigDefaultValues.orichalcum_damageModifier);
      durasteel_modifier = (int)Config.getFloat("durasteel_damageModifier", "WeaponAttributes", ConfigDefaultValues.durasteel_damageModifier);
      osmium_modifier = (int)Config.getFloat("osmium_damageModifier", "WeaponAttributes", ConfigDefaultValues.osmium_damageModifier);
      prometheum_modifier = (int)Config.getFloat("prometheum_damageModifier", "WeaponAttributes", ConfigDefaultValues.prometheum_damageModifier);
      quadrillum_modifier = (int)Config.getFloat("quadrillum_damageModifier", "WeaponAttributes", ConfigDefaultValues.quadrillum_damageModifier);
      runite_modifier = (int)Config.getFloat("runite_damageModifier", "WeaponAttributes", ConfigDefaultValues.runite_damageModifier);
      star_platinum_modifier = (int)Config.getFloat("starPlatinum_damageModifier", "WeaponAttributes", ConfigDefaultValues.starPlatinum_damageModifier);
      bronze_modifier = (int)Config.getFloat("bronze_damageModifier", "WeaponAttributes", ConfigDefaultValues.bronze_damageModifier);
      copper_modifier = (int)Config.getFloat("copper_damageModifier", "WeaponAttributes", ConfigDefaultValues.copper_damageModifier);
      steel_modifier = (int)Config.getFloat("steel_damageModifier", "WeaponAttributes", ConfigDefaultValues.steel_damageModifier);
      palladium_modifier = (int)Config.getFloat("palladium_damageModifier", "WeaponAttributes", ConfigDefaultValues.palladium_damageModifier);
      stormyx_modifier = (int)Config.getFloat("stormyx_damageModifier", "WeaponAttributes", ConfigDefaultValues.stormyx_damageModifier);
      celestium_modifier = (int)Config.getFloat("celestium_damageModifier", "WeaponAttributes", ConfigDefaultValues.celestium_damageModifier);
      metallurgium_modifier = (int)Config.getFloat("metallurgium_damageModifier", "WeaponAttributes", ConfigDefaultValues.metallurgium_damageModifier);
      
	  MYTHICMETALS_ITEM = DeferredRegister.create(ForgeRegistries.ITEMS, "simplyswords");
	  ADAMANTITE_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + longsword_modifier, longsword_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + twinblade_modifier, twinblade_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + rapier_modifier, rapier_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + katana_modifier, katana_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + sai_modifier, sai_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + spear_modifier, spear_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + glaive_modifier, glaive_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + warglaive_modifier, warglaive_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + cutlass_modifier, cutlass_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + claymore_modifier, claymore_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + greathammer_modifier, greathammer_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + greataxe_modifier, greataxe_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + chakram_modifier, chakram_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + scythe_modifier, scythe_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      ADAMANTITE_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/adamantite/adamantite_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + halberd_modifier, halberd_attackspeed, new String[]{"mythicmetals:adamantite_ingot"});});
      AQUARIUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + longsword_modifier, longsword_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + twinblade_modifier, twinblade_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + rapier_modifier, rapier_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + katana_modifier, katana_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + sai_modifier, sai_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + spear_modifier, spear_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + glaive_modifier, glaive_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + warglaive_modifier, warglaive_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + cutlass_modifier, cutlass_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + claymore_modifier, claymore_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + greathammer_modifier, greathammer_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + greataxe_modifier, greataxe_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + chakram_modifier, chakram_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + scythe_modifier, scythe_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      AQUARIUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/aquarium/aquarium_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + halberd_modifier, halberd_attackspeed, new String[]{"mythicmetals:aquarium_ingot"});});
      BANGLUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + longsword_modifier, longsword_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + twinblade_modifier, twinblade_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + rapier_modifier, rapier_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + katana_modifier, katana_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + sai_modifier, sai_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + spear_modifier, spear_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + glaive_modifier, glaive_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + warglaive_modifier, warglaive_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + cutlass_modifier, cutlass_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + claymore_modifier, claymore_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + greathammer_modifier, greathammer_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + greataxe_modifier, greataxe_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + chakram_modifier, chakram_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + scythe_modifier, scythe_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      BANGLUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/banglum/banglum_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + halberd_modifier, halberd_attackspeed, new String[]{"mythicmetals:banglum_ingot"});});
      CARMOT_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + longsword_modifier, longsword_attackspeed, new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + twinblade_modifier, twinblade_attackspeed, new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + rapier_modifier, rapier_attackspeed, new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      CARMOT_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/carmot/carmot_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:carmot_ingot"});});
      KYBER_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      KYBER_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/kyber/kyber_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:kyber_ingot"});});
      MYTHRIL_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      MYTHRIL_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/mythril/mythril_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:mythril_ingot"});});
      ORICHALCUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      ORICHALCUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/orichalcum/orichalcum_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:orichalcum_ingot"});});
      OSMIUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      OSMIUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/osmium/osmium_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:osmium_ingot"});});
      PROMETHEUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_longsword", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_twinblade", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_rapier", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_katana", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_sai", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_spear", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_glaive", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_warglaive", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_cutlass", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_claymore", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_greathammer", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_greataxe", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_chakram", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_scythe", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      PROMETHEUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/prometheum/prometheum_halberd", () -> {
         return new PrometheumSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:prometheum_ingot"});});
      QUADRILLUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      QUADRILLUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/quadrillum/quadrillum_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:quadrillum_ingot"});});
      RUNITE_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      RUNITE_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/runite/runite_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:runite_ingot"});});
      STAR_PLATINUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      STAR_PLATINUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/star_platinum/star_platinum_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:star_platinum"});});
      BRONZE_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      BRONZE_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/bronze/bronze_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:bronze_ingot"});});
      STEEL_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STEEL_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/steel/steel_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STEEL, steel_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:steel_ingot"});});
      STORMYX_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      STORMYX_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/stormyx/stormyx_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.STORMYX, stormyx_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:stormyx_ingot"});});
      PALLADIUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      PALLADIUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/palladium/palladium_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.PALLADIUM, palladium_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:palladium_ingot"});});
      METALLURGIUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      METALLURGIUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/metallurgium/metallurgium_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.METALLURGIUM, metallurgium_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:metallurgium_ingot"});});
      CELESTIUM_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_TWINBLADE = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_twinblade", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + twinblade_modifier, twinblade_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_RAPIER = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_rapier", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + rapier_modifier, rapier_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_KATANA = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_katana", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + katana_modifier, katana_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_SAI = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_sai", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + sai_modifier, sai_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_SPEAR = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_spear", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + spear_modifier, spear_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_GLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_glaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + glaive_modifier, glaive_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_WARGLAIVE = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_warglaive", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + warglaive_modifier, warglaive_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_CUTLASS = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_cutlass", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + cutlass_modifier, cutlass_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_CLAYMORE = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_claymore", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + claymore_modifier, claymore_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_GREATAXE = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_greataxe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + greataxe_modifier, greataxe_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_CHAKRAM = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_chakram", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + chakram_modifier, chakram_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_SCYTHE = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_scythe", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + scythe_modifier, scythe_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      CELESTIUM_HALBERD = MYTHICMETALS_ITEM.register("mythicmetals_compat/celestium/celestium_halberd", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.CELESTIUM, celestium_modifier + halberd_modifier, halberd_attackspeed,  new String[]{"mythicmetals:celestium_ingot"});});
      COPPER_LONGSWORD = MYTHICMETALS_ITEM.register("mythicmetals_compat/copper/copper_longsword", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.COPPER, copper_modifier + longsword_modifier, longsword_attackspeed,  new String[]{"mythicmetals:copper_ingot"});});
      DURASTEEL_GREATHAMMER = MYTHICMETALS_ITEM.register("mythicmetals_compat/durasteel/durasteel_greathammer", () -> {
         return new SimplySwordsSwordItem(ModToolMaterial.DURASTEEL, durasteel_modifier + greathammer_modifier, greathammer_attackspeed,  new String[]{"mythicmetals:durasteel_ingot"});});
   }
}
