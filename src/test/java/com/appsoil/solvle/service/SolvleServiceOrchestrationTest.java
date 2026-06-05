package com.appsoil.solvle.service;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.controller.GameScoreDTO;
import com.appsoil.solvle.controller.SolvleDTO;
import com.appsoil.solvle.controller.WordScoreDTO;
import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.PlayOut;
import com.appsoil.solvle.data.SharedPositions;
import com.appsoil.solvle.data.TupleScore;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.data.WordFrequencyScore;
import com.appsoil.solvle.data.WordRestrictions;
import com.appsoil.solvle.service.job.JobStatus;
import com.appsoil.solvle.service.job.SolveJob;
import com.appsoil.solvle.service.solvers.RemainingSolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest(classes = {SolvleService.class, SolvleServiceOrchestrationTest.SolvleOrchestrationConfiguration.class})
@ActiveProfiles("test")
class SolvleServiceOrchestrationTest {

    @TestConfiguration
    static class SolvleOrchestrationConfiguration {
        @Bean
        Map<DictionaryType, Dictionary> allDictionaries() {
            Map<DictionaryType, Dictionary> dictionaries = new EnumMap<>(DictionaryType.class);
            dictionaries.put(DictionaryType.SIMPLE, dictionary("crane", "trace"));
            dictionaries.put(DictionaryType.EXTENDED, dictionary("crane", "trace", "brine", "gulps"));
            dictionaries.put(DictionaryType.REDUCED, dictionary("crane"));
            dictionaries.put(DictionaryType.BIG, dictionary("crane", "trace", "slate", "pound", "fjord"));
            dictionaries.put(DictionaryType.SPANISH, dictionary("noche", "canto"));
            dictionaries.put(DictionaryType.ICELANDIC, dictionary("cielo"));
            dictionaries.put(DictionaryType.ICELANDIC_FISHING, dictionary("cielo", "fjord"));
            dictionaries.put(DictionaryType.GERMAN_6MAL5, dictionary("geist"));
            dictionaries.put(DictionaryType.GERMAN_WORDLE_GLOBAL, dictionary("ander"));
            return dictionaries;
        }

        private static Dictionary dictionary(String... words) {
            Set<Word> wordSet = Stream.of(words)
                    .map(Word::new)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return new Dictionary(Map.of(5, wordSet));
        }
    }

    @Autowired
    SolvleService solvleService;

    @Test
    void getWordAnalysis_englishSolutionListsUseBigDictionaryForFishing() {
        SolvleDTO result = solvleService.getWordAnalysis(
                WordRestrictions.NO_RESTRICTIONS,
                DictionaryType.SIMPLE,
                WordConfig.SIMPLE,
                false,
                false
        );

        Set<String> viableWords = words(result.wordList());
        Set<String> fishingWords = words(result.fishingWords());

        Assertions.assertEquals(Set.of("crane", "trace"), viableWords);
        Assertions.assertTrue(fishingWords.contains("slate"), "English fishing guesses should come from BIG");
        Assertions.assertTrue(fishingWords.contains("pound"), "English fishing guesses should include non-solution BIG words");
    }

    @Test
    void getWordAnalysis_requireAnswerRestrictsFishingWordsToSolutionSet() {
        SolvleDTO result = solvleService.getWordAnalysis(
                WordRestrictions.NO_RESTRICTIONS,
                DictionaryType.SIMPLE,
                WordConfig.SIMPLE,
                false,
                true
        );

        Assertions.assertEquals(Set.of("crane", "trace"), words(result.wordList()));
        Assertions.assertEquals(Set.of("crane", "trace"), words(result.fishingWords()));
    }

