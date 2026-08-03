package net.sweenus.simplyswords.config.settings;

import net.minecraft.text.Text;

import java.util.function.Consumer;

/** Lightweight 1.20.1 replacement for the later vanilla TooltipAppender API. */
@FunctionalInterface
public interface TooltipProvider {
    void appendTooltip(Consumer<Text> tooltip);
}
