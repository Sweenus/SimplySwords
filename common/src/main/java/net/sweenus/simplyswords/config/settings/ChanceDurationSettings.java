package net.sweenus.simplyswords.config.settings;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.sweenus.simplyswords.config.settings.TooltipProvider;

import java.util.function.Supplier;

public class ChanceDurationSettings extends TooltipSettings {

	public ChanceDurationSettings(int chance, int duration, Supplier<? extends TooltipProvider> appender) {
		super(appender);
		this.chance = chance;
		this.duration = duration;
	}

	public ChanceDurationSettings(int chance, int duration) {
		super();
		this.chance = chance;
		this.duration = duration;
	}

	@Translation(prefix = "simplyswords.config.basic_settings")
	@ValidatedInt.Restrict(min = 0, max = 100)
	public int chance;

	@Translation(prefix = "simplyswords.config.basic_settings")
	@ValidatedInt.Restrict(min = 0)
	public int duration;

}