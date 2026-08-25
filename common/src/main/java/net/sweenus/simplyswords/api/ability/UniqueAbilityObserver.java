package net.sweenus.simplyswords.api.ability;

@FunctionalInterface
public interface UniqueAbilityObserver {
    UniqueAbilityObserver NONE = event -> { };

    void onEvent(UniqueAbilityEvent event);
}
