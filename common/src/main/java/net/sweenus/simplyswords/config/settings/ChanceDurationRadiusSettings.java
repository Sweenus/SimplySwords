package net.sweenus.simplyswords.config.settings;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.sweenus.simplyswords.config.settings.TooltipProvider;

import java.util.function.Supplier;

public class ChanceDurationRadiusSettings extends TooltipSettings {

	public ChanceDurationRadiusSettings(int chance, int duration, double radius, Supplier<? extends TooltipProvider> appender) {
		super(appender);
		this.chance = chance;
		this.duration = duration;
		this.radius = radius;
	}

	public ChanceDurationRadiusSettings(int chance, int duration, double radius) {
		super();
		this.chance = chance;
		this.duration = duration;
		this.radius = radius;
	}

	@Translation(prefix = "simplyswords.config.basic_settings")
	@ValidatedInt.Restrict(min = 0, max = 100)
	public int chance;

	@Translation(prefix = "simplyswords.config.basic_settings")
	@ValidatedInt.Restrict(min = 0)
	public int duration;

	@Translation(prefix = "simplyswords.config.basic_settings")
	@ValidatedDouble.Restrict(min = 0.0)
	public double radius;
}