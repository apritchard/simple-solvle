package com.appsoil.solvle.benchmark;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
        "config", "dictionary", "hardMode", "requireAnswer",
        "firstWord", "wordCount",
        "distribution",
        "mean", "median", "p95", "max", "stdev", "failureCount",
        "runtimeMillisMedian", "runtimeMillisPerSolution", "runs"
})
public record BenchmarkReport(
        String config,
        String dictionary,
        boolean hardMode,
        boolean requireAnswer,
        String firstWord,
        int wordCount,
        Map<Integer, Integer> distribution,
        double mean,
        double median,
        double p95,
        int max,
        double stdev,
        int failureCount,
        long runtimeMillisMedian,
        double runtimeMillisPerSolution,
        int runs
) {
}
