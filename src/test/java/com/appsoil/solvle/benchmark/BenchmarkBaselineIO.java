package com.appsoil.solvle.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class BenchmarkBaselineIO {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Path BASELINE_DIR = Path.of("src/test/resources/benchmarks");
    private static final Path CURRENT_DIR = Path.of("target/benchmark-reports");

    private BenchmarkBaselineIO() {
    }

    public static Path baselinePath(String config, String dictionary, boolean hardMode) {
        return BASELINE_DIR.resolve(String.format("%s-%s%s-baseline.json", config, dictionary, hardMode ? "-hardMode" : ""));
    }

    public static Path currentPath(BenchmarkReport report) {
        return CURRENT_DIR.resolve(String.format("%s-%s%s.json", report.config(), report.dictionary(), report.hardMode() ? "-hardMode" : ""));
    }

    public static void writeCurrent(BenchmarkReport report) throws IOException {
        Files.createDirectories(CURRENT_DIR);
        MAPPER.writeValue(currentPath(report).toFile(), report);
    }

    public static void writeBaseline(BenchmarkReport report) throws IOException {
        Files.createDirectories(BASELINE_DIR);
        MAPPER.writeValue(baselinePath(report.config(), report.dictionary(), report.hardMode()).toFile(), report);
    }

    public static Optional<BenchmarkReport> readBaseline(String config, String dictionary, boolean hardMode) throws IOException {
        Path path = baselinePath(config, dictionary, hardMode);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(MAPPER.readValue(path.toFile(), BenchmarkReport.class));
    }
}
