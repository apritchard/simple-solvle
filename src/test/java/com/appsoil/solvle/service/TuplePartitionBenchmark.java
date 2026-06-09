package com.appsoil.solvle.service;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.config.SolvleConfig;
import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.data.WordRestrictions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Throwaway micro-benchmark (not a correctness test) comparing the old rescan-based partition
 * "remaining" computation against the optimized count-based path, on the real dictionary, for a
 * one-word tuple. Run manually with: mvn -o test -Dtest=TuplePartitionBenchmark -Djacoco.skip=true
 */
@Disabled("Manual micro-benchmark (~7 min); remove @Disabled and run with -Djacoco.skip=true to measure")
class TuplePartitionBenchmark {

    @Test
    void benchmarkCraneTuple() {
        Map<DictionaryType, Dictionary> dictionaries = new SolvleConfig().allDictionaries();
        Set<Word> solutions = dictionaries.get(DictionaryType.SIMPLE).wordsBySize().get(5);
        Set<Word> fishing = dictionaries.get(DictionaryType.BIG).wordsBySize().get(5);
        WordCalculationService service = new WordCalculationService(WordCalculationConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING);

        Word first = new Word("crane");
        // Mirror the real job's candidate filter: no duplicate letters, and at most one letter shared
        // with the input word.
        List<Word> candidates = fishing.stream()
                .filter(w -> noDuplicateLetters(w) && sharedLetters(first, w) <= 1)
                .toList();

        System.out.printf("solutions=%d  fishing=%d  surviving candidates for 'crane'=%d%n",
                solutions.size(), fishing.size(), candidates.size());

        // Warm up the JIT on a subset.
        List<Word> warm = candidates.subList(0, Math.min(300, candidates.size()));
        for (Word c : warm) {
            optimized(service, solutions, first, c);
            reference(service, solutions, first, c);
        }

        long tOpt = 0, tRef = 0;
        double sink = 0;
        for (Word c : candidates) {
            long s1 = System.nanoTime();
            sink += optimized(service, solutions, first, c);
            tOpt += System.nanoTime() - s1;

            long s2 = System.nanoTime();
            sink += reference(service, solutions, first, c);
            tRef += System.nanoTime() - s2;
        }

        double optMs = tOpt / 1_000_000.0;
        double refMs = tRef / 1_000_000.0;
        System.out.printf("OPTIMIZED full sweep: %.0f ms  (%.1f us/candidate)%n", optMs, tOpt / 1000.0 / candidates.size());
        System.out.printf("REFERENCE full sweep: %.0f ms  (%.1f us/candidate)%n", refMs, tRef / 1000.0 / candidates.size());
        System.out.printf("speedup: %.1fx   (sink=%.3f)%n", refMs / optMs, sink);
    }

    private static double optimized(WordCalculationService service, Set<Word> solutions, Word first, Word second) {
        Set<Word> tuple = new HashSet<>();
        tuple.add(first);
        tuple.add(second);
        return service.getPartitionStatsForTuple(WordRestrictions.NO_RESTRICTIONS, solutions, tuple).wordsRemaining();
    }

    private static double reference(WordCalculationService service, Set<Word> solutions, Word first, Word second) {
        Map<WordRestrictions, Integer> groups = new HashMap<>();
        for (Word solution : solutions) {
            WordRestrictions effective = WordRestrictions.generateRestrictions(solution, first, WordRestrictions.NO_RESTRICTIONS);
            effective = WordRestrictions.generateRestrictions(solution, second, effective);
            groups.merge(effective, 1, Integer::sum);
        }
        double remaining = 0.0;
        for (Map.Entry<WordRestrictions, Integer> group : groups.entrySet()) {
            int matchingCount = service.findMatchingWords(solutions, group.getKey()).size();
            remaining += (double) matchingCount * group.getValue();
        }
        return remaining / solutions.size();
    }

    private static boolean noDuplicateLetters(Word w) {
        return w.letters().size() == w.getLength();
    }

    private static int sharedLetters(Word a, Word b) {
        Set<Character> shared = new HashSet<>(a.letters().keySet());
        shared.retainAll(b.letters().keySet());
        return shared.size();
    }
}
