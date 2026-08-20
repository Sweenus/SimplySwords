package net.sweenus.simplyswords.config;

import com.google.common.collect.ImmutableMap;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.custom.*;

public class UniqueEffectsConfig extends Config {

    public UniqueEffectsConfig() {
        super(Identifier.of(SimplySwords.MOD_ID, "unique_effects"));
    }


    // Mana drawn from Iron's Spells when an active ability fires. 0 disables the cost.
    public ValidatedIdentifierMap<Integer> weaponManaCosts = new ValidatedIdentifierMap.Builder<Integer>()
            .keyHandler(ValidatedIdentifier.ofRegistry(Identifier.of(SimplySwords.MOD_ID, "emberblade"), Registries.ITEM))
            .valueHandler(new ValidatedInt(0, 10000, 0))
            .defaults(
                    ImmutableMap.<Identifier, Integer>builder()
                            .put(Identifier.of(SimplySwords.MOD_ID, "arcanethyst"), 45)
                            .put(Identifier.of(SimplySwords.MOD_ID, "awakened_lichblade"), 55)
                            .put(Identifier.of(SimplySwords.MOD_ID, "bloodwake"), 15)
                            .put(Identifier.of(SimplySwords.MOD_ID, "bramblethorn"), 45)
                            .put(Identifier.of(SimplySwords.MOD_ID, "brimstone_claymore"), 45)
                            .put(Identifier.of(SimplySwords.MOD_ID, "caelestis"), 150)
                            .put(Identifier.of(SimplySwords.MOD_ID, "chompolotl"), 43)
                            .put(Identifier.of(SimplySwords.MOD_ID, "dawnquiver"), 45)
                            .put(Identifier.of(SimplySwords.MOD_ID, "dreadtide"), 0)
                            .put(Identifier.of(SimplySwords.MOD_ID, "dreadwhisper"), 45)
                            .put(Identifier.of(SimplySwords.MOD_ID, "emberblade"), 15)
                            .put(Identifier.of(SimplySwords.MOD_ID, "emberlash"), 20)
                            .put(Identifier.of(SimplySwords.MOD_ID, "enigma"), 100)
                            .put(Identifier.of(SimplySwords.MOD_ID, "flamewind"), 30)
                            .put(Identifier.of(SimplySwords.MOD_ID, "frostfall"), 17)
                            .put(Identifier.of(SimplySwords.MOD_ID, "gloampiercer"), 45)
                            .put(Identifier.of(SimplySwords.MOD_ID, "harbinger"), 67)
                            .put(Identifier.of(SimplySwords.MOD_ID, "hearthflame"), 25)
                            .put(Identifier.of(SimplySwords.MOD_ID, "hiveheart"), 80)
                            .put(Identifier.of(SimplySwords.MOD_ID, "icewhisper"), 45)
                            .put(Identifier.of(SimplySwords.MOD_ID, "ionbound_stormscale"), 25)
                            .put(Identifier.of(SimplySwords.MOD_ID, "livyatan"), 10)
                            .put(Identifier.of(SimplySwords.MOD_ID, "magiblade"), 15)
                            .put(Identifier.of(SimplySwords.MOD_ID, "magiscythe"), 77)
                            .put(Identifier.of(SimplySwords.MOD_ID, "magispear"), 35)
                            .put(Identifier.of(SimplySwords.MOD_ID, "mjolnir"), 55)
                            .put(Identifier.of(SimplySwords.MOD_ID, "molten_edge"), 10)
                            .put(Identifier.of(SimplySwords.MOD_ID, "ribboncleaver"), 10)
                            .put(Identifier.of(SimplySwords.MOD_ID, "riftmane"), 60)
                            .put(Identifier.of(SimplySwords.MOD_ID, "shadowsting"), 70)
                            .put(Identifier.of(SimplySwords.MOD_ID, "soulkeeper"), 40)
                            .put(Identifier.of(SimplySwords.MOD_ID, "soulpyre"), 115)
                            .put(Identifier.of(SimplySwords.MOD_ID, "soulrender"), 10)
                            .put(Identifier.of(SimplySwords.MOD_ID, "soulstalker"), 100)
                            .put(Identifier.of(SimplySwords.MOD_ID, "soulstealer"), 60)
                            .put(Identifier.of(SimplySwords.MOD_ID, "stars_edge"), 52)
                            .put(Identifier.of(SimplySwords.MOD_ID, "stormbringer"), 10)
                            .put(Identifier.of(SimplySwords.MOD_ID, "storms_edge"), 30)
                            .put(Identifier.of(SimplySwords.MOD_ID, "stormscale"), 25)
                            .put(Identifier.of(SimplySwords.MOD_ID, "sunfire"), 67)
                            .put(Identifier.of(SimplySwords.MOD_ID, "tempest"), 20)
                            .put(Identifier.of(SimplySwords.MOD_ID, "the_devourer"), 60)
                            .put(Identifier.of(SimplySwords.MOD_ID, "thunderbrand"), 45)
                            .put(Identifier.of(SimplySwords.MOD_ID, "toxic_longsword"), 0)
                            .put(Identifier.of(SimplySwords.MOD_ID, "twisted_blade"), 20)
                            .put(Identifier.of(SimplySwords.MOD_ID, "waxweaver"), 40)
                            .put(Identifier.of(SimplySwords.MOD_ID, "whisperwind"), 40)
                            .put(Identifier.of(SimplySwords.MOD_ID, "wickpiercer"), 3)
                            .put(Identifier.of(SimplySwords.MOD_ID, "wraithfang"), 40)
                            .put(Identifier.of(SimplySwords.MOD_ID, "wraithmaw"), 47)
                            .build()
            ).build();

