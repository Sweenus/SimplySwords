package net.sweenus.simplyswords.entity;

import java.util.UUID;

public interface CaelestisBreachCreature extends SimplySwordsMinion {

    UUID getBreachId();

    UUID getBreachActorId();

    UUID getBreachPrincipalId();

    boolean isUnbound();

    int getCorruptionSeed();

    boolean isOpenInvitationUnbound();

    void setOpenInvitationUnbound(boolean openInvitationUnbound);

    long getOwnerMarkUntil();

    void setOwnerMarkUntil(long ownerMarkUntil);

    int getUnboundRefundTicks();

    void setUnboundRefundTicks(int unboundRefundTicks);

    float getUnboundAttackDamage();

    void setUnboundAttackDamage(float unboundAttackDamage);

    void configureBreachCreature(UUID breachId, UUID actorId, UUID principalId,
                                 boolean unbound, int corruptionSeed);
}
