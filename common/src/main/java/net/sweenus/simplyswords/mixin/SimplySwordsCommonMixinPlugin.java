package net.sweenus.simplyswords.mixin;

import dev.architectury.platform.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class SimplySwordsCommonMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogManager.getLogger("simplyswords");
    private static final Supplier<Boolean> TRUE = () -> true;
    private static final Map<String, Supplier<Boolean>> CONDITIONS = Map.of(
            "net.sweenus.simplyswords.mixin.client.compat.SpellEngineSpellHotbarMixin",
            () -> isModLoaded("spell_engine")
    );

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean shouldApply = CONDITIONS.getOrDefault(mixinClassName, TRUE).get();
        if (CONDITIONS.containsKey(mixinClassName)) {
            LOGGER.info("Simply Swords compatibility mixin {}: {}", shouldApply ? "enabled" : "skipped", mixinClassName);
        }
        return shouldApply;
    }

    private static boolean isModLoaded(String modId) {
        return Platform.isModLoaded(modId);
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