    public float abilityAbsorptionCap = 20f;
    public GloamSettings gloam = new GloamSettings();

    public HarbingerSwordItem.EffectSettings harbinger = new HarbingerSwordItem.EffectSettings();
    public ArcanethystSwordItem.EffectSettings arcanethyst = new ArcanethystSwordItem.EffectSettings();
    public CaelestisSwordItem.EffectSettings caelestis = new CaelestisSwordItem.EffectSettings();
    public BrambleSwordItem.EffectSettings bramblethorn = new BrambleSwordItem.EffectSettings();
    public BrimstoneClaymoreItem.EffectSettings brimstone_claymore = new BrimstoneClaymoreItem.EffectSettings();
    public StarsEdgeSwordItem.EffectSettings stars_edge = new StarsEdgeSwordItem.EffectSettings();
    public EmberIreSwordItem.EffectSettings emberblade = new EmberIreSwordItem.EffectSettings();
    public FlamewindSwordItem.EffectSettings flamewind = new FlamewindSwordItem.EffectSettings();
    public EnigmaSwordItem.EffectSettings        enigma = new EnigmaSwordItem.EffectSettings();
    public WhisperwindSwordItem.EffectSettings whisperwind = new WhisperwindSwordItem.EffectSettings();
    public DreadwhisperSwordItem.EffectSettings dreadwhisper = new DreadwhisperSwordItem.EffectSettings();
    public TwistedBladeItem.EffectSettings twisted_blade = new TwistedBladeItem.EffectSettings();
    public WickpiercerSwordItem.EffectSettings wickpiercer = new WickpiercerSwordItem.EffectSettings();
    public GloampiercerSwordItem.EffectSettings gloampiercer = new GloampiercerSwordItem.EffectSettings();
    public FrostfallSwordItem.EffectSettings frostfall = new FrostfallSwordItem.EffectSettings();
    public LivyatanSwordItem.EffectSettings livyatan = new LivyatanSwordItem.EffectSettings();
    public HiveheartSwordItem.EffectSettings hiveheart = new HiveheartSwordItem.EffectSettings();
    public MagibladeSwordItem.EffectSettings     magiblade = new MagibladeSwordItem.EffectSettings();
    public MagispearSwordItem.EffectSettings magispear = new MagispearSwordItem.EffectSettings();
    public MagiscytheSwordItem.EffectSettings magiscythe = new MagiscytheSwordItem.EffectSettings();
    public MoltenEdgeSwordItem.EffectSettings molten_edge = new MoltenEdgeSwordItem.EffectSettings();
    public IcewhisperSwordItem.EffectSettings icewhisper = new IcewhisperSwordItem.EffectSettings();
    public PlagueSwordItem.EffectSettings toxic_longsword = new PlagueSwordItem.EffectSettings();
    public RibboncleaverSwordItem.EffectSettings ribboncleaver = new RibboncleaverSwordItem.EffectSettings();
    public RiftmaneSwordItem.EffectSettings riftmane = new RiftmaneSwordItem.EffectSettings();
    public DawnquiverSwordItem.EffectSettings dawnquiver = new DawnquiverSwordItem.EffectSettings();
    public SunfireSwordItem.EffectSettings sunfire = new SunfireSwordItem.EffectSettings();
    public ShadowstingSwordItem.EffectSettings shadowsting = new ShadowstingSwordItem.EffectSettings();
    public StormbringerSwordItem.EffectSettings stormbringer = new StormbringerSwordItem.EffectSettings();
    public StormscaleSwordItem.EffectSettings stormscale = new StormscaleSwordItem.EffectSettings();
    public IonboundStormscaleSwordItem.EffectSettings ionbound_stormscale = new IonboundStormscaleSwordItem.EffectSettings();
    public BloodwakeSwordItem.EffectSettings bloodwake = new BloodwakeSwordItem.EffectSettings();
    public EmberlashSwordItem.EffectSettings emberlash = new EmberlashSwordItem.EffectSettings();
    public LichbladeSwordItem.EffectSettings lichblade = new LichbladeSwordItem.EffectSettings();
    public SoulkeeperSwordItem.EffectSettings    soulkeeper = new SoulkeeperSwordItem.EffectSettings();
    public SoulrenderSwordItem.EffectSettings soulrender = new SoulrenderSwordItem.EffectSettings();
    public SoulstalkerSwordItem.EffectSettings soulstalker = new SoulstalkerSwordItem.EffectSettings();
    public SoulPyreSwordItem.EffectSettings soulpyre = new SoulPyreSwordItem.EffectSettings();
    public StealSwordItem.EffectSettings soulstealer = new StealSwordItem.EffectSettings();
    public StormSwordItem.EffectSettings mjolnir = new StormSwordItem.EffectSettings();
    public StormsEdgeSwordItem.EffectSettings storms_edge = new StormsEdgeSwordItem.EffectSettings();
    public ThunderbrandSwordItem.EffectSettings thunderbrand = new ThunderbrandSwordItem.EffectSettings();
    public HearthflameSwordItem.EffectSettings hearthflame = new HearthflameSwordItem.EffectSettings();
    public TempestSwordItem.EffectSettings tempest = new TempestSwordItem.EffectSettings();
    public WatcherSwordItem.EffectSettings       watcher = new WatcherSwordItem.EffectSettings();
    public DevourerClaymoreItem.EffectSettings devourer = new DevourerClaymoreItem.EffectSettings();
    public WaxweaverSwordItem.EffectSettings waxweaver = new WaxweaverSwordItem.EffectSettings();
    public WraithfangSwordItem.EffectSettings wraithfang = new WraithfangSwordItem.EffectSettings();
    public WraithmawSwordItem.EffectSettings wraithmaw = new WraithmawSwordItem.EffectSettings();
    public ChompolotlSwordItem.EffectSettings chompolotl = new ChompolotlSwordItem.EffectSettings();