    @Test
    void getWordAnalysis_hardModeFiltersFishingWordsThroughCurrentRestrictions() {
        WordRestrictions restrictions = new WordRestrictions("c1rane");

        SolvleDTO relaxed = solvleService.getWordAnalysis(restrictions, DictionaryType.SIMPLE, WordConfig.SIMPLE, false, false);
        SolvleDTO hardMode = solvleService.getWordAnalysis(restrictions, DictionaryType.SIMPLE, WordConfig.SIMPLE, true, false);

        Assertions.assertEquals(Set.of("crane"), words(relaxed.wordList()));
        Assertions.assertTrue(words(relaxed.fishingWords()).contains("slate"), "Non-hard mode can still suggest unrestricted fishing words");
        Assertions.assertEquals(Set.of("crane"), words(hardMode.fishingWords()));
    }

    @Test
    void getWordAnalysis_languageDictionariesDoNotFallbackToBigFishingWords() {
        SolvleDTO spanish = solvleService.getWordAnalysis(
                WordRestrictions.NO_RESTRICTIONS,
                DictionaryType.SPANISH,
                WordConfig.SIMPLE,
                false,
                false
        );
        SolvleDTO icelandic = solvleService.getWordAnalysis(
                WordRestrictions.NO_RESTRICTIONS,
                DictionaryType.ICELANDIC,
                WordConfig.SIMPLE,
                false,
                false
        );

        Assertions.assertEquals(Set.of("noche", "canto"), words(spanish.fishingWords()));
        Assertions.assertFalse(words(spanish.fishingWords()).contains("slate"));

        Assertions.assertEquals(Set.of("cielo"), words(icelandic.wordList()));
        Assertions.assertTrue(words(icelandic.fishingWords()).contains("fjord"));
        Assertions.assertFalse(words(icelandic.fishingWords()).contains("slate"));
    }

    @Test
    void getScore_returnsFiniteScoreAndPartitionStatsForCandidate() {
        WordScoreDTO score = solvleService.getScore(
                WordRestrictions.NO_RESTRICTIONS,
                "crane",
                DictionaryType.SIMPLE,
                WordConfig.SIMPLE,
                false,
                false
        );

        Assertions.assertTrue(score.remainingWords() > 0);
        Assertions.assertTrue(score.fishingScore() > 0);
        Assertions.assertTrue(score.entropy() >= 0);
    }

    @Test
    void rateGame_returnsRowsForEachGuessAndTracksActualRemainingWords() {
        GameScoreDTO gameScore = solvleService.rateGame(
                "crane",
                List.of("trace", "crane"),
                DictionaryType.SIMPLE,
                WordConfig.SIMPLE,
                false,
                false
        );

        Assertions.assertEquals(2, gameScore.getRows().size());
        Assertions.assertEquals("trace", gameScore.getRows().get(0).playerWord());
        Assertions.assertEquals("crane", gameScore.getRows().get(1).playerWord());
        Assertions.assertEquals(1, gameScore.getRows().get(1).actualRemaining());
    }

