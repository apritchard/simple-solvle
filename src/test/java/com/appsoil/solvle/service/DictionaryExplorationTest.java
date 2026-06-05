package com.appsoil.solvle.service;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.config.SolvleConfig;
import com.appsoil.solvle.controller.SolvleDTO;
import com.appsoil.solvle.data.KnownPosition;
import com.appsoil.solvle.data.PlayOut;
import com.appsoil.solvle.data.SharedPositions;
import com.appsoil.solvle.data.TupleScore;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.data.WordFrequencyScore;
import com.appsoil.solvle.data.WordRestrictions;
import com.appsoil.solvle.service.job.JobStatus;
import com.appsoil.solvle.service.solvers.RemainingSolver;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manual exploration suite around the full-dictionary solver. Not a regression test:
 * none of these methods assert behavior. They exist as a playground for tuning solver
 * configuration, exploring starter-word choices, and one-shot inspection of solver output.
 *
 * Run via {@code mvn -Pexploration test}. Excluded from default {@code mvn test} via the
 * {@code exploration} JUnit tag.
 *
 * For solver-quality regression tests with baselines and tolerances, see
 * {@link com.appsoil.solvle.benchmark.DictionaryBenchmarkTest}.
 */
@Log4j2
@Tag("exploration")
@SpringBootTest(classes = {SolvleService.class, SolvleConfig.class})
@ActiveProfiles("test")
class DictionaryExplorationTest {

    @Autowired
    SolvleService solvleService;

    static Map<WordCalculationConfig, TestReport> testReports;

    record TestReport(DescriptiveStatistics stats, String firstWord, List<List<String>> problems) {
    }

    @BeforeAll
    static void init() {
        testReports = new HashMap<>();
    }

    @AfterAll
    static void report() {
        log.warn("Begin report full report:");
        testReports.forEach(DictionaryExplorationTest::logReport);
    }