    public static final class GloamSettings extends ConfigSection {
        @ValidatedInt.Restrict(min = 10) public int exposureBuildTicks = 40;
        @ValidatedInt.Restrict(min = 1) public int exposureDecayTicks = 80;
        @ValidatedInt.Restrict(min = 0, max = 4) public int maximumSlowBonus = 3;
        @ValidatedInt.Restrict(min = 1) public int graspDuration = 75;
        @ValidatedInt.Restrict(min = 0) public int graspImmunityDuration = 70;
        @ValidatedDouble.Restrict(min = 0.25, max = 4.0) public double growthRadius = 2.5;
        @ValidatedInt.Restrict(min = 20) public int growthDuration = 160;
        @ValidatedInt.Restrict(min = 1) public int growthFadeDuration = 30;
        @ValidatedInt.Restrict(min = 1, max = 32) public int growthPatchCap = 8;
    }

    // eldritch end compat
    public ValidatedCondition<DreadtideSwordItem.EffectSettings> dreadtide = new ValidatedAny<>(new DreadtideSwordItem.EffectSettings())
            .toCondition(
                    () -> SimplySwords.passVersionCheck("eldritch_end", SimplySwords.minimumEldritchEndVersion),
                    Text.translatable("simplyswords.unique_effects.dreadtide.compat"),
					DreadtideSwordItem.EffectSettings::new
            ).withFailTitle(Text.translatable("simplyswords.unique_effects.dreadtide.compat.failTitle"));

//EffectSettings
        //much like the gem power settings, each setting block is stored within the sword item it's used for
        //These settings use the unique item themselves to provide a config context tooltip
        //example
            //Type declaration: A tooltip settings, so needs a supplier of TooltipAppender
            //Constructor
                //Items are not tooltip appenders, so I made a class ItemStackTooltipAppender to help
                //NOTE: the ::get part is very important. RegistrySuppliers are suppliers, but without the get you get load order issues
            //settings: work basically like the gem power configs.
                //note that here I've left all the sword-specific naming
                //so instead of using the basic "Cooldown", it still will say "Harbinger cooldown" and so on.
            //like gem powers, call like
                //int c = Config.uniqueEffects.abyssalStandard.cooldown;
        /*
        public static class EffectSettings extends TooltipSettings {

            public EffectSettings() {
                super(new ItemStackTooltipAppender(ItemsRegistry.HARBINGER::get));
            }

            @ValidatedInt.Restrict(min = 0, max = 100)
            public int chance = 15;
            @ValidatedInt.Restrict(min = 0)
            public int cooldown = 700;
            @ValidatedFloat.Restrict(min = 0f)
            public float damage = 3f;
            @ValidatedFloat.Restrict(min = 0f)
            public float spellScaling = 1.2f;

        }
        */
}
