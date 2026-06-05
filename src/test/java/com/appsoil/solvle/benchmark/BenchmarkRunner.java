package com.appsoil.solvle.benchmark;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.service.SolvleService;
import com.appsoil.solvle.service.WordCalculationConfig;
import com.appsoil.solvle.service.WordConfig;
import com.appsoil.solvle.service.solvers.RemainingSolver;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BenchmarkRunner {

    private BenchmarkRunner() {
    }

    public static BenchmarkReport run(SolvleService solvleService,
                                      WordConfig wordConfig,
                                      DictionaryType dictionary,
                                      boolean hardMode,
                                      boolean requireAnswer,
                                      int warmupRuns,
                                      int measurementRuns) {

        WordCalculationConfig config = wordConfig.config.withHardMode(hardMode).withRequireAnswer(requireAnswer);
        RemainingSolver solver = new RemainingSolver(solvleService, config);

        for (int i = 0; i < warmupRuns; i++) {
            solvleService.solveDictionary(solver, "", config, dictionary);
        }

        long[] runtimes = new long[measurementRuns];
        Map<String, List<String>> outcome = null;
        for (int i = 0; i < measurementRuns; i++) {
            long t0 = System.nanoTime();
            outcome = solvleService.solveDictionary(solver, "", config, dictionary);
            long t1 = System.nanoTime();
            runtimes[i] = (t1 - t0) / 1_000_000L;
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        Map<Integer, Integer> distribution = new TreeMap<>();
        for (List<String> guesses : outcome.values()) {
            int n = guesses.size();
            stats.addValue(n);
            distribution.merge(n, 1, Integer::sum);
        }

        int failureCount = distribution.entrySet().stream()
                .filter(e -> e.getKey() > 6)
                .mapToInt(Map.Entry::getValue)
                .sum();

        String firstWord = outcome.values().stream()
                .findFirst()
                .map(g -> g.isEmpty() ? "" : g.get(0))
                .orElse("");

        long runtimeMedian = median(runtimes);
        double perSolution = outcome.isEmpty() ? 0.0 : (double) runtimeMedian / outcome.size();

        return new BenchmarkReport(
                wordConfig.name(),
                dictionary.name(),
                hardMode,
                requireAnswer,
                firstWord,
                outcome.size(),
                distribution,
                stats.getMean(),
                stats.getPercentile(50),
                stats.getPercentile(95),
                (int) stats.getMax(),
                stats.getStandardDeviation(),
                failureCount,
                runtimeMedian,
                perSolution,
                measurementRuns
        );
    }

    private static long median(long[] arr) {
        long[] sorted = arr.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        if (n == 0) {
            return 0;
        }
        if (n % 2 == 0) {
            return (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
        }
        return sorted[n / 2];
    }
}
