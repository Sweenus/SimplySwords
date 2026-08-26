package net.sweenus.simplyswords.world;

public final class WickpiercerThrowMath {

    private WickpiercerThrowMath() {
    }

    public static int scaledFireSeconds(int fireTicks) {
        return fireTicks <= 0 ? 0 : Math.max(1, (fireTicks + 19) / 20);
    }

    public static int nextStrikeAge(int currentAge, int interval) {
        return currentAge + Math.max(1, interval);
    }

    public static boolean orbitContinues(int currentAge, int endAge, boolean targetAlive) {
        return targetAlive && currentAge <= endAge;
    }
}
