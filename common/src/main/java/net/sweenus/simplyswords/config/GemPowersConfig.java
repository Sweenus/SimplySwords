package net.sweenus.simplyswords.config;

import dev.architectury.platform.Platform;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedSet;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.power.powers.*;
import net.sweenus.simplyswords.registry.GemPowerRegistry;

public class GemPowersConfig extends Config {

	public GemPowersConfig() {
		super(Identifier.of(SimplySwords.MOD_ID, "gem_powers"));
	}

	@SuppressWarnings("deprecation")
	public ValidatedSet<Identifier> disabledPowers = ValidatedIdentifier.ofRegistryKey(GemPowerRegistry.REGISTRY.key()).toSet();

	public ActiveDefencePower.Settings activeDefence = new ActiveDefencePower.Settings();
	public FloatPower.Settings         floating = new FloatPower.Settings();
	public FreezePower.Settings        freeze = new FreezePower.Settings();
	public FrostWardPower.Settings     frostWard = new FrostWardPower.Settings();
	public DancingBladesPower.Settings dancingBlades = new DancingBladesPower.Settings();
	public ImbuedPower.Settings        imbued = new ImbuedPower.Settings();
	public ImmolationPower.Settings    immolation = new ImmolationPower.Settings();
	public RadiancePower.Settings      radiance = new RadiancePower.Settings();
	public MomentumPower.Settings      momentum = new MomentumPower.Settings();
	public ShieldingPower.Settings     shielding = new ShieldingPower.Settings();
	public SlowPower.Settings          slow = new SlowPower.Settings();
	public StormlashPower.Settings     stormlash = new StormlashPower.Settings();
	public StoneskinPower.Settings     stoneskin = new StoneskinPower.Settings();
	public SwiftnessPower.Settings     swiftness = new SwiftnessPower.Settings();
	public TrailblazePower.Settings    trailblaze = new TrailblazePower.Settings();
	public UnstablePower.Settings      unstable = new UnstablePower.Settings();
	public WeakenPower.Settings        weaken = new WeakenPower.Settings();
	public WildfirePower.Settings      wildfire = new WildfirePower.Settings();
	public ZephyrPower.Settings        zephyr = new ZephyrPower.Settings();
	public VerdantTrailPower.Settings  verdantTrail = new VerdantTrailPower.Settings();
	public FaultlinePower.Settings  faultline = new FaultlinePower.Settings();
	public NecromanticArsenalPower.Settings necromanticArsenal = new NecromanticArsenalPower.Settings();
	public WolfPackPower.Settings wolfPack = new WolfPackPower.Settings();
	public GoatStampedePower.Settings goatStampede = new GoatStampedePower.Settings();
	public BaneheadSwarmPower.Settings baneheadSwarm = new BaneheadSwarmPower.Settings();
	public WingBuffetPower.Settings wingBuffet = new WingBuffetPower.Settings();
	public DragonMawPower.Settings dragonMaw = new DragonMawPower.Settings();
	public RunicSlashPower.Settings runicSlash = new RunicSlashPower.Settings();
	public EvocationPower.Settings evocation = new EvocationPower.Settings();
	public SnifferSlamPower.Settings snifferSlam = new SnifferSlamPower.Settings();

	public SimplySkills simplySkills = new SimplySkills();

	public static class SimplySkills extends ConfigSection {

		public ValidatedCondition<Integer> preciseChance = createCondition(30);
		public ValidatedCondition<Integer> mightyChance = createCondition(30);
		public ValidatedCondition<Integer> stealthyChance = createCondition(30);
		public ValidatedCondition<Integer> renewedChance = createCondition(30);
		public ValidatedCondition<Integer> leapingChance = createCondition(65);
		public ValidatedCondition<Integer> spellshieldChance = createCondition(15);

		private ValidatedCondition<Integer> createCondition(int defaultValue) {
			return new ValidatedInt(defaultValue, 100, 0)
					.toCondition(
							() -> Platform.isModLoaded("simplyskills"),
							Text.translatable("simplyswords.gem_powers.simplySkills.condition"),
							() -> defaultValue
					).withFailTitle(Text.translatable("simplyswords.gem_powers.simplySkills.failTitle"));
		}
	}

//Settings
	    //Settings blocks are all stored within the corresponding power.
		//some of them override predefined basic settings classes; Fzzy Config works with inherited settings as well as ones directly in the subclass
	    //example
			//type declaration: Settings extends TooltipSettings, if nothing else. This allows tooltips of what the power is to appear in-game
			//constructor: TooltipSettings require a supplier of TooltipAppender.
				//Conveniently, RegistrySupplier is a supplier, and GemPower is a TooltipAppender!
			//settings: in most places I've restricted the settings so non-functional numbers aren't possible (like negative durations).
				//Also, most power settings share names, so the @Translation mentioned in Config is here too
			//Reference into a settings like
				//int f = Config.gemPowers.activeDefence.frequency;
		/*
		public static class Settings extends TooltipSettings {

			public Settings() {
				super(GemPowerRegistry.ACTIVE_DEFENCE);
			}

			@Translation(prefix = "simplyswords.config.basic_settings")
			@ValidatedInt.Restrict(min = 1)
			public int frequency = 20;

			@Translation(prefix = "simplyswords.config.basic_settings")
			@ValidatedDouble.Restrict(min = 0.0)
			public double radius = 5.0;
		}
		*/

		//example 2, simple settings
			//this exmaple uses one of the predefined settings classes. Many powers use the exact same set of 2 or 3 settings.
			//simply provide the chance and duration and the super class handles all the config stuff for you

		/*
		public static class Settings extends ChanceDurationSettings {

			public Settings() {
				super(15, 50, GemPowerRegistry.FLOAT);
			}
		}
		*/


}
