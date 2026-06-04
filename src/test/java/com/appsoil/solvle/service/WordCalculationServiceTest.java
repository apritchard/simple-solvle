package com.appsoil.solvle.service;

import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.PartitionStats;
import com.appsoil.solvle.data.SharedPositions;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.data.WordFrequencyScore;
import com.appsoil.solvle.data.WordRestrictions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WordCalculationServiceTest {

    WordCalculationService wordCalculationService = new WordCalculationService(WordCalculationConfig.OPTIMAL_MEAN);

    private final Set<Word> ALL_LETTERS_WORD_SET = Stream.of("how", "quickly", "daft", "jumping", "zebras", "vex").map(Word::new).collect(Collectors.toSet());
    private final Set<Character> CHARACTERS_IN_TWO_WORDS = Set.of('u', 'e', 'a', 'i');
    private final Set<Character> ALPHABET_SET = "abcdefghijklmnopqrstuvwxyz".chars().mapToObj(c -> (char)c).collect(Collectors.toSet());

    @Test
    void calculateCharacterCounts_allLettersPresent_countsCorrectly() {
        Map<Character, LongAdder> counts = wordCalculationService.calculateCharacterCounts(ALL_LETTERS_WORD_SET);

        ALPHABET_SET.forEach(c -> {
            if(CHARACTERS_IN_TWO_WORDS.contains(c)) {
                Assertions.assertEquals(2, counts.get(c).intValue());
            } else {
                Assertions.assertEquals(1, counts.get(c).intValue());
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
            "later, alert, 1.0",
            "abcde, abcdd, 0.8",
            "abcde, aaaaa, 0.2",
            "abcde, fghij, 0.0"})
    void calculateViableResults_singleSourceAndWord_returnsNumberOfSourceCharacters(String sourceWord, String viableWord, double score) {
        Map<Character, LongAdder> counts = wordCalculationService.calculateCharacterCounts(Set.of(new Word(sourceWord)));
        Set<Word> viableWords = Set.of(new Word(viableWord));

        Set<WordFrequencyScore> scores = wordCalculationService.calculateViableWords(viableWords, counts, 1, 0, 100, new HashMap<>());

        Assertions.assertEquals(score, scores.stream().findFirst().get().freqScore());
    }

    @ParameterizedTest
    @CsvSource({
            "later, alert, 1.0, a",
            "abcde, abcdd, 0.75, d",
            "abcde, aaaae, 0.25, e",
            "abcde, aaacd, 0.5, a",
            "abcde, aahij, 0.0, a",
            "abcde, fghij, 0.0, g"})
    void calculateFishingWords_singleSourceAndWord_excludesCharactersFromCount(String sourceWord, String viableWord, double score, Character requiredChar) {
        Map<Character, LongAdder> counts = wordCalculationService.calculateCharacterCounts(Set.of(new Word(sourceWord)));
        Set<Word> viableWords = Set.of(new Word(viableWord));

        Set<WordFrequencyScore> scores = wordCalculationService.calculateFishingWords(viableWords, counts, 1, 100, Set.of(requiredChar), new HashMap<>());

        Assertions.assertEquals(score, scores.stream().findFirst().get().freqScore());
    }

    @Test
    void calculateFishingWordsByPosition_priotizesNewLettersFollowedByPossibleSolutions() {
        Set<Word> words = Arrays.stream("AA, AB, AC, AD, BA, BB, BC, BD, CA, CB, CC, CD, DA, DB, DC, DD".split(", ")).map((String word) -> new Word(word)).collect(Collectors.toSet());
        Set<Word> allWords = getFormattedWords(words);
        Set<Word> viableWords = Stream.of("AA", "AC", "AD").map(Word::new).collect(Collectors.toSet());
        WordRestrictions restrictions = new WordRestrictions("A1B!2CD");
        //
        // solution: AA
        // first guess: AB
        // viable words: AA, AC, AD
        //
        // character counts: 1- a:3, 2- a:1, c:1, d:1
        //
        // ideal fishing words: CD, DC
        //

        var characterCounts = wordCalculationService.calculateCharacterCountsByPosition(viableWords);
        Set<WordFrequencyScore> scores = wordCalculationService.calculateFishingWordsByPosition(allWords, characterCounts, viableWords, 25, restrictions, new HashMap<>());

        Set<String> expected = Set.of("CD", "DC");

        Assertions.assertTrue(scores.stream().limit(2).map(WordFrequencyScore::word).collect(Collectors.toSet()).containsAll(expected), "Top solutions did not match expected");
        System.out.println(scores.toString());
    }

    @Test
    void calculateFishingWordsByCharacter2() {
        Set<Word> allWords = Stream.of("haver", "chivy", "bumph").map(Word::new).collect(Collectors.toSet());
        Set<Word> viableWords = Stream.of("hover", "mover", "homer", "joker", "poker", "rover", "boxer", "foyer", "roger").map(Word::new).collect(Collectors.toSet());
        WordRestrictions restrictions = new WordRestrictions("BE4!5FGHJKMO2PQR5UVXYZ".toLowerCase());

        var characterCounts = wordCalculationService.calculateCharacterCountsByPosition(viableWords);
        Set<WordFrequencyScore> scores = wordCalculationService.calculateFishingWordsByPosition(allWords, characterCounts, viableWords, 25, restrictions, new HashMap<>());

        System.out.println(scores.toString());
    }

    @Test
    void calculateFishingWordsByCharacter3() {
        Set<Word> allWords = Stream.of("haven", "hakim", "bumph").map(Word::new).collect(Collectors.toSet());
        Set<Word> viableWords = Stream.of("hover", "mover", "homer", "joker", "poker", "rover", "boxer", "roger").map(Word::new).collect(Collectors.toSet());
        WordRestrictions restrictions = new WordRestrictions("BE4!5FGHJKMO2PQR5!4UVXZ".toLowerCase());

        var characterCounts = wordCalculationService.calculateCharacterCountsByPosition(viableWords);
        Set<WordFrequencyScore> scores = wordCalculationService.calculateFishingWordsByPosition(allWords, characterCounts, viableWords, 25, restrictions, new HashMap<>());

        System.out.println(scores.toString());
    }

    @Test
    void calculateFreqScore_zeroTotalWordsOrMaxScore_returnsZero() {
        Assertions.assertEquals(0.0, wordCalculationService.calculateFreqScore(new Word("abc"), Map.of(), 0, 3, new HashMap<>()));
        Assertions.assertEquals(0.0, wordCalculationService.calculateFreqScore(new Word("abc"), Map.of(), 1, 0, new HashMap<>()));
    }

    @Test
    void removeRequiredLettersFromCountsByPosition_removesKnownAndInferredPositions() {
        Map<Integer, Map<Character, LongAdder>> countsByPosition = Map.of(
                1, Map.of('a', count(5), 'b', count(1)),
                2, Map.of('c', count(3)),
                3, Map.of('d', count(5), 'e', count(2)),
                4, Map.of('f', count(1), 'g', count(2), 'h', count(3))
        );
        WordRestrictions restrictions = new WordRestrictions(
                new Word("abcdefgh"),
                Set.of('a'),
                Map.of(1, 'a'),
                new HashMap<>(),
                new HashMap<>()
        );

        Map<Integer, Map<Character, LongAdder>> reduced = wordCalculationService.removeRequiredLettersFromCountsByPosition(countsByPosition, restrictions);

        Assertions.assertTrue(reduced.get(1).isEmpty(), "Known positions should be removed from positional fishing counts");
        Assertions.assertTrue(reduced.get(2).isEmpty(), "Single-option positions are implicitly known and should be removed");
        Assertions.assertEquals(Set.of('e'), reduced.get(3).keySet(), "Two-option positions keep only the least-common option");
        Assertions.assertEquals(2, reduced.get(3).get('e').intValue());
        Assertions.assertEquals(Set.of('f', 'g', 'h'), reduced.get(4).keySet(), "Positions with three or more options are kept intact");
    }

    @Test
    void calculateRemainingWords_respectsThresholdAndUsesTwoWordFastPath() {
        WordRestrictions restrictions = new WordRestrictions("ab");
        Set<Word> containedWords = getFormattedWords(Stream.of("aa", "ab").map(Word::new).collect(Collectors.toSet()));
        Set<WordFrequencyScore> viableScores = Set.of(
                new WordFrequencyScore(1, "aa", 1.0, null),
                new WordFrequencyScore(2, "ab", 0.5, null)
        );

        WordCalculationService disabledPartitioning = new WordCalculationService(new WordCalculationConfig(0, 0, 1, 0));
        WordCalculationService enabledPartitioning = new WordCalculationService(new WordCalculationConfig(0, 0, 2, 0));

        Assertions.assertTrue(disabledPartitioning.calculateRemainingWords(restrictions, containedWords, viableScores, Set.of()).isEmpty());

        Set<WordFrequencyScore> remaining = enabledPartitioning.calculateRemainingWords(restrictions, containedWords, viableScores, Set.of());

        Assertions.assertEquals(Set.of("aa", "ab"), remaining.stream().map(WordFrequencyScore::word).collect(Collectors.toSet()));
        remaining.forEach(score -> Assertions.assertEquals(0.5, score.freqScore()));
    }

    @Test
    void mergeWordPools_deduplicatesViableAndFishingScoresByWord() {
        Set<WordFrequencyScore> viableScores = Set.of(
                new WordFrequencyScore(1, "crane", 2.0, null),
                new WordFrequencyScore(2, "slate", 1.5, null)
        );
        Set<WordFrequencyScore> fishingScores = Set.of(
                new WordFrequencyScore(1, "crane", 3.0, null),
                new WordFrequencyScore(3, "adieu", 1.0, null)
        );

        Set<Word> pool = wordCalculationService.mergeWordPools(viableScores, fishingScores);

        Assertions.assertEquals(Set.of("crane", "slate", "adieu"), pool.stream().map(Word::word).collect(Collectors.toSet()));
    }

    @Test
    void getPartitionStatsForWord_calculatesAverageRemainingAndEntropy() {
        Set<Word> containedWords = Stream.of("aa", "ab").map(Word::new).collect(Collectors.toSet());

        PartitionStats stats = wordCalculationService.getPartitionStatsForWord(new WordRestrictions("ab"), containedWords, new Word("aa"));

        Assertions.assertEquals(1.0, stats.wordsRemaining());
        Assertions.assertEquals(2, stats.groupCount());
        Assertions.assertEquals(1.0, stats.entropy());
        Assertions.assertTrue(stats.ruts().isEmpty());
    }

    @Test
    void sharedPositionWeights_rewardUnknownLettersInLargeEnoughRuts() {
        WordCalculationService rutBreakingService = new WordCalculationService(
                new WordCalculationConfig(3, 8, 10, 0).withRutBreak(2.0, 3)
        );
        Set<Word> rutWords = Stream.of("abcde", "abfde", "abgde").map(Word::new).collect(Collectors.toSet());

        SharedPositions sharedPositions = rutBreakingService.findSharedWordRestrictions(rutWords);
        Map<Character, DoubleAdder> weights = rutBreakingService.generateSharedCharacterWeights(
                sharedPositions,
                new WordRestrictions("abcdefg")
        );

        Assertions.assertEquals(3, sharedPositions.largestSet());
        Assertions.assertEquals(List.of("ab_de"), sharedPositions.getDescription());
        Assertions.assertEquals(2.0, weights.get('c').doubleValue());
        Assertions.assertEquals(2.0, weights.get('f').doubleValue());
        Assertions.assertEquals(2.0, weights.get('g').doubleValue());

        Map<Character, DoubleAdder> weightsWithKnownC = rutBreakingService.generateSharedCharacterWeights(
                sharedPositions,
                new WordRestrictions(new Word("abcdefg"), Set.of('c'), Map.of(), new HashMap<>(), new HashMap<>())
        );

        Assertions.assertFalse(weightsWithKnownC.containsKey('c'));
        Assertions.assertEquals(2.0, weightsWithKnownC.get('f').doubleValue());
        Assertions.assertEquals(2.0, weightsWithKnownC.get('g').doubleValue());
    }

    private static Set<Word> getFormattedWords(Set<Word> words) {
        int size = words.stream().findFirst().get().getLength();
        var wordMap = Map.of(size, words);
        var d = new Dictionary(wordMap);
        return d.wordsBySize().get(size);
    }

    private static LongAdder count(int value) {
        LongAdder adder = new LongAdder();
        adder.add(value);
        return adder;
    }

}
