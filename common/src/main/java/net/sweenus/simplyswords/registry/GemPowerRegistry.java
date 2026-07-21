package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.registries.options.DefaultIdRegistrarOption;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.power.GemPower;
import net.sweenus.simplyswords.power.PowerType;
import net.sweenus.simplyswords.power.powers.*;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class GemPowerRegistry {

	public static void register() {}

	public static Registrar<GemPower> REGISTRY = RegistrarManager.get("simplyswords")
			.<GemPower>builder(Identifier.of(SimplySwords.MOD_ID, "gem_power"))
			.option(new DefaultIdRegistrarOption(Identifier.of(SimplySwords.MOD_ID, "empty_power")))
			.syncToClients()
			.build();

	private static RegistrySupplier<GemPower> register(String path, Supplier<GemPower> power) {
		return REGISTRY.register(Identifier.of(SimplySwords.MOD_ID, path), power);
	}

	public static List<? extends RegistryEntry<GemPower>> getPowers(PowerType powerType) {
		return powerType.getEntries().stream().filter(entry -> !Config.gemPowers.disabledPowers.contains(entry.getId())).toList();
	}

	/*
	public static RegistryEntry<GemPower> gemRandomPower(PowerType powerType) {
		List<? extends RegistryEntry<GemPower>> powers = getPowers(powerType);
		if (powers.isEmpty()) {
			return EMPTY;
		}
		return powers.get(HelperMethods.random().nextInt(powers.size()));
	}

	 */

	public static RegistryEntry<GemPower> gemRandomPower(PowerType powerType) {
		return gemRandomPower(powerType, null);
	}

	public static RegistryEntry<GemPower> gemRandomPower(PowerType powerType, String[] blacklist) {
		List<? extends RegistryEntry<GemPower>> powers = getPowers(powerType);

		if (powers.isEmpty()) {
			return EMPTY;
		}

		if (blacklist != null) {
			Set<String> blacklistSet = new HashSet<>(Arrays.asList(blacklist));

			// Filter the list of powers to exclude blacklisted ones
			powers = powers.stream()
					.filter(power -> !blacklistSet.contains(power.toString()))
					.toList();

			// Check if all available options are blacklisted
			if (powers.isEmpty()) {
				return EMPTY;
			}
		}

		 //System.out.println("excluding: " + Arrays.toString(blacklist) + " from " + powers.size() + " powers" );
		//System.out.println("choosing: " + powers.get(HelperMethods.random().nextInt(powers.size())).toString());
		return powers.get(HelperMethods.random().nextInt(powers.size()));
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
	public static RegistrySupplier<GemPower> EARTHSHATTER = register("earthshatter", EarthshatterPower::new);
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

}
