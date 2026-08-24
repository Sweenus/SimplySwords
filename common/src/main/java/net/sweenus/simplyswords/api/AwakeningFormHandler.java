package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;

@FunctionalInterface
public interface AwakeningFormHandler {
    Identifier selectRoute(AwakeningFormContext context);

    default void onCommitted(AwakeningFormCommitContext context) {
    }
}
