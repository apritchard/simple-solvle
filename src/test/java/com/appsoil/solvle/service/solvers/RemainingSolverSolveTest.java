package com.appsoil.solvle.service.solvers;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.service.SolvleService;
import com.appsoil.solvle.service.WordCalculationConfig;
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

/**
 * Integration tests for the full {@link RemainingSolver} solve loop against a small in-memory
 * dictionary.
 *
 * <p>This is a separate top-level class from {@link RemainingSolverTest} (the pure
 * {@code getNextGuess} unit tests) so that the heavyweight {@link SpringBootTest} application
 * context and its in-memory dictionary {@link TestConfiguration} bean stay scoped here and never
 * affect the context-free unit tests. Both classes run normally by class name, with no special
 * Surefire include patterns required.
 */
@SpringBootTest(classes = {SolvleService.class, RemainingSolverSolveTest.SolveConfiguration.class})
@ActiveProfiles("test")
class RemainingSolverSolveTest {

    @TestConfiguration
    static class SolveConfiguration {
        @Bean
        Map<DictionaryType, Dictionary> allDictionaries() {
            Map<DictionaryType, Dictionary> dictionaries = new EnumMap<>(DictionaryType.class);
            dictionaries.put(DictionaryType.SIMPLE, dictionary("crane", "trace", "brine"));
            dictionaries.put(DictionaryType.BIG, dictionary("crane", "trace", "brine", "slate", "pound", "fjord"));
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
    void solvesWordAndEndsOnTheSolution() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);

        List<String> guesses = solvleService.solveWord(solver, new Word("crane"), "", DictionaryType.SIMPLE);

        Assertions.assertFalse(guesses.isEmpty());
        Assertions.assertEquals("crane", guesses.get(guesses.size() - 1));
    }

    @Test
    void returnsOnlyFirstWord_whenFirstWordIsTheSolution() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);

        List<String> guesses = solvleService.solveWord(solver, new Word("crane"), "crane", DictionaryType.SIMPLE);

        Assertions.assertEquals(List.of("crane"), guesses);
    }

    @Test
    void prependsValidFirstWordBeforeContinuingToSolve() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);

        List<String> guesses = solvleService.solveWord(solver, new Word("brine"), "slate", DictionaryType.SIMPLE);

        Assertions.assertEquals("slate", guesses.get(0));
        Assertions.assertEquals("brine", guesses.get(guesses.size() - 1));
    }

    @Test
    void rejectsUnknownSolutionAndInvalidFirstWord() {
        RemainingSolver solver = new RemainingSolver(solvleService, WordCalculationConfig.SIMPLE);

        Assertions.assertEquals(List.of("Word Not Found"),
                solvleService.solveWord(solver, new Word("zzzzz"), "", DictionaryType.SIMPLE));
        Assertions.assertEquals(List.of("First word not valid"),
                solvleService.solveWord(solver, new Word("crane"), "zzzzz", DictionaryType.SIMPLE));
    }
}
