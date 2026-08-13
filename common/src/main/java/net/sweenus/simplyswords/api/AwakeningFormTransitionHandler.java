package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;

@FunctionalInterface
public interface AwakeningFormTransitionHandler {
    Identifier transitionRoute(AwakeningFormContext context, Identifier currentRoute);
}
