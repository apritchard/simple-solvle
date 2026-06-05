package com.appsoil.solvle.benchmark;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.config.SolvleConfig;
import com.appsoil.solvle.service.SolvleService;
import com.appsoil.solvle.service.WordConfig;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

@Log4j2
@Tag("benchmark")
@SpringBootTest(classes = {SolvleService.class, SolvleConfig.class})
@ActiveProfiles("test")
class DictionaryBenchmarkTest {

    private static final int WARMUP_RUNS = 1;
    private static final int MEASUREMENT_RUNS = 3;
    private static final boolean REQUIRE_ANSWER = true;
    private static final DictionaryType DICTIONARY = DictionaryType.SIMPLE;
    private static final BenchmarkTolerances TOLERANCES = BenchmarkTolerances.defaults();

    @Autowired
    SolvleService solvleService;

    @Test
    void simpleConfig() throws IOException {
        runConfig(WordConfig.SIMPLE, false);
    }

    @Test
    void simpleWithPartitioning() throws IOException {
        runConfig(WordConfig.SIMPLE_WITH_PARTITIONING, false);
    }

    @Test
    void optimalMean() throws IOException {
        runConfig(WordConfig.OPTIMAL_MEAN, false);
    }

    @Test
    void optimalMeanWithPartitioning_flagship() throws IOException {
        runConfig(WordConfig.OPTIMAL_MEAN_WITH_PARTITIONING, false);
    }

    @Test
    void optimalMeanExtendedPartitioning() throws IOException {
        runConfig(WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING, false);
    }

    @Test
    void twoOrLess() throws IOException {
        runConfig(WordConfig.TWO_OR_LESS, false);
    }

    @Test
    void optimalMeanWithPartitioning_harmonic() throws IOException {
        runConfig(WordConfig.OPTIMAL_MEAN_WITH_PARTITIONING_HARMONIC, false);
    }

    @Test
    void optimalMeanWithPartitioning_hardMode() throws IOException {
        runConfig(WordConfig.OPTIMAL_MEAN_WITH_PARTITIONING_HARD_MODE, true);
    }

    @Test
    void optimalMeanWithPartitioning_hardMode_rutBreak() throws IOException {
        runConfig(WordConfig.OPTIMAL_MEAN_WITH_PARTITIONING_HARD_MODE_RUTBREAK, true);
    }

    private void runConfig(WordConfig wordConfig, boolean hardMode) throws IOException {
        log.info("Benchmarking {} on {} hardMode={} ({} warmup + {} measurement runs)",
                wordConfig, DICTIONARY, hardMode, WARMUP_RUNS, MEASUREMENT_RUNS);

        BenchmarkReport report = BenchmarkRunner.run(
                solvleService, wordConfig, DICTIONARY,
                hardMode, REQUIRE_ANSWER, WARMUP_RUNS, MEASUREMENT_RUNS
        );

        BenchmarkBaselineIO.writeCurrent(report);
        log.info("[BENCHMARK] {} firstWord={} mean={} median={} p95={} max={} failures={} runtime={}ms ({} ms/word)",
                report.config(), report.firstWord(),
                String.format("%.4f", report.mean()),
                String.format("%.1f", report.median()),
                String.format("%.1f", report.p95()),
                report.max(), report.failureCount(),
                report.runtimeMillisMedian(),
                String.format("%.2f", report.runtimeMillisPerSolution()));

        if (Boolean.getBoolean("benchmark.baseline.write")) {
            BenchmarkBaselineIO.writeBaseline(report);
            log.info("[BENCHMARK] Wrote baseline for {} on {} (hardMode={})", report.config(), report.dictionary(), report.hardMode());
            return;
        }

        Optional<BenchmarkReport> baseline = BenchmarkBaselineIO.readBaseline(report.config(), report.dictionary(), report.hardMode());
        if (baseline.isEmpty()) {
            fail("No baseline for " + report.config() + " on " + report.dictionary() + " (hardMode=" + report.hardMode() + ")"
                    + ". Run with -Dbenchmark.baseline.write=true to create one.");
            return;
        }

        BenchmarkComparator.runtimeWarning(baseline.get(), report, TOLERANCES)
                .ifPresent(msg -> log.warn("[BENCHMARK WARN] {} - {}", report.config(), msg));

        List<BenchmarkComparator.Violation> violations =
                BenchmarkComparator.compare(baseline.get(), report, TOLERANCES);

        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> "  - " + v.metric() + ": " + v.message())
                    .collect(Collectors.joining("\n"));
            fail("Benchmark regression for " + report.config() + " on " + report.dictionary() + " (hardMode=" + report.hardMode() + "):\n" + msg);
        }
    }
}
