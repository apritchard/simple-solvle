package com.appsoil.solvle.service;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.config.SolvleConfig;
import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.data.WordRestrictions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guards the optimization in {@link WordCalculationService#getPartitionStats} that, outside hard
 * mode, skips the per-group {@code findMatchingWords} rescan and uses the group's own count instead.
 *
 * <p>That shortcut is correct only if, for every partition group, the number of words consistent
 * with the group's generated restriction equals the number of solutions that produced it
 * (matchingCount == groupCount). This test reproduces the original rescan-based "remaining" metric
 * and asserts it matches the current production output across a broad sample of the real BIG
 * dictionary, for both the single-word path ({@code getPartitionStatsForWord}) and the tuple path
 * ({@code getPartitionStatsForTuple}). If the equality ever breaks, the count-based path would
 * silently change rankings, and these assertions fail.
 */
class WordCalculationServicePartitionParityTest {

    private static Set<Word> solutions;
    private static List<Word> guessSample;
    private static WordCalculationService service;

    @BeforeAll
    static void loadRealDictionaries() {
        Map<DictionaryType, Dictionary> dictionaries = new SolvleConfig().allDictionaries();
        solutions = dictionaries.get(DictionaryType.SIMPLE).wordsBySize().get(5);
        Set<Word> fishing = dictionaries.get(DictionaryType.BIG).wordsBySize().get(5);
        // OPTIMAL_MEAN_EXTENDED_PARTITIONING is the exact config the tuple job uses; hardMode=false.
        service = new WordCalculationService(WordCalculationConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING);
        // Deterministic spread across the fishing set so the sample covers a wide variety of words.
        guessSample = evenSample(fishing, 150);
    }

    @Test
    void singleWordRemainingMatchesRescanReference() {
        for (Word guess : guessSample) {
            double expected = referenceRemaining(solutions, List.of(guess));
            double actual = service.getPartitionStatsForWord(WordRestrictions.NO_RESTRICTIONS, solutions, guess)
                    .wordsRemaining();
            Assertions.assertEquals(expected, actual, 1e-9,
                    () -> "remaining mismatch for guess " + guess.word()
                            + " (matchingCount != groupCount for some partition)");
        }
    }

    @Test
    void tupleRemainingMatchesRescanReference() {
        Word first = new Word("crane");
        for (Word second : guessSample) {
            if (first.word().equals(second.word())) {
                continue;
            }
            // The same Set instance is fed to both the reference and production so the guesses are
            // applied in identical iteration order (generateRestrictions' min-frequency put is order
            // sensitive for letters shared across the tuple).
            Set<Word> tuple = new HashSet<>();
            tuple.add(first);
            tuple.add(second);

            double expected = referenceRemaining(solutions, new ArrayList<>(tuple));
            double actual = service.getPartitionStatsForTuple(WordRestrictions.NO_RESTRICTIONS, solutions, tuple)
                    .wordsRemaining();
            Assertions.assertEquals(expected, actual, 1e-9,
                    () -> "remaining mismatch for tuple crane+" + second.word()
                            + " (matchingCount != groupCount for some partition)");
        }
    }

    /**
     * The original "remaining" computation: partition the solutions by the restriction the tuple
     * generates, then for each group rescan the solution set with findMatchingWords and weight the
     * matching count by the group's probability. Equivalent to the pre-optimization getPartitionStats.
     */
    private double referenceRemaining(Set<Word> contained, List<Word> tupleInIterationOrder) {
        Map<WordRestrictions, Integer> groups = new HashMap<>();
        for (Word solution : contained) {
            WordRestrictions effective = WordRestrictions.NO_RESTRICTIONS;
            for (Word guess : tupleInIterationOrder) {
                effective = WordRestrictions.generateRestrictions(solution, guess, effective);
            }
            groups.merge(effective, 1, Integer::sum);
        }
        double remaining = 0.0;
        for (Map.Entry<WordRestrictions, Integer> group : groups.entrySet()) {
            int matchingCount = service.findMatchingWords(contained, group.getKey()).size();
            remaining += (double) matchingCount * group.getValue();
        }
        return remaining / contained.size();
    }

    private static List<Word> evenSample(Set<Word> words, int target) {
        List<Word> all = new ArrayList<>(words);
        int step = Math.max(1, all.size() / target);
        List<Word> sample = new ArrayList<>();
        for (int i = 0; i < all.size() && sample.size() < target; i += step) {
            sample.add(all.get(i));
        }
        return sample;
    }
}
