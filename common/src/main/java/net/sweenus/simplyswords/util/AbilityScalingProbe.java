package net.sweenus.simplyswords.util;

import net.sweenus.simplyswords.api.SpellScalingProfile;

public final class AbilityScalingProbe {

    public enum BranchKind {
        DAMAGE,
        VALUE
    }

    @FunctionalInterface
    public interface Sink {
        void record(SpellScalingProfile profile, BranchKind kind, float spellBranch, float meleeBranch,
                    float chosen, boolean spellWon);
    }

    private static volatile Sink sink;

    private AbilityScalingProbe() {
    }

    public static void setSink(Sink newSink) {
        sink = newSink;
    }

    static float choose(SpellScalingProfile profile, BranchKind kind, float spellBranch, float meleeBranch) {
        float chosen = Math.max(spellBranch, meleeBranch);
        Sink current = sink;
        if (current != null) {
            current.record(profile, kind, spellBranch, meleeBranch, chosen, spellBranch > meleeBranch);
        }
        return chosen;
    }
}
