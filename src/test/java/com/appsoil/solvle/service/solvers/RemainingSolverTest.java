package com.appsoil.solvle.service.solvers;

import com.appsoil.solvle.controller.KnownPositionDTO;
import com.appsoil.solvle.controller.SolvleDTO;
import com.appsoil.solvle.data.WordFrequencyScore;
import com.appsoil.solvle.service.WordCalculationConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

/**
 * Direct unit tests for the pure guess-selection logic. These cover the three
 * branches of {@link RemainingSolver#getNextGuess}: fishing, partition, and
 * viable-word fallback, plus previous-guess avoidance and the terminal/null case.
 *
 * <p>These tests need no Spring context. The context-dependent full solve-loop tests live in the
 * separate {@link RemainingSolverSolveTest} class so that bootstrapping Spring is only paid for
 * where it is actually required.
 */
class RemainingSolverTest {

    private static WordFrequencyScore score(int naturalOrdering, String word) {
        return new WordFrequencyScore(naturalOrdering, word, 1.0, null);
    }

    private static Set<WordFrequencyScore> ordered(WordFrequencyScore... scores) {
        return new LinkedHashSet<>(List.of(scores));
    }

    private static SolvleDTO dto(int totalWords,
                                 Set<WordFrequencyScore> wordList,
                                 Set<WordFrequencyScore> fishingWords,
                                 Set<WordFrequencyScore> bestWords) {
        return new SolvleDTO("", wordList, fishingWords, bestWords, totalWords,
                Map.<Character, LongAdder>of(), List.<KnownPositionDTO>of());
    }

    private static WordCalculationConfig config(int partitionThreshold, int fishingThreshold) {
        return new WordCalculationConfig(0, 0, partitionThreshold, 0).withFishingThreshold(fishingThreshold);
    }

    @Test
    void returnsTopFishingWord_whenAboveBothPartitionAndFishingThresholds() {
        WordCalculationConfig config = config(5, 2);
        SolvleDTO analysis = dto(10,
                ordered(score(1, "crane")),
                ordered(score(2, "slate"), score(3, "pound")),
                ordered(score(4, "trace")));

        WordFrequencyScore next = RemainingSolver.getNextGuess(config, analysis, new ArrayList<>());

        Assertions.assertEquals("slate", next.word());
    }

    @Test
    void skipsFishingAndUsesPartition_whenTopFishingWordAlreadyGuessed() {
        WordCalculationConfig config = config(5, 2);
        SolvleDTO analysis = dto(10,
                ordered(score(1, "crane")),
                ordered(score(2, "slate")),
                ordered(score(3, "trace")));

        WordFrequencyScore next = RemainingSolver.getNextGuess(config, analysis, new ArrayList<>(List.of("slate")));

        Assertions.assertEquals("trace", next.word());
    }

    @Test
    void usesPartitionWord_whenAtOrBelowPartitionThresholdButAboveFishingThreshold() {
        WordCalculationConfig config = config(5, 2);
        SolvleDTO analysis = dto(4,
                ordered(score(1, "crane")),
                ordered(score(2, "slate")),
                ordered(score(3, "trace"), score(4, "brine")));

        WordFrequencyScore next = RemainingSolver.getNextGuess(config, analysis, new ArrayList<>());

        Assertions.assertEquals("trace", next.word());
    }

    @Test
    void avoidsPreviouslyGuessedPartitionWord() {
        WordCalculationConfig config = config(5, 2);
        SolvleDTO analysis = dto(4,
                ordered(score(1, "crane")),
                ordered(score(2, "slate")),
                ordered(score(3, "trace"), score(4, "brine")));

        WordFrequencyScore next = RemainingSolver.getNextGuess(config, analysis, new ArrayList<>(List.of("trace")));

        Assertions.assertEquals("brine", next.word());
    }

    @Test
    void fallsBackToViableWordList_whenAtOrBelowFishingThreshold() {
        WordCalculationConfig config = config(5, 2);
        SolvleDTO analysis = dto(2,
                ordered(score(1, "crane"), score(2, "trace")),
                ordered(score(3, "slate")),
                ordered(score(4, "brine")));

        WordFrequencyScore next = RemainingSolver.getNextGuess(config, analysis, new ArrayList<>());

        Assertions.assertEquals("crane", next.word());
    }

    @Test
    void avoidsPreviouslyGuessedViableWord() {
        WordCalculationConfig config = config(5, 2);
        SolvleDTO analysis = dto(2,
                ordered(score(1, "crane"), score(2, "trace")),
                ordered(score(3, "slate")),
                ordered(score(4, "brine")));

        WordFrequencyScore next = RemainingSolver.getNextGuess(config, analysis, new ArrayList<>(List.of("crane")));

        Assertions.assertEquals("trace", next.word());
    }

    @Test
    void usesViableWordList_whenNoPartitionWordsAvailable() {
        WordCalculationConfig config = config(5, 2);
        SolvleDTO analysis = dto(4,
                ordered(score(1, "crane"), score(2, "trace")),
                ordered(score(3, "slate")),
                ordered());

        WordFrequencyScore next = RemainingSolver.getNextGuess(config, analysis, new ArrayList<>());

        Assertions.assertEquals("crane", next.word());
    }

    @Test
    void returnsNull_whenNoWordsRemain() {
        WordCalculationConfig config = config(5, 2);
        SolvleDTO analysis = dto(0, ordered(), ordered(), ordered());

        Assertions.assertNull(RemainingSolver.getNextGuess(config, analysis, new ArrayList<>()));
    }
}
