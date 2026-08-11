package net.sweenus.simplyswords.api;

public record PlayerMovementIntent(int forward, int strafe) {

    public static final PlayerMovementIntent NONE = new PlayerMovementIntent(0, 0);

    public PlayerMovementIntent {
        forward = Integer.compare(forward, 0);
        strafe = Integer.compare(strafe, 0);
    }

    public boolean isNeutral() {
        return forward == 0 && strafe == 0;
    }
}
