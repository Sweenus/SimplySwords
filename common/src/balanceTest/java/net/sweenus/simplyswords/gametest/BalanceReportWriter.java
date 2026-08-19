package net.sweenus.simplyswords.gametest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class BalanceReportWriter {

    private static final float SINGLE_TARGET_JUDGEMENT_FLOOR = 0.25F;
    private static final String[] COLUMNS = {
            "Weapon", "Ability", "Build", "Scenario", "Targets", "DurationTicks", "DurationSeconds",
            "Casts", "MeleeSwings", "DamageEvents", "TotalMeleeDamage", "TotalAbilityDamage", "TotalDamage",
            "AverageDamagePerEvent", "MaximumHit", "DamagePerCast", "AbilityDPS", "MeleeDPS", "TotalDPS",
            "RatioVsVanillaBase", "RatioVsSharpnessV", "HitRate", "MeanDPS", "MedianDPS", "P95DPS",
            "RawSpellMeleeRatio", "AdjustedSpellMeleeRatio",
            "AbilityDpsVsVanillaBase", "AbilityDpsVsSharpnessV",
            "SpellBranchAvg", "MeleeBranchAvg", "SpellBranchWinRate", "BranchSamples", "ValueBranchSamples",
            "TopDamageSources",
            "Status", "Notes"
    };

    private BalanceReportWriter() {
    }

    static void write(List<BalanceRunResult> results) throws IOException {
        Path reportDir = Path.of(System.getProperty("simplyswords.balance-report-dir",
                "build/reports/simplyswords-balance"));
        Files.createDirectories(reportDir);
        List<ReportRow> rows = aggregate(results);
        Files.writeString(reportDir.resolve("ability-balance.csv"), csv(rows), StandardCharsets.UTF_8);
        Files.writeString(reportDir.resolve("ability-balance.md"), markdown(rows), StandardCharsets.UTF_8);
    }

    private static List<ReportRow> aggregate(List<BalanceRunResult> results) {
        Map<RowKey, List<BalanceRunResult>> grouped = results.stream().collect(Collectors.groupingBy(
                result -> new RowKey(result.spec().weaponId(), result.spec().ability(), result.build(),
                        result.scenario(), result.targets(), result.durationTicks()), LinkedHashMap::new, Collectors.toList()));
        Map<RowKey, Float> meanDps = new LinkedHashMap<>();
        Map<RowKey, Float> meanAbilityDps = new LinkedHashMap<>();
        grouped.forEach((key, runs) -> {
            meanDps.put(key, mean(runs.stream().map(BalanceRunResult::totalDps).toList()));
            meanAbilityDps.put(key, mean(runs.stream().map(BalanceRunResult::abilityDps).toList()));
        });

        Map<String, BalanceScenarioType> judgedScenario = new LinkedHashMap<>();
        grouped.keySet().forEach(key -> {
            if (key.build != BalanceBuildProfile.VANILLA_SHARPNESS_V) {
                return;
            }
            if (key.scenario == BalanceScenarioType.SINGLE_TARGET_ROTATION) {
                Float baseline = meanAbilityDps.get(key);
                if (baseline != null && baseline >= SINGLE_TARGET_JUDGEMENT_FLOOR) {
                    judgedScenario.put(key.weapon, BalanceScenarioType.SINGLE_TARGET_ROTATION);
                }
            } else if (key.scenario == BalanceScenarioType.FIVE_TARGET_CLUSTER) {
                judgedScenario.putIfAbsent(key.weapon, BalanceScenarioType.FIVE_TARGET_CLUSTER);
            }
        });

        List<ReportRow> rows = new ArrayList<>();
        grouped.forEach((key, runs) -> {
            RowKey vanillaKey = key.withBuild(BalanceBuildProfile.VANILLA_BASE);
            RowKey sharpnessKey = key.withBuild(BalanceBuildProfile.VANILLA_SHARPNESS_V);
            Float abilityBaseline = meanAbilityDps.get(sharpnessKey);
            rows.add(ReportRow.from(key, runs,
                    ratio(meanDps.get(key), meanDps.get(vanillaKey)),
                    ratio(meanDps.get(key), meanDps.get(sharpnessKey)),
                    ratio(meanAbilityDps.get(key), meanAbilityDps.get(vanillaKey)),
                    ratio(meanAbilityDps.get(key), abilityBaseline),
                    abilityBaseline == null ? 0.0F : abilityBaseline,
                    judgedScenario.getOrDefault(key.weapon, BalanceScenarioType.SINGLE_TARGET_ROTATION)));
        });
        rows.sort(Comparator.comparing((ReportRow row) -> row.key.weapon)
                .thenComparing(row -> row.key.scenario)
                .thenComparing(row -> row.key.build));
        return rows;
    }

    private static String csv(List<ReportRow> rows) {
        StringBuilder output = new StringBuilder(String.join(",", COLUMNS)).append('\n');
        rows.forEach(row -> output.append(row.csv()).append('\n'));
        return output.toString();
    }

    private static String markdown(List<ReportRow> rows) {
        StringBuilder output = new StringBuilder("# Simply Swords ability balance\n\n");
        output.append("Generated from real server-tick GameTest scenarios. Spell criticals remain enabled; explicit crit attribution is intentionally not added, so distribution columns describe observed DPS variation.\n\n");
        output.append('|').append(String.join("|", COLUMNS)).append("|\n|");
        for (int i = 0; i < COLUMNS.length; i++) {
            output.append("---|");
        }
        output.append('\n');
        rows.forEach(row -> output.append(row.markdown()).append('\n'));
        return output.toString();
    }

    private static float ratio(Float value, Float baseline) {
        return value == null || baseline == null || baseline <= 0.0F ? Float.NaN : value / baseline;
    }

    private static float mean(List<Float> values) {
        return values.isEmpty() ? 0.0F : (float) values.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
    }

    private static float percentile(List<Float> input, double percentile) {
        if (input.isEmpty()) {
            return 0.0F;
        }
        List<Float> values = input.stream().sorted().toList();
        int index = (int) Math.ceil(percentile * values.size()) - 1;
        return values.get(Math.clamp(index, 0, values.size() - 1));
    }

    private static String format(float value) {
        return Float.isFinite(value) ? String.format(Locale.ROOT, "%.4f", value) : "";
    }

    private static String csvCell(String value) {
        String safe = Objects.toString(value, "").replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private static String markdownCell(String value) {
        return Objects.toString(value, "").replace("|", "\\|").replace("\n", " ");
    }

    private record RowKey(String weapon, String ability, BalanceBuildProfile build,
                          BalanceScenarioType scenario, int targets, int durationTicks) {
        RowKey withBuild(BalanceBuildProfile replacement) {
            return new RowKey(weapon, ability, replacement, scenario, targets, durationTicks);
        }
    }

    private record ReportRow(RowKey key, List<String> values) {
        static ReportRow from(RowKey key, List<BalanceRunResult> runs, float ratioVanilla, float ratioSharpness,
                              float abilityRatioVanilla, float abilityRatioSharpness, float abilityBaseline,
                              BalanceScenarioType judgedScenario) {
            List<BalanceRunResult> available = runs.stream().filter(BalanceRunResult::available).toList();
            boolean hasData = !available.isEmpty();
            List<Float> dps = available.stream().map(BalanceRunResult::totalDps).toList();
            float durationSeconds = key.durationTicks / 20.0F;
            float casts = averageInt(available, BalanceRunResult::casts);
            float swings = averageInt(available, BalanceRunResult::meleeSwings);
            float events = averageInt(available, result -> result.events().size());
            float meleeDamage = BalanceReportWriter.mean(available.stream().map(BalanceRunResult::meleeDamage).toList());
            float abilityDamage = BalanceReportWriter.mean(available.stream().map(BalanceRunResult::abilityDamage).toList());
            float totalDamage = BalanceReportWriter.mean(available.stream().map(BalanceRunResult::totalDamage).toList());
            float maxHit = available.stream().map(BalanceRunResult::maximumHit).max(Float::compare).orElse(0.0F);
            int totalCasts = available.stream().mapToInt(BalanceRunResult::casts).sum();
            int castsHit = available.stream().mapToInt(BalanceRunResult::castsHit).sum();
            String notes = runs.stream().map(BalanceRunResult::notes).filter(value -> value != null && !value.isBlank())
                    .distinct().collect(Collectors.joining("; "));
            List<BranchStats> branches = available.stream().map(BalanceRunResult::branchStats)
                    .filter(stats -> stats.samples() > 0).toList();
            float spellBranchAvg = BalanceReportWriter.mean(branches.stream().map(BranchStats::spellBranchAvg).toList());
            float meleeBranchAvg = BalanceReportWriter.mean(branches.stream().map(BranchStats::meleeBranchAvg).toList());
            float spellWinRate = BalanceReportWriter.mean(branches.stream().map(BranchStats::spellWinRate).toList());
            int branchSamples = available.stream().mapToInt(result -> result.branchStats().samples()).sum();
            int valueSamples = available.stream().mapToInt(result -> result.branchStats().valueSamples()).sum();
            String topSources = topDamageSources(available);
            String status = status(key, hasData, abilityRatioVanilla, abilityRatioSharpness, abilityBaseline,
                    judgedScenario);
            List<String> values = List.of(
                    key.weapon,
                    key.ability,
                    key.build.name(),
                    key.scenario.displayName(),
                    Integer.toString(key.targets),
                    Integer.toString(key.durationTicks),
                    format(durationSeconds),
                    hasData ? format(casts) : "",
                    hasData ? format(swings) : "",
                    hasData ? format(events) : "",
                    hasData ? format(meleeDamage) : "",
                    hasData ? format(abilityDamage) : "",
                    hasData ? format(totalDamage) : "",
                    hasData && events > 0.0F ? format(totalDamage / events) : "",
                    hasData ? format(maxHit) : "",
                    hasData && casts > 0.0F ? format(abilityDamage / casts) : "",
                    hasData ? format(abilityDamage / durationSeconds) : "",
                    hasData ? format(meleeDamage / durationSeconds) : "",
                    hasData ? format(BalanceReportWriter.mean(dps)) : "",
                    hasData ? format(ratioVanilla) : "",
                    hasData ? format(ratioSharpness) : "",
                    key.scenario == BalanceScenarioType.RELIABILITY && totalCasts > 0
                            ? format(castsHit / (float) totalCasts) : "",
                    hasData ? format(BalanceReportWriter.mean(dps)) : "",
                    hasData ? format(BalanceReportWriter.percentile(dps, 0.5)) : "",
                    hasData ? format(BalanceReportWriter.percentile(dps, 0.95)) : "",
                    hasData ? format(BalanceReportWriter.mean(available.stream().map(BalanceRunResult::rawSpellMeleeRatio).toList())) : "",
                    hasData ? format(BalanceReportWriter.mean(available.stream().map(BalanceRunResult::adjustedSpellMeleeRatio).toList())) : "",
                    hasData ? format(abilityRatioVanilla) : "",
                    hasData ? format(abilityRatioSharpness) : "",
                    branchSamples > 0 ? format(spellBranchAvg) : "",
                    branchSamples > 0 ? format(meleeBranchAvg) : "",
                    branchSamples > 0 ? format(spellWinRate) : "",
                    Integer.toString(branchSamples),
                    Integer.toString(valueSamples),
                    topSources,
                    status,
                    notes
            );
            return new ReportRow(key, values);
        }

        private static float averageInt(List<BalanceRunResult> runs,
                                        java.util.function.ToIntFunction<BalanceRunResult> mapper) {
            return runs.isEmpty() ? 0.0F : (float) runs.stream().mapToInt(mapper).average().orElse(0.0);
        }

        private static String topDamageSources(List<BalanceRunResult> runs) {
            Map<String, Double> bySource = new LinkedHashMap<>();
            double total = 0.0;
            for (BalanceRunResult run : runs) {
                for (DamageEvent event : run.events()) {
                    if (event.melee()) {
                        continue;
                    }
                    bySource.merge(event.source(), (double) event.amount(), Double::sum);
                    total += event.amount();
                }
            }
            if (total <= 0.0) {
                return "";
            }
            double scale = total;
            return bySource.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(4)
                    .map(entry -> entry.getKey() + "=" + Math.round(entry.getValue() / scale * 100.0) + "%")
                    .collect(Collectors.joining(" "));
        }

        private static String status(RowKey key, boolean available, float ratioVanilla, float ratioSharpness,
                                     float abilityBaseline, BalanceScenarioType judgedScenario) {
            if (!available) {
                return "UNAVAILABLE";
            }
            if (key.scenario != judgedScenario) {
                return "INFORMATIONAL";
            }
            if (abilityBaseline <= 1.0E-3F) {
                return "NO_ABILITY_DAMAGE";
            }
            if (key.build == BalanceBuildProfile.VANILLA_BASE || key.build == BalanceBuildProfile.VANILLA_SHARPNESS_V) {
                return "BASELINE";
            }
            if (key.build != BalanceBuildProfile.HYBRID_SHARPNESS_CASTER) {
                return "INFORMATIONAL";
            }
            if (!Float.isFinite(ratioSharpness)) {
                return "INFORMATIONAL";
            }
            if (ratioSharpness > 1.50F) {
                return "SEVERE";
            }
            if (ratioSharpness > 1.35F) {
                return "OVERPERFORMING";
            }
            if (ratioSharpness > 1.30F) {
                return "WATCH";
            }
            if (ratioSharpness < 1.10F) {
                return "UNDERPERFORMING";
            }
            return "HEALTHY";
        }

        String csv() {
            return values.stream().map(BalanceReportWriter::csvCell).collect(Collectors.joining(","));
        }

        String markdown() {
            return "|" + values.stream().map(BalanceReportWriter::markdownCell).collect(Collectors.joining("|")) + "|";
        }
    }
}
