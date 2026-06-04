package com.appsoil.solvle.service;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.controller.GameScoreDTO;
import com.appsoil.solvle.controller.SolvleDTO;
import com.appsoil.solvle.controller.WordScoreDTO;
import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.TupleScore;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.data.WordFrequencyScore;
import com.appsoil.solvle.data.WordRestrictions;
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
            dictionaries.put(DictionaryType.EXTENDED, dictionary("crane", "trace", "brine"));
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
