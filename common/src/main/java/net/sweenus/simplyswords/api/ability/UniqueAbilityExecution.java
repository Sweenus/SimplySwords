package net.sweenus.simplyswords.api.ability;

import java.util.List;

public final class UniqueAbilityExecution {
    private final long id;
    private final UniqueAbilityDefinition definition;
    private final UniqueAbilityContext context;
    private final UniqueAbilityTuning tuning;
    private final List<UniqueAbilityObserver> observers;
    private boolean started;
    private boolean terminal;
    private double pendingCooldownRefundFraction;

    UniqueAbilityExecution(long id, UniqueAbilityDefinition definition, UniqueAbilityContext context,
                           UniqueAbilityTuning tuning, List<UniqueAbilityObserver> observers) {
        this.id = id;
        this.definition = definition;
        this.context = context;
        this.tuning = tuning;
        this.observers = List.copyOf(observers);
    }

    public long id() {
        return id;
    }

    public UniqueAbilityDefinition definition() {
        return definition;
    }

    public UniqueAbilityContext context() {
        return context;
    }

    public UniqueAbilityTuning tuning() {
        return tuning;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public int cooldownTicks(int fallback) {
        return definition.cooldownKey().map(tuning::get).orElse(fallback);
    }

    public void requestCooldownRefundFraction(double fraction) {
        if (!Double.isFinite(fraction) || fraction <= 0) return;
        pendingCooldownRefundFraction = Math.max(pendingCooldownRefundFraction, Math.min(1, fraction));
    }

    public double takeCooldownRefundFraction() {
        double fraction = pendingCooldownRefundFraction;
        pendingCooldownRefundFraction = 0;
        return fraction;
    }

    List<UniqueAbilityObserver> observers() {
        return observers;
    }

    void markStarted() {
        started = true;
    }

    void markTerminal() {
        terminal = true;
    }
}
