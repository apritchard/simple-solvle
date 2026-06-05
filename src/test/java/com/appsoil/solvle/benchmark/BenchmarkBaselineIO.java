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

    public static Path baselinePath(String config, String dictionary) {
        return BASELINE_DIR.resolve(String.format("%s-%s-baseline.json", config, dictionary));
    }

    public static Path currentPath(BenchmarkReport report) {
        return CURRENT_DIR.resolve(String.format("%s-%s.json", report.config(), report.dictionary()));
    }

    public static void writeCurrent(BenchmarkReport report) throws IOException {
        Files.createDirectories(CURRENT_DIR);
        MAPPER.writeValue(currentPath(report).toFile(), report);
    }

    public static void writeBaseline(BenchmarkReport report) throws IOException {
        Files.createDirectories(BASELINE_DIR);
        MAPPER.writeValue(baselinePath(report.config(), report.dictionary()).toFile(), report);
    }

    public static Optional<BenchmarkReport> readBaseline(String config, String dictionary) throws IOException {
        Path path = baselinePath(config, dictionary);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(MAPPER.readValue(path.toFile(), BenchmarkReport.class));
    }
}
