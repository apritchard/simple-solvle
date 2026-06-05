package com.appsoil.solvle.benchmark;

public record BenchmarkTolerances(
        double maxMeanDelta,
        boolean failureCountStrict,
        boolean maxStrict,
        double maxDistributionShiftFraction,
        double runtimeWarningThreshold
) {
    public static BenchmarkTolerances defaults() {
        return new BenchmarkTolerances(
                0.02,
                true,
                true,
                0.005,
                0.25
        );
    }
}