    static void addStats(WordCalculationConfig config, Map<String, List<String>> solution) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        List<List<String>> problems = new ArrayList<>();
        String firstWord = solution.values().stream().findFirst().get().get(0);
        solution.forEach((k, v) -> {
            stats.addValue(v.size());
            if (v.size() > 0) {
                log.info(v.size() + ":" + v);
                problems.add(v);
            }
        });
        TestReport report = new TestReport(stats, firstWord, problems);
        testReports.put(config, report);
        var countMap = Arrays.stream(report.stats().getSortedValues()).mapToInt(num -> (int) num).boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        log.info(config);
        log.info("Mean: {}, StDv: {}, Median: {}, Counts: {}",
                report.stats().getMean(), report.stats().getStandardDeviation(), report.stats().getPercentile(50), countMap);
        logReport(config, report);
    }

    static void logReport(WordCalculationConfig config, TestReport report) {
        var countMap = Arrays.stream(report.stats().getSortedValues()).mapToInt(num -> (int) num).boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        log.warn("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t\t\t\t\t\t{}\t{}\t{}\t{}\t{}",
                report.stats().getMean(),
                report.stats().getMin(),
                report.stats().getMax(),
                report.stats().getPercentile(50),
                report.stats().getStandardDeviation(),
                config.fishingThreshold(),
                config.viableWordAdjustmentScale(),
                config.vowelMultiplier(),
                config.rightLocationMultiplier(),
                config.uniquenessMultiplier(),
                config.partitionThreshold(),
                config.viableWordPreference(),
                config.useHarmonic(),
                countMap.getOrDefault(1, 0L),
                countMap.getOrDefault(2, 0L),
                countMap.getOrDefault(3, 0L),
                countMap.getOrDefault(4, 0L),
                countMap.getOrDefault(5, 0L),
                countMap.getOrDefault(6, 0L),
                countMap.getOrDefault(7, 0L),
                config.locationAdjustmentScale(),
                config.uniqueAdjustmentScale(),
                config.rutBreakThreshold(),
                config.rutBreakMultiplier(),
                report.firstWord()
        );
    }

    @Nested
    class StarterWordSearch {

        @Test
        void differentStartingWords() {
            List<String> words = List.of("TARSE", "SALET", "REAST", "SLATE", "CRATE", "TRACE", "CARLE", "TORSE");
            for (String firstWord : words) {
                WordCalculationConfig config = WordCalculationConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING.withHardMode(false);
                addStats(config, solvleService.solveDictionary(
                        new RemainingSolver(solvleService, config), firstWord.toLowerCase(), config, DictionaryType.SIMPLE));
            }
        }

        /**
         * Long-running exploration helper: solve the full dictionary while forcing the first TWO guesses.
         * Edit the two words below to whatever pair you want to analyze.
         */
        @Test
        void twoForcedStartingWords() {
            List<String> startingWords = List.of("salet", "porin");
            WordCalculationConfig config = WordCalculationConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING.withHardMode(false).withRequireAnswer(true);
            addStats(config, solvleService.solveDictionary(
                    new RemainingSolver(solvleService, config),
                    startingWords.stream().map(String::toLowerCase).toList(),
                    config,
                    DictionaryType.EXTENDED
            ));
        }
    }

    @Nested
    class ParameterSweeps {

        @ParameterizedTest
        @CsvSource({
                "2, 20",
                "2, 10",
                "2, 50",
        })
        void fishingAndPermutationThresholdGrid(int fishingThreshold, int permutationThreshold) {
            log.info("Starting permutation solver {}, {}", fishingThreshold, permutationThreshold);
            String firstWord = "";
            WordCalculationConfig config = new WordCalculationConfig(3, 5, permutationThreshold, 0.0).withFishingThreshold(2);
            solvleService.solveDictionary(new RemainingSolver(solvleService, config), firstWord, config, DictionaryType.SIMPLE);
        }

        @ParameterizedTest
        @CsvSource({
                "3, 4, 3, 0, .25, 1, 0, 0, 0.9",
                "3, 8, 3, 0, .25, 1, 0, 1, 0.9",
                "3, 3, 8, 0, .1, 1, 0, 0, 0.7",
                "3, 4, 3, 0, 1, 1, 0, 0, 0.9",
                "3, 8, 3, 0, 1, 1, 0, 1, 0.9",
                "3, 3, 8, 0, 1, 1, 0, 0.5, 0.7",
                "3, 4, 3, 30, .25, 1, 0, 0, 0.9",
                "3, 8, 3, 30, .25, 1, 0, 1, 0.9",
                "3, 3, 8, 30, .1, 1, 0, 0, 0.7",
        })
        void hyperparameterGrid(int fishingThreshold, double rightLocationMultiplier, double uniquenessMultiplier,
                                int permutationThreshold, double viableWordPreference,
                                double locationAdjustmentScale, double uniqueAdjustmentScale,
                                double viableWordAdjustmentScale, double vowelMultiplier) {
            log.info("Starting permutation solver {}, {}", fishingThreshold, permutationThreshold);
            String firstWord = "";
            WordCalculationConfig config = new WordCalculationConfig(rightLocationMultiplier, uniquenessMultiplier, permutationThreshold, viableWordPreference)
                    .withFishingThreshold(fishingThreshold)
                    .withFineTuning(locationAdjustmentScale, uniqueAdjustmentScale, viableWordAdjustmentScale, vowelMultiplier);
            var outcome = solvleService.solveDictionary(new RemainingSolver(solvleService, config), firstWord, config, DictionaryType.SIMPLE);
            addStats(config, outcome);
        }

        @ParameterizedTest
        @CsvSource({
                "2, 4, 9, 100, false, .007",
        })
        void hyperparameterWithStartingRestrictions(int fishingThreshold, double rightLocationMultiplier,
                                                    double uniquenessMultiplier, int permutationThreshold,
                                                    boolean useHarmonic, double viableWordPreference) {
            log.info("Starting permutation solver {}, {}", fishingThreshold, permutationThreshold);
            String firstWord = "";
            String startingRestrictions = "BCDFGHIJKMNOPQRUVWXYZ";
            WordCalculationConfig config = new WordCalculationConfig(rightLocationMultiplier, uniquenessMultiplier, permutationThreshold, viableWordPreference)
                    .withFishingThreshold(fishingThreshold);
            addStats(config, solvleService.solveDictionary(
                    new RemainingSolver(solvleService, config), List.of(firstWord), config, startingRestrictions, DictionaryType.SIMPLE));
        }

        @ParameterizedTest
        @MethodSource("hyperparameterLargeGridParameters")
        void hyperparameterLargeGrid(WordCalculationConfig config) {
            log.info("Starting permutation solver {}", config);
            String firstWord = "";
            addStats(config, solvleService.solveDictionary(new RemainingSolver(solvleService, config), firstWord, config, DictionaryType.SIMPLE));
        }

        static Stream<Arguments> hyperparameterLargeGridParameters() {
            List<Arguments> args = new ArrayList<>();
            List<Integer> locationMults = List.of(3, 4, 5, 6, 7, 8, 9);
            List<Integer> uniqueMults = List.of(3, 4, 5, 6, 7, 8, 9);
            List<Double> viableWordPrefs = List.of(2.0);
            List<Double> vowelMults = List.of(1.0);
            List<Integer> partThreshs = List.of(0);
            List<Double> locAdjusts = List.of(0.0);
            List<Double> uniqAdjusts = List.of(0.0);
            List<Double> vwAdjusts = List.of(0.0, 1.0, -1.0);

            double timePerCase = 9.0;
            double tests = locationMults.size() * uniqueMults.size() * viableWordPrefs.size() * vowelMults.size()
                    * partThreshs.size() * locAdjusts.size() * uniqAdjusts.size() * vwAdjusts.size();
            log.info("Generating {} tests. Estimated time: {}", tests, DurationFormatUtils.formatDurationHMS((long) (tests * timePerCase * 1000)));

            for (int locMult : locationMults) {
                for (int uniqueMult : uniqueMults) {
                    for (double viableWordPref : viableWordPrefs) {
                        for (double vowelMult : vowelMults) {
                            for (int partThresh : partThreshs) {
                                for (double locAdjust : locAdjusts) {
                                    for (double uniqAdjust : uniqAdjusts) {
                                        for (double vwAdjust : vwAdjusts) {
                                            args.add(Arguments.of(new WordCalculationConfig(locMult, uniqueMult, partThresh, viableWordPref)
                                                    .withFineTuning(locAdjust, uniqAdjust, vwAdjust, vowelMult)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return args.stream();
        }
    }

    @Nested
    class OneShotInspection {

        @Test
        void playOut() {
            WordConfig config = WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING;
            String wordRestrictions = "aáäbcdðeéfghiíjklmnñoópqrsßtuúüvwxyýzþæö";
            log.info("Playout requested with configuration {}", config);
            Set<PlayOut> result = solvleService.playOutSolutions(wordRestrictions.toLowerCase(), DictionaryType.SIMPLE, config, false, 2);
            log.info(result);
        }

        @Test
        void findRestrictions() {
            final int wordLength = 5;
            final DictionaryType dictionary = DictionaryType.SIMPLE;
            SharedPositions out = solvleService.findSharedWordRestrictions(dictionary);

            Word allLetters = new Word("aáäbcdðeéfghiíjklmnñoópqrsßtuúüvwxyýzþæö");
            out.sortedPositionStream().forEach(es -> {
                if (es.getKey().getShared() > 2 && es.getValue().size() > 5) {
                    WordRestrictions restrictions = new WordRestrictions(allLetters, new HashSet<>(es.getKey().pos().values()),
                            es.getKey().pos(), new HashMap<>(), new HashMap<>());
                    SolvleDTO solution = solvleService.getWordAnalysis(restrictions, dictionary, WordConfig.OPTIMAL_MEAN, false, false);
                    log.warn("{} {} Words: {} \n{} Recommended: {}",
                            es.getKey(), es.getValue().size(), es.getValue(),
                            formatKnownPosition(es.getKey(), wordLength),
                            solution.bestWords().stream().limit(5).map(WordFrequencyScore::word).collect(Collectors.toSet()));
                }
            });
        }

        @Test
        void findAnalysis() {
            WordRestrictions restrictions = WordRestrictions.NO_RESTRICTIONS;
            var out = solvleService.getWordAnalysis(restrictions, DictionaryType.SIMPLE, WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING, false, false);
            log.info("Words: " + out.wordList());
            log.info("Fish: " + out.fishingWords());
            log.info("Partition: " + out.bestWords());
        }

        @Test
        void findBestWords() {
            WordConfig config = WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING;
            var bestTuples = solvleService.findBestNWords(2, DictionaryType.ICELANDIC, config, true);
            int i = 0;
            log.info("|Guesses|Entropy|Remaining Words|");
            log.info("|-----|-----|-----|");
            for (TupleScore bestTuple : bestTuples) {
                log.info("|{}|{}|{}|",
                        bestTuple.tuple().stream().map(Word::word).sorted().toList(),
                        String.format("%.3f", bestTuple.partitionStats().entropy()),
                        String.format("%.3f", bestTuple.partitionStats().wordsRemaining()));
                if (i++ >= 99) {
                    break;
                }
            }
        }

        @ParameterizedTest
        @CsvSource({
                "'arise,pound'",
                "'soare,clint'"
        })
        void scoreTuple(String words) {
            Set<Word> tuple = Arrays.stream(words.split(",")).map(Word::new).collect(Collectors.toSet());
            var tupleScore = solvleService.scoreTuple(tuple, DictionaryType.SIMPLE);
            log.info(tupleScore);
        }

        @ParameterizedTest
        @CsvSource({
                "'arise'",
                "'donut'"
        })
        void finishTuple(String words) {
            Set<Word> tuple = Arrays.stream(words.split(",")).map(Word::new).collect(Collectors.toSet());
            var dictionary = DictionaryType.SIMPLE;
            boolean requireAnswer = false;

            var results = solvleService.submitTupleJob(tuple, dictionary, requireAnswer);
            while (Set.of(JobStatus.PENDING, JobStatus.RUNNING).contains(results.getStatus())) {
                results = solvleService.submitTupleJob(tuple, dictionary, requireAnswer);
                log.info(results);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
            results.getResult().forEach(t -> log.info("{}: {} remaining, {} entropy", t.tuple(),
                    String.format("%.3f", t.partitionStats().wordsRemaining()),
                    String.format("%.3f", t.partitionStats().entropy())));
        }
    }

    static String formatKnownPosition(KnownPosition kp, int wordLength) {
        String out = "";
        for (int i = 0; i < wordLength; i++) {
            if (kp.pos().containsKey(i + 1)) {
                out += kp.pos().get(i + 1);
            } else {
                out += "\\_";
            }
        }
        return out.toUpperCase();
    }
}
