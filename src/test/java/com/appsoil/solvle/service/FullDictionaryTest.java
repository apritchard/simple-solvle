package com.appsoil.solvle.service;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.config.SolvleConfig;
import com.appsoil.solvle.controller.SolvleDTO;
import com.appsoil.solvle.controller.WordScoreDTO;
import com.appsoil.solvle.data.*;
import com.appsoil.solvle.service.job.JobStatus;
import com.appsoil.solvle.service.solvers.RemainingSolver;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Disabled
@SpringBootTest(classes = {SolvleService.class, SolvleConfig.class})
@ActiveProfiles("test")
public class FullDictionaryTest {

    @Autowired
    SolvleService solvleService;

    static Map<WordCalculationConfig, TestReport> testReports;

    record TestReport(DescriptiveStatistics stats, String firstWord, List<List<String>> problems) {}

    @BeforeAll
    static void init() {
         testReports = new HashMap<>();
    }

    @AfterAll
    static void report() {
        log.warn("Begin report full report:");
        testReports.forEach(FullDictionaryTest::logReport);
    }

    static private void addStats(WordCalculationConfig config, Map<String, List<String>> solution) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        List<List<String>> problems = new ArrayList<>();
        String firstWord = solution.values().stream().findFirst().get().get(0);
        solution.forEach((k, v) -> {
            stats.addValue(v.size());
            if(v.size() > 0) {
                log.info(v);
                problems.add(v);
            }
        });
        TestReport report = new TestReport(stats, firstWord, problems);
        testReports.put(config, report);
        var countMap = Arrays.stream(report.stats().getSortedValues()).mapToInt(num -> (int) num).boxed().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        log.info(config);
        log.info("Mean: {}, StDv: {}, Median: {}, Counts: {}", report.stats().getMean(), report.stats().getStandardDeviation(), report.stats().getPercentile(50), countMap);
        logReport(config, report);
    }

    static private void logReport(WordCalculationConfig config, TestReport report) {
        //format data to copy into an annoying spreadsheet
        var countMap = Arrays.stream(report.stats().getSortedValues()).mapToInt(num -> (int) num).boxed().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
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
                countMap.getOrDefault(1, 0l),
                countMap.getOrDefault(2, 0l),
                countMap.getOrDefault(3, 0l),
                countMap.getOrDefault(4, 0l),
                countMap.getOrDefault(5, 0l),
                countMap.getOrDefault(6, 0l),
                countMap.getOrDefault(7, 0l),
                config.locationAdjustmentScale(),
                config.uniqueAdjustmentScale(),
                config.rutBreakThreshold(),
                config.rutBreakMultiplier(),
                report.firstWord()
        );
    }

    static private void printResults(Map<String, List<String>> results) {
        results.forEach((k, v) -> {
            log.warn(String.join(",", v));
        });
    }

    @Test
    public void testConfig() {
        String firstWord = "";
        WordCalculationConfig config = WordCalculationConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING.withHardMode(false).withRequireAnswer(true);
        addStats(config, solvleService.solveDictionary(new RemainingSolver(solvleService, config), firstWord.toLowerCase(), config, DictionaryType.SIMPLE));
    }

    @Test
    public void testWords() {
        List<String> words = List.of("TARSE", "SALET", "REAST", "SLATE", "CRATE", "TRACE", "CARLE", "TORSE");
        for(String firstWord : words) {
            WordCalculationConfig config = WordCalculationConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING.withHardMode(false);
            addStats(config, solvleService.solveDictionary(new RemainingSolver(solvleService, config), firstWord.toLowerCase(), config, DictionaryType.SIMPLE));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "2, 20",
            "2, 10",
            "2, 50",
    })
    public void dictionaryRemainingSolver( int fishingThreshold, int permutationThreshold ) {
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
    public void dictionaryRemainingPermutationSolver( int fishingThreshold, double rightLocationMultiplier, double uniquenessMultiplier, int permutationThreshold,double viableWordPreference,
                                                      double locationAdjustmentScale, double uniqueAdjustmentScale, double viableWordAdjustmentScale, double vowelMultiplier) {
        log.info("Starting permutation solver {}, {}", fishingThreshold, permutationThreshold);
        String firstWord = "";
        WordCalculationConfig config = new WordCalculationConfig(rightLocationMultiplier, uniquenessMultiplier, permutationThreshold, viableWordPreference).withFishingThreshold(fishingThreshold)
                .withFineTuning(locationAdjustmentScale, uniqueAdjustmentScale, viableWordAdjustmentScale, vowelMultiplier);
//                .withRutBreak(1.0, 6);
        var outcome = solvleService.solveDictionary(new RemainingSolver(solvleService, config), firstWord, config, DictionaryType.SIMPLE);
        addStats(config, outcome);
    }

    @ParameterizedTest
    @CsvSource({
            "2, 4, 9, 100, false, .007",
    })
    public void dictionaryRemainingPermutationSolverWithRestrictions( int fishingThreshold, double rightLocationMultiplier, double uniquenessMultiplier, int permutationThreshold, boolean useHarmonic, double viableWordPreference) {
        log.info("Starting permutation solver {}, {}", fishingThreshold, permutationThreshold);
        String firstWord = "";
        String startingRestrictions = "BCDFGHIJKMNOPQRUVWXYZ";
        WordCalculationConfig config = new WordCalculationConfig(rightLocationMultiplier, uniquenessMultiplier, permutationThreshold, viableWordPreference).withFishingThreshold(fishingThreshold);
        addStats(config, solvleService.solveDictionary(new RemainingSolver(solvleService, config), List.of(firstWord), config, startingRestrictions, DictionaryType.SIMPLE));
    }

    @ParameterizedTest
    @MethodSource("dictionaryPermutationParameters")
    public void dictionaryRemainingPermutationSolver2( WordCalculationConfig config) {
        log.info("Starting permutation solver {}", config);
        String firstWord = "";
        addStats(config, solvleService.solveDictionary(new RemainingSolver(solvleService, config), firstWord, config, DictionaryType.SIMPLE));
    }

    private static Stream<Arguments> dictionaryPermutationParameters() {
        List<Arguments> args = new ArrayList<>();
        List<Integer> locationMults = List.of(3,4,5,6,7,8,9);
        List<Integer> uniqueMults = List.of(3,4,5,6,7,8,9);
        List<Double> viableWordPrefs = List.of(2.0);
        List<Double> vowelMults = List.of(1.0);
        List<Integer> partThreshs = List.of(0);
        List<Double> locAdjusts = List.of(0.0);
        List<Double> uniqAdjusts = List.of(0.0);
        List<Double> vwAdjusts = List.of(0.0, 1.0, -1.0);

        double timePerCase = 9.0;
        double tests = locationMults.size() * uniqueMults.size() * viableWordPrefs.size() * vowelMults.size() * partThreshs.size() * locAdjusts.size() * uniqAdjusts.size() * vwAdjusts.size();

        log.info("Generating " + tests + " tests. Estimated time: " + DurationFormatUtils.formatDurationHMS((int)(tests*timePerCase*1000)));

        for(int locMult : locationMults) {
            for(int uniqueMult : uniqueMults) {
                for(double viableWordPref : viableWordPrefs) {
                    for(double vowelMult : vowelMults) {
                        for(int partThresh : partThreshs) {
                            for(double locAdjust : locAdjusts) {
                                for(double uniqAdjust : uniqAdjusts) {
                                    for(double vwAdjust : vwAdjusts) {
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


    @Test
    public void compareStandardConfigs() {
        String firstWord = "".toLowerCase();
        Arrays.stream(WordConfig.values()).forEach((config) -> {
            log.info("Running " + config.toString());
            WordCalculationConfig c = config.config.withHardMode(false);
            addStats(c, solvleService.solveDictionary(new RemainingSolver(solvleService, c), firstWord, c, DictionaryType.SIMPLE));
        });
    }

    @Test
    public void playOut() {
        WordConfig config = WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING;
        String wordRestrictions = "aáäbcdðeéfghiíjklmnñoópqrsßtuúüvwxyýzþæö";
        log.info("Playout requested with configuration {}", config);
        Set<PlayOut> result = solvleService.playOutSolutions(wordRestrictions.toLowerCase(), DictionaryType.SIMPLE, config, false, 2);
        log.info(result);
    }


    @Test
    public void findRestrictions() {
        final int wordLength = 5;
        final DictionaryType dictionary = DictionaryType.SIMPLE;
        SharedPositions out = solvleService.findSharedWordRestrictions(dictionary);

        Word allLetters = new Word("aáäbcdðeéfghiíjklmnñoópqrsßtuúüvwxyýzþæö");
        out.sortedPositionStream().forEach(es -> {
            if(es.getKey().getShared() > 2 && es.getValue().size() > 5) { // && notEndInS(es.getKey(), wordLength) && notEndinEd(es.getKey(), wordLength) && notEndinIng(es.getKey(), wordLength) ) {
                WordRestrictions restrictions = new WordRestrictions(allLetters, new HashSet<>(es.getKey().pos().values()), es.getKey().pos(), new HashMap<>(), new HashMap<>());
                SolvleDTO solution = solvleService.getWordAnalysis(restrictions, dictionary, WordConfig.OPTIMAL_MEAN, false, false);
                log.warn("{} {} Words: {} \n{} Recommended: {}", es.getKey(), es.getValue().size(), es.getValue(), formatKnownPosition(es.getKey(), wordLength), solution.bestWords().stream().limit(5).map(WordFrequencyScore::word).collect(Collectors.toSet()));
            }
        });
    }


    @Test
    public void findAnalysis() {
        WordRestrictions restrictions = WordRestrictions.NO_RESTRICTIONS;
        var out = solvleService.getWordAnalysis(restrictions, DictionaryType.SIMPLE, WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING, false, false);
        log.info("Words: " + out.wordList());
        log.info("Fish: " + out.fishingWords());
        log.info("Partition: " + out.bestWords());
    }

    @Test
    void findBestWords() {
        WordConfig config = WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING;
        var bestTuples = solvleService.findBestNWords( 2, DictionaryType.ICELANDIC, config, true);
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
    void testScoreTuple(String words) {
        Set<Word> tuple = Arrays.stream(words.split(",")).map(Word::new).collect(Collectors.toSet());
        var tupleScore = solvleService.scoreTuple(tuple, DictionaryType.SIMPLE);
        log.info(tupleScore);
    }

    @ParameterizedTest
    @CsvSource({
            "'arise'",
            "'donut'"
    })
    void testFinishTuple(String words) {
        Set<Word> tuple = Arrays.stream(words.split(",")).map(Word::new).collect(Collectors.toSet());
        var dictionary = DictionaryType.SIMPLE;
        boolean requireAnswer = false;

        var results = solvleService.submitTupleJob(tuple, dictionary, requireAnswer);
        while(Set.of(JobStatus.PENDING, JobStatus.RUNNING).contains(results.getStatus())) {
            results = solvleService.submitTupleJob(tuple, dictionary, requireAnswer);
            log.info(results);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

        }
        results.getResult().forEach( t -> log.info("{}: {} remaining, {} entropy", t.tuple(),
                String.format("%.3f", t.partitionStats().wordsRemaining()),
                String.format("%.3f", t.partitionStats().entropy())));
    }

    private static String formatKnownPosition(KnownPosition kp, int wordLength) {
        String out = "";
        for(int i = 0; i < wordLength; i++) {
            if(kp.pos().containsKey(i+1)){
                out += kp.pos().get(i+1);
            } else {
                out += "\\_";
            }
        }
        return out.toUpperCase();
    }

    private static boolean notEndInS(KnownPosition a, int wordLength) {
        return !a.pos().containsKey(wordLength-1) || a.pos().get(wordLength-1) != 's';
    }

    private static boolean notEndinEd(KnownPosition a, int wordLength) {
        return (!a.pos().containsKey(wordLength -2) || !a.pos().containsKey(wordLength-1))
                || (a.pos().get(wordLength -2) != 'e' || a.pos().get(wordLength-1) != 'd');
    }

    private static boolean notEndinIng(KnownPosition a, int wordLength) {
        return (!a.pos().containsKey(wordLength -3) || !a.pos().containsKey(wordLength -2) || !a.pos().containsKey(wordLength-1))
                || (a.pos().get(wordLength-3) != 'i' || a.pos().get(wordLength-2) != 'n' || a.pos().get(wordLength-1) != 'g');
    }



}
