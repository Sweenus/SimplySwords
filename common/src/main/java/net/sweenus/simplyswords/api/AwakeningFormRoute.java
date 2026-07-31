package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

//
// A locked awakening route and its level-dependent stages.
//
public record AwakeningFormRoute(Identifier id, List<AwakeningFormStage> stages) {
    public AwakeningFormRoute {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(stages, "stages");
        stages = stages.stream()
                .sorted(Comparator.comparingInt(AwakeningFormStage::minimumLevel))
                .toList();
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("Awakening route must contain at least one stage: " + id);
        }
        for (int i = 1; i < stages.size(); i++) {
            if (stages.get(i - 1).minimumLevel() == stages.get(i).minimumLevel()) {
                throw new IllegalArgumentException("Duplicate awakening stage level "
                        + stages.get(i).minimumLevel() + " in route " + id);
            }
        }
    }

    public Optional<AwakeningFormStage> stageForLevel(int level) {
        AwakeningFormStage resolved = null;
        for (AwakeningFormStage stage : stages) {
            if (stage.minimumLevel() > level) break;
            resolved = stage;
        }
        return Optional.ofNullable(resolved);
    }
}
