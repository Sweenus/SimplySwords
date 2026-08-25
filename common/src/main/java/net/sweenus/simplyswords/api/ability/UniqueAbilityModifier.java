package net.sweenus.simplyswords.api.ability;

@FunctionalInterface
public interface UniqueAbilityModifier {
    UniqueAbilityObserver prepare(UniqueAbilityContext context, UniqueAbilityDefinition definition,
                                  UniqueAbilityTuning.Builder tuning);
}
