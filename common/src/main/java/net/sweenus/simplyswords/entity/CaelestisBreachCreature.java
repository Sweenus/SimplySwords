package net.sweenus.simplyswords.entity;

import java.util.UUID;

public interface CaelestisBreachCreature extends SimplySwordsMinion {

    UUID getBreachId();

    UUID getBreachActorId();

    UUID getBreachPrincipalId();

    boolean isUnbound();

    int getCorruptionSeed();

    void configureBreachCreature(UUID breachId, UUID actorId, UUID principalId,
                                 boolean unbound, int corruptionSeed);
}
