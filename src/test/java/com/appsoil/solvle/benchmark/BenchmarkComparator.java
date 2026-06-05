package com.appsoil.solvle.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BenchmarkComparator {

    public record Violation(String metric, String message) {
    }

    private BenchmarkComparator() {
    }

    public static List<Violation> compare(BenchmarkReport baseline, BenchmarkReport current, BenchmarkTolerances tol) {
        List<Violation> violations = new ArrayList<>();

        double meanDelta = current.mean() - baseline.mean();
        if (meanDelta > tol.maxMeanDelta()) {
            violations.add(new Violation("mean", String.format(
                    "Mean regressed: baseline=%.4f, current=%.4f, delta=%+.4f (tolerance %+.4f)",
                    baseline.mean(), current.mean(), meanDelta, tol.maxMeanDelta())));
        }

        if (tol.maxStrict() && current.max() > baseline.max()) {
            violations.add(new Violation("max", String.format(
                    "Max regressed: baseline=%d, current=%d",
                    baseline.max(), current.max())));
        }

        if (tol.failureCountStrict() && current.failureCount() > baseline.failureCount()) {
            violations.add(new Violation("failureCount", String.format(
                    "Failure count regressed: baseline=%d, current=%d",
                    baseline.failureCount(), current.failureCount())));
        }

        violations.addAll(distributionViolations(baseline, current, tol));

        return violations;
    }

    public static Optional<String> runtimeWarning(BenchmarkReport baseline, BenchmarkReport current, BenchmarkTolerances tol) {
        if (baseline.runtimeMillisMedian() <= 0) {
            return Optional.empty();
        }
        double ratio = (double) current.runtimeMillisMedian() / baseline.runtimeMillisMedian();
        if (ratio > 1.0 + tol.runtimeWarningThreshold()) {
            return Optional.of(String.format(
                    "Runtime regression: baseline=%dms, current=%dms (%+.0f%%, threshold +%.0f%%)",
                    baseline.runtimeMillisMedian(), current.runtimeMillisMedian(),
                    (ratio - 1) * 100, tol.runtimeWarningThreshold() * 100));
        }
        return Optional.empty();
    }

    private static List<Violation> distributionViolations(BenchmarkReport baseline, BenchmarkReport current, BenchmarkTolerances tol) {
        List<Violation> violations = new ArrayList<>();
        int wordCount = current.wordCount();
        double maxShift = tol.maxDistributionShiftFraction() * wordCount;

        int maxBucket = Math.max(
                maxKey(baseline.distribution()),
                maxKey(current.distribution())
        );

        for (int n = 1; n <= maxBucket; n++) {
            int baseTail = tailCount(baseline.distribution(), n);
            int currentTail = tailCount(current.distribution(), n);
            int shifted = currentTail - baseTail;
            if (shifted > maxShift) {
                violations.add(new Violation("distribution", String.format(
                        "Distribution shifted: %d more solutions take more than %d guesses (baseline tail=%d, current tail=%d, tolerance %.0f)",
                        shifted, n, baseTail, currentTail, maxShift)));
                return violations;
            }
        }
        return violations;
    }

    private static int tailCount(Map<Integer, Integer> distribution, int threshold) {
        return distribution.entrySet().stream()
                .filter(e -> e.getKey() > threshold)
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    private static int maxKey(Map<Integer, Integer> distribution) {
        return distribution.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }
}
