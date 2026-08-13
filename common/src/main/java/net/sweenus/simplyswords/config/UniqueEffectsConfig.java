package net.sweenus.simplyswords.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.custom.*;

public class UniqueEffectsConfig extends Config {

    public UniqueEffectsConfig() {
        super(new Identifier(SimplySwords.MOD_ID, "unique_effects"));
    }

    public float abilityAbsorptionCap = 20f;

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
    public TwistedBladeItem.EffectSettings twisted_blade = new TwistedBladeItem.EffectSettings();
    public WickpiercerSwordItem.EffectSettings wickpiercer = new WickpiercerSwordItem.EffectSettings();
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
    public SoulPyreSwordItem.EffectSettings soulpyre = new SoulPyreSwordItem.EffectSettings();
    public StealSwordItem.EffectSettings soulstealer = new StealSwordItem.EffectSettings();
    public StormSwordItem.EffectSettings mjolnir = new StormSwordItem.EffectSettings();
    public StormsEdgeSwordItem.EffectSettings storms_edge = new StormsEdgeSwordItem.EffectSettings();
    public ThunderbrandSwordItem.EffectSettings thunderbrand = new ThunderbrandSwordItem.EffectSettings();
    public HearthflameSwordItem.EffectSettings hearthflame = new HearthflameSwordItem.EffectSettings();
    public TempestSwordItem.EffectSettings tempest = new TempestSwordItem.EffectSettings();
    public WatcherSwordItem.EffectSettings       watcher = new WatcherSwordItem.EffectSettings();
    public WaxweaverSwordItem.EffectSettings waxweaver = new WaxweaverSwordItem.EffectSettings();
    public WraithfangSwordItem.EffectSettings wraithfang = new WraithfangSwordItem.EffectSettings();
    public ChompolotlSwordItem.EffectSettings chompolotl = new ChompolotlSwordItem.EffectSettings();

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
