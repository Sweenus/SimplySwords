package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.registries.options.DefaultIdRegistrarOption;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.power.GemPower;
import net.sweenus.simplyswords.power.PowerType;
import net.sweenus.simplyswords.power.powers.*;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class GemPowerRegistry {

	public static void register() {}

	public static Registrar<GemPower> REGISTRY = RegistrarManager.get("simplyswords")
			.<GemPower>builder(new Identifier(SimplySwords.MOD_ID, "gem_power"))
			.option(new DefaultIdRegistrarOption(GemPower.EMPTY_ID))
			.syncToClients()
			.build();

	// Resolves a stored power id to its GemPower.
	//
	// Deliberately goes through Registrar#get(Identifier) rather than any Holder accessor:
	// Architectury's RegistrySupplier inherits conflicting getKey() defaults from
	// DeferredSupplier and NeoForge's IHolderExtension, so anything that routes through
	// getKey() (unwrap, unwrapKey, getRegisteredName) throws IncompatibleClassChangeError
	// on NeoForge. See architectury-api#562 / #592 / #703.
	//
	// Unknown ids resolve to EMPTY rather than failing, so a stack keeps its power id on
	// disk when the addon that registered it is temporarily absent.
	public static GemPower resolve(@Nullable Identifier id) {
		if (id == null) return GemPower.EMPTY;
		GemPower power = REGISTRY.get(id);
		return power == null ? GemPower.EMPTY : power;
	}

	private static RegistrySupplier<GemPower> register(String path, Supplier<GemPower> power) {
		return REGISTRY.register(new Identifier(SimplySwords.MOD_ID, path), power);
	}

	public static List<? extends RegistrySupplier<GemPower>> getPowers(PowerType powerType) {
		return powerType.getEntries().stream().filter(entry -> !Config.gemPowers.disabledPowers.contains(entry.getId())).toList();
	}

	public static Identifier gemRandomPower(PowerType powerType) {
		return gemRandomPower(powerType, null);
	}

	public static Identifier gemRandomPower(PowerType powerType, Identifier[] blacklist) {
		List<? extends RegistrySupplier<GemPower>> powers = getPowers(powerType);

		if (powers.isEmpty()) {
			return GemPower.EMPTY_ID;
		}

		if (blacklist != null) {
			Set<Identifier> blacklistSet = new HashSet<>(Arrays.asList(blacklist));

			// Filter the list of powers to exclude blacklisted ones
			powers = powers.stream()
					.filter(power -> !blacklistSet.contains(power.getId()))
					.toList();

			// Check if all available options are blacklisted
			if (powers.isEmpty()) {
				return GemPower.EMPTY_ID;
			}
		}

		return powers.get(HelperMethods.random().nextInt(powers.size())).getId();
	}




	public static RegistrySupplier<GemPower> EMPTY = register("empty_power", () -> GemPower.EMPTY);

	public static RegistrySupplier<GemPower> ACTIVE_DEFENCE = register("active_defence", ActiveDefencePower::new);
	public static RegistrySupplier<GemPower> FLOAT = register("float", () -> new FloatPower(false));
	public static RegistrySupplier<GemPower> GREATER_FLOAT = register("greater_float", () -> new FloatPower(true));
	public static RegistrySupplier<GemPower> FREEZE = register("freeze", FreezePower::new);
	public static RegistrySupplier<GemPower> SHIELDING = register("shielding", () -> new ShieldingPower(false));
	public static RegistrySupplier<GemPower> GREATER_SHIELDING = register("greater_shielding", () -> new ShieldingPower(true));
	public static RegistrySupplier<GemPower> SLOW = register("slow", () -> new SlowPower(false));
	public static RegistrySupplier<GemPower> GREATER_SLOW = register("greater_slow", () -> new SlowPower(true));
	public static RegistrySupplier<GemPower> STONESKIN = register("stoneskin", () -> new StoneskinPower(false));
	public static RegistrySupplier<GemPower> GREATER_STONESKIN = register("greater_stoneskin", () -> new StoneskinPower(true));
	public static RegistrySupplier<GemPower> SWIFTNESS = register("swiftness", () -> new SwiftnessPower(false));
	public static RegistrySupplier<GemPower> GREATER_SWIFTNESS = register("greater_swiftness", () -> new SwiftnessPower(true));
	public static RegistrySupplier<GemPower> TRAILBLAZE = register("trailblaze", () -> new TrailblazePower(false));
	public static RegistrySupplier<GemPower> GREATER_TRAILBLAZE = register("greater_trailblaze", () -> new TrailblazePower(true));
	public static RegistrySupplier<GemPower> WEAKEN = register("weaken", () -> new WeakenPower(false));
	public static RegistrySupplier<GemPower> GREATER_WEAKEN = register("greater_weaken", () -> new WeakenPower(true));
	public static RegistrySupplier<GemPower> ZEPHYR = register("zephyr", () -> new ZephyrPower(false));
	public static RegistrySupplier<GemPower> GREATER_ZEPHYR = register("greater_zephyr", () -> new ZephyrPower(true));
	public static RegistrySupplier<GemPower> FROST_WARD = register("frost_ward", FrostWardPower::new);
	public static RegistrySupplier<GemPower> WILDFIRE = register("wildfire", WildfirePower::new);
	public static RegistrySupplier<GemPower> UNSTABLE = register("unstable", UnstablePower::new);
	public static RegistrySupplier<GemPower> IMBUED = register("imbued", () -> new ImbuedPower(false));
	public static RegistrySupplier<GemPower> GREATER_IMBUED = register("greater_imbued", () -> new ImbuedPower(true));
	public static RegistrySupplier<GemPower> STORMLASH = register("stormlash", StormlashPower::new);
	public static RegistrySupplier<GemPower> DANCING_BLADES = register("dancing_blades", DancingBladesPower::new);
	public static RegistrySupplier<GemPower> FAULTLINE = register("faultline", FaultlinePower::new);
	public static RegistrySupplier<GemPower> RUNIC_SLASH = register("runic_slash", RunicSlashPower::new);
	public static RegistrySupplier<GemPower> EVOCATION = register("evocation", EvocationPower::new);
	public static RegistrySupplier<GemPower> SNIFFER_SLAM = register("sniffer_slam", SnifferSlamPower::new);
	public static RegistrySupplier<GemPower> PINCUSHION = register("pincushion", () -> new PincushionPower(false));
	public static RegistrySupplier<GemPower> GREATER_PINCUSHION = register("greater_pincushion", () -> new PincushionPower(true));

	public static RegistrySupplier<GemPower> MOMENTUM = register("momentum", () -> new MomentumPower(false));
	public static RegistrySupplier<GemPower> GREATER_MOMENTUM = register("greater_momentum", () -> new MomentumPower(true));
	public static RegistrySupplier<GemPower> WARD = register("ward", WardPower::new);
	public static RegistrySupplier<GemPower> IMMOLATION = register("immolation", ImmolationPower::new);
	public static RegistrySupplier<GemPower> THROWING = register("throwing", ThrowingPower::new);

	public static RegistrySupplier<GemPower> ECHO = register("echo", EchoPower::new);
	public static RegistrySupplier<GemPower> BERSERK = register("berserk", BerserkPower::new);
	public static RegistrySupplier<GemPower> RADIANCE = register("radiance", RadiancePower::new);
	public static RegistrySupplier<GemPower> ONSLAUGHT = register("onslaught", OnslaughtPower::new);
	public static RegistrySupplier<GemPower> NULLIFICATION = register("nullification", NullificationPower::new);
	public static RegistrySupplier<GemPower> VERDANT_TRAIL = register("verdant_trail", VerdantTrailPower::new);
	public static RegistrySupplier<GemPower> NECROMANTIC_ARSENAL = register("necromantic_arsenal", NecromanticArsenalPower::new);
	public static RegistrySupplier<GemPower> WOLF_PACK = register("wolf_pack", WolfPackPower::new);
	public static RegistrySupplier<GemPower> GOAT_STAMPEDE = register("goat_stampede", GoatStampedePower::new);
	public static RegistrySupplier<GemPower> BANEHEAD_SWARM = register("banehead_swarm", BaneheadSwarmPower::new);
	public static RegistrySupplier<GemPower> WING_BUFFET = register("wing_buffet", WingBuffetPower::new);
	public static RegistrySupplier<GemPower> DRAGON_MAW = register("dragon_maw", DragonMawPower::new);

}
