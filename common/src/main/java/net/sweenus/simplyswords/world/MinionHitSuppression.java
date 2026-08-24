package net.sweenus.simplyswords.world;

public final class MinionHitSuppression {

    private static final ThreadLocal<Boolean> SUPPRESSED = ThreadLocal.withInitial(() -> false);

    private MinionHitSuppression() {
    }

    public static boolean isSuppressed() {
        return SUPPRESSED.get();
    }

    public static void runSuppressed(Runnable runnable) {
        SUPPRESSED.set(true);
        try {
            runnable.run();
        } finally {
            SUPPRESSED.set(false);
        }
    }
}