    @Test
    void solveWord_rejectsUnknownSolutionsAndInvalidFirstWords() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);

        Assertions.assertEquals(List.of("Word Not Found"),
                solvleService.solveWord(solver, new Word("slate"), "", DictionaryType.SIMPLE));
        Assertions.assertEquals(List.of("First word not valid"),
                solvleService.solveWord(solver, new Word("crane"), "zzzzz", DictionaryType.SIMPLE));
    }

    @Test
    void findBestNWords_respectsRequireAnswerWhenChoosingAvailableGuesses() {
        Set<TupleScore> answerOnly = solvleService.findBestNWords(2, DictionaryType.SIMPLE, WordConfig.SIMPLE, true);
        Set<TupleScore> openGuessList = solvleService.findBestNWords(2, DictionaryType.SIMPLE, WordConfig.SIMPLE, false);

        Assertions.assertTrue(answerOnly.isEmpty());
        Assertions.assertFalse(openGuessList.isEmpty());
        Assertions.assertTrue(tupleWords(openGuessList).stream()
                .anyMatch(Set.of("slate", "pound", "fjord")::contains));
    }

    @Test
    void scoreTuple_returnsPartitionStatsForProvidedTupleAgainstSolutionSet() {
        TupleScore score = solvleService.scoreTuple(Set.of(new Word("crane")), DictionaryType.SIMPLE);

        Assertions.assertEquals(Set.of("crane"), score.tuple().stream().map(Word::word).collect(Collectors.toSet()));
        Assertions.assertTrue(score.partitionStats().wordsRemaining() > 0);
        Assertions.assertTrue(score.partitionStats().entropy() >= 0);
    }

    @Test
    void playOutSolutions_failuresPopulatedWhenGuessNumberLeavesNoRoom() {
        // playOutSolutions passes guessNumber to getWordsBySolveLength, which records a "failure" for
        // any solve whose guess list length is greater than (6 - guessNumber). At guessNumber=6 the
        // threshold is 0, so every non-empty guess list counts — the failures list in each PlayOut
        // becomes the full set of solutions.
        Set<PlayOut> playOuts = solvleService.playOutSolutions(
                "abcdefghijklmnopqrstuvwxyz",
                DictionaryType.SIMPLE,
                WordConfig.SIMPLE,
                false,
                6
        );

        Assertions.assertFalse(playOuts.isEmpty());
        Assertions.assertTrue(playOuts.stream().anyMatch(p -> !p.failures().isEmpty()),
                "At guessNumber=6 every successful solve registers as a failure in PlayOut.failures");
    }

    @Test
    void playOutSolutions_returnsPlayoutsDrawnFromMergedViableAndFishingPool() {
        Set<PlayOut> playOuts = solvleService.playOutSolutions(
                "abcdefghijklmnopqrstuvwxyz",
                DictionaryType.SIMPLE,
                WordConfig.SIMPLE,
                false,
                0
        );

        Assertions.assertFalse(playOuts.isEmpty(), "Playout pool should contain candidates for the tiny dictionary");
        playOuts.forEach(p -> Assertions.assertTrue(p.average() >= 1.0,
                () -> "PlayOut for " + p.word() + " should require at least one guess on average; got " + p.average()));

        Set<String> bigPool = Set.of("crane", "trace", "slate", "pound", "fjord");
        Set<String> playOutWords = playOuts.stream().map(PlayOut::word).collect(Collectors.toSet());
        Assertions.assertTrue(playOutWords.stream().anyMatch(bigPool::contains),
                () -> "Merged playout pool should draw from viable + fishing words, got " + playOutWords);
    }

    @Test
    void solveDictionary_blankFirstWordIsPickedFromAnalysisAndAppliedToEachSolution() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);
        Map<String, List<String>> outcome = solvleService.solveDictionary(
                solver, "", WordCalculationConfig.SIMPLE, DictionaryType.SIMPLE);

        Assertions.assertEquals(Set.of("crane", "trace"), outcome.keySet());

        String pickedFirstWord = outcome.values().iterator().next().get(0);
        Assertions.assertTrue(Set.of("crane", "trace", "slate", "pound", "fjord").contains(pickedFirstWord),
                () -> "First word should come from analysis fishing/best words; got " + pickedFirstWord);

        outcome.forEach((solution, guesses) -> {
            Assertions.assertEquals(pickedFirstWord, guesses.get(0),
                    "All solutions should start with the same analysis-picked first word");
            Assertions.assertEquals(solution, guesses.get(guesses.size() - 1),
                    "Final guess for each solution should be the solution itself");
        });
    }

    @Test
    void solveDictionary_explicitFirstWordIsUsedAsTheOpeningGuess() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);
        Map<String, List<String>> outcome = solvleService.solveDictionary(
                solver, "trace", WordCalculationConfig.SIMPLE, DictionaryType.SIMPLE);

        Assertions.assertEquals(List.of("trace"), outcome.get("trace"),
                "First word equal to solution returns just that word");

        List<String> craneGuesses = outcome.get("crane");
        Assertions.assertEquals("trace", craneGuesses.get(0));
        Assertions.assertEquals("crane", craneGuesses.get(craneGuesses.size() - 1));
    }

    @Test
    void solveDictionary_singleForcedStarterIsPrependedToGuessList() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);
        Map<String, List<String>> outcome = solvleService.solveDictionary(
                solver, List.of("slate"), WordCalculationConfig.SIMPLE, DictionaryType.SIMPLE);

        outcome.forEach((solution, guesses) -> {
            Assertions.assertEquals("slate", guesses.get(0),
                    "Forced starter should appear as the first guess for every solution");
            Assertions.assertEquals(solution, guesses.get(guesses.size() - 1),
                    "Final guess for each solution should be the solution itself");
        });
    }

    @Test
    void solveDictionary_multipleForcedStartersArePrependedInOrder() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);
        Map<String, List<String>> outcome = solvleService.solveDictionary(
                solver, List.of("slate", "pound"), WordCalculationConfig.SIMPLE, DictionaryType.SIMPLE);

        outcome.forEach((solution, guesses) -> {
            Assertions.assertEquals("slate", guesses.get(0));
            Assertions.assertEquals("pound", guesses.get(1));
            Assertions.assertEquals(solution, guesses.get(guesses.size() - 1));
        });
    }

    @Test
    void solveDictionary_forcedStarterEqualToSolutionShortCircuitsForThatSolution() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);
        Map<String, List<String>> outcome = solvleService.solveDictionary(
                solver, List.of("crane"), WordCalculationConfig.SIMPLE, DictionaryType.SIMPLE);

        Assertions.assertEquals(List.of("crane"), outcome.get("crane"),
                "Starter equal to solution returns just the starter");

        List<String> traceGuesses = outcome.get("trace");
        Assertions.assertEquals("crane", traceGuesses.get(0),
                "Other solutions still get the starter prepended");
        Assertions.assertEquals("trace", traceGuesses.get(traceGuesses.size() - 1));
    }

    @Test
    void solveDictionary_forcedStarterOfWrongLengthIsRejectedForEverySolution() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);
        Map<String, List<String>> outcome = solvleService.solveDictionary(
                solver, List.of("nope"), WordCalculationConfig.SIMPLE, DictionaryType.SIMPLE);

        outcome.values().forEach(guesses ->
                Assertions.assertEquals(List.of("First word not valid"), guesses));
    }

    @Test
    void solveDictionary_forcedStarterNotInFishingSetIsRejectedForEverySolution() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);
        Map<String, List<String>> outcome = solvleService.solveDictionary(
                solver, List.of("zzzzz"), WordCalculationConfig.SIMPLE, DictionaryType.SIMPLE);

        outcome.values().forEach(guesses ->
                Assertions.assertEquals(List.of("First word not valid"), guesses));
    }

    @Test
    void submitTupleJob_returnsCachedJobForRepeatedSubmits() {
        Set<Word> tuple = Set.of(new Word("crane"));

        SolveJob<Set<TupleScore>> first = solvleService.submitTupleJob(tuple, DictionaryType.SIMPLE, true);
        SolveJob<Set<TupleScore>> second = solvleService.submitTupleJob(tuple, DictionaryType.SIMPLE, true);

        Assertions.assertEquals(first.getId(), second.getId(),
                "Repeated submit with the same key should return the cached SolveJob");
    }

    @Test
    void submitTupleJob_restartsAfterFailure() throws InterruptedException {
        Set<Word> tuple = Set.of(new Word("trace"));

        SolveJob<Set<TupleScore>> first = solvleService.submitTupleJob(tuple, DictionaryType.SIMPLE, true);

        // The executor sets RUNNING then COMPLETED on the job; wait for it to finish so our
        // explicit FAILED status is not overwritten by the runnable still in flight.
        awaitTerminalStatus(first, 5000);
        first.setStatus(JobStatus.FAILED);

        SolveJob<Set<TupleScore>> second = solvleService.submitTupleJob(tuple, DictionaryType.SIMPLE, true);

        Assertions.assertNotEquals(first.getId(), second.getId(),
                "A FAILED cached job should be replaced on the next submit");
    }

    @Test
    void submitTupleJob_completesAndPopulatesResultForTinyDictionary() throws InterruptedException {
        Set<Word> tuple = Set.of(new Word("brine"));

        SolveJob<Set<TupleScore>> job = solvleService.submitTupleJob(tuple, DictionaryType.EXTENDED, true);

        awaitTerminalStatus(job, 5000);

        Assertions.assertEquals(JobStatus.COMPLETED, job.getStatus(),
                () -> "Tuple job should complete within 5s on the tiny dictionary; error: " + job.getError());
        Assertions.assertNotNull(job.getResult(), "Completed tuple job should expose a result set");
    }

    @Test
    void submitTupleJob_mapsCandidatesThatPassDuplicateLetterFilter() throws InterruptedException {
        // EXTENDED includes "gulps" which shares no letters with brine, so it survives isValidCombination
        // and the .map block builds a TupleScore around {brine, gulps}.
        Set<Word> tuple = Set.of(new Word("brine"));

        SolveJob<Set<TupleScore>> job = solvleService.submitTupleJob(tuple, DictionaryType.EXTENDED, true);
        awaitTerminalStatus(job, 5000);

        Assertions.assertEquals(JobStatus.COMPLETED, job.getStatus(),
                () -> "Tuple job should complete; error: " + job.getError());
        Set<TupleScore> result = job.getResult();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty(), "At least one letter-disjoint candidate should produce a TupleScore");
        Assertions.assertTrue(result.stream().anyMatch(ts -> ts.tuple().stream()
                        .map(Word::word).collect(Collectors.toSet()).containsAll(Set.of("brine", "gulps"))),
                () -> "Expected a tuple containing brine + gulps, got: " + result);
    }

    @Test
    void getScore_stringEntryPointParsesRestrictionsAndDelegates() {
        WordScoreDTO score = solvleService.getScore(
                "abcdefghijklmnopqrstuvwxyz",
                "crane",
                DictionaryType.SIMPLE,
                WordConfig.SIMPLE,
                false,
                false
        );

        Assertions.assertTrue(score.remainingWords() > 0);
        Assertions.assertTrue(score.fishingScore() > 0);
        Assertions.assertTrue(score.entropy() >= 0);
    }

    @Test
    void getScore_positionalBranchUsedWhenRightLocationMultiplierIsNonZero() {
        // WordConfig.OPTIMAL_MEAN has rightLocationMultiplier=3, so getScore goes through
        // calculateFreqScoreByPosition (line 216) instead of the non-positional branch.
        WordScoreDTO score = solvleService.getScore(
                WordRestrictions.NO_RESTRICTIONS,
                "crane",
                DictionaryType.SIMPLE,
                WordConfig.OPTIMAL_MEAN,
                false,
                false
        );

        Assertions.assertTrue(score.remainingWords() > 0);
        Assertions.assertTrue(score.fishingScore() > 0);
    }

    @Test
    void getWordAnalysis_partitioningBranchPopulatesBestWords() {
        // WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING has partitionThreshold=4000, so the else
        // branch at SolvleService:132 runs and calculateRemainingWords produces bestWords.
        SolvleDTO result = solvleService.getWordAnalysis(
                WordRestrictions.NO_RESTRICTIONS,
                DictionaryType.SIMPLE,
                WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING,
                false,
                false
        );

        Assertions.assertNotNull(result.bestWords(), "Partitioning config should produce a non-null bestWords");
        Assertions.assertFalse(result.bestWords().isEmpty(), "With viable words and partition enabled, bestWords should be non-empty");
    }

    @Test
    void solveDictionary_blankFirstWordPicksFromBestWordsWhenPartitioningEnabled() {
        // With partitioning enabled and a non-empty bestWords set, solveDictionary's blank-firstWord
        // branch takes the bestWords path (line 327-328) instead of the fishingWords fallback.
        WordCalculationConfig config = WordCalculationConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING;
        RemainingSolver solver = new RemainingSolver(solvleService, config);

        Map<String, List<String>> outcome = solvleService.solveDictionary(
                solver, "", config, DictionaryType.SIMPLE);

        Assertions.assertEquals(Set.of("crane", "trace"), outcome.keySet());
        String pickedFirstWord = outcome.values().iterator().next().get(0);
        Assertions.assertTrue(Set.of("crane", "trace").contains(pickedFirstWord),
                () -> "First word should be a viable bestWords pick (containedWords <=2 fast path), got " + pickedFirstWord);
    }

    @Test
    void getWordAnalysis_germanDictionariesRouteToTheirOwnFishingSets() {
        SolvleDTO german6mal5 = solvleService.getWordAnalysis(
                WordRestrictions.NO_RESTRICTIONS,
                DictionaryType.GERMAN_6MAL5,
                WordConfig.SIMPLE,
                false,
                false
        );
        SolvleDTO germanGlobal = solvleService.getWordAnalysis(
                WordRestrictions.NO_RESTRICTIONS,
                DictionaryType.GERMAN_WORDLE_GLOBAL,
                WordConfig.SIMPLE,
                false,
                false
        );

        Assertions.assertEquals(Set.of("geist"), words(german6mal5.wordList()));
        Assertions.assertEquals(Set.of("geist"), words(german6mal5.fishingWords()),
                "GERMAN_6MAL5 should use its own dictionary for fishing, not BIG");
        Assertions.assertEquals(Set.of("ander"), words(germanGlobal.wordList()));
        Assertions.assertEquals(Set.of("ander"), words(germanGlobal.fishingWords()),
                "GERMAN_WORDLE_GLOBAL should use its own dictionary for fishing, not BIG");
    }

    @Test
    void findSharedWordRestrictions_returnsSharedPositionsForDictionary() {
        SharedPositions positions = solvleService.findSharedWordRestrictions(DictionaryType.SIMPLE);

        Assertions.assertNotNull(positions);
        Assertions.assertNotNull(positions.knownPositions());
    }

    @Test
    void solveDictionary_previousGuessesAndStartingRestrictionsPrependsHistoryAndUsesRestrictions() {
        WordCalculationConfig config = WordCalculationConfig.SIMPLE;
        RemainingSolver solver = new RemainingSolver(solvleService, config);

        Map<String, List<String>> outcome = solvleService.solveDictionary(
                solver,
                List.of("aaaaa"),
                config,
                "abcdefghijklmnopqrstuvwxyz",
                DictionaryType.SIMPLE
        );

        Assertions.assertEquals(Set.of("crane", "trace"), outcome.keySet());
        outcome.forEach((solution, guesses) -> {
            Assertions.assertEquals("aaaaa", guesses.get(0),
                    "Each solution's guess list should start with the prepended previousGuesses entry");
            Assertions.assertEquals(solution, guesses.get(guesses.size() - 1),
                    "Final guess for each solution should be the solution");
        });
    }

    private static void awaitTerminalStatus(SolveJob<?> job, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            JobStatus status = job.getStatus();
            if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
                return;
            }
            Thread.sleep(10);
        }
    }

    private static Set<String> words(Set<WordFrequencyScore> scores) {
        return scores.stream().map(WordFrequencyScore::word).collect(Collectors.toSet());
    }

    private static Set<String> tupleWords(Set<TupleScore> scores) {
        return scores.stream()
                .flatMap(score -> score.tuple().stream())
                .map(Word::word)
                .collect(Collectors.toSet());
    }
}
