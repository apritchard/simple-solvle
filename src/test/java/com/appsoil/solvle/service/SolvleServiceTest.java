package com.appsoil.solvle.service;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.controller.SolvleDTO;
import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.data.WordFrequencyScore;
import com.appsoil.solvle.data.WordRestrictions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest(classes = {SolvleService.class, SolvleServiceTest.SolvleTestConfiguration.class})
@ActiveProfiles("test")
public class SolvleServiceTest {


    @TestConfiguration
    public static class SolvleTestConfiguration {
        @Bean
         Map<DictionaryType, Dictionary> allDictionaries() {
            Set<Word> words = Stream.of("aaaaa", "aaaab", "aaabc", "aabcd", "abcde", "bcdea").map(Word::new).collect(Collectors.toSet());
            Dictionary dictionary = new Dictionary(Map.of(5, words));
            return Arrays.stream(DictionaryType.values())
                    .collect(Collectors.toMap(
                            type -> type,
                            type -> dictionary
                    ));
        }
    }

    @Autowired
    SolvleService solvleService;
    WordConfig config = WordConfig.OPTIMAL_MEAN;
    boolean hardMode = false;
    boolean requireAnswer = false;


    @ParameterizedTest
    @CsvSource(value = {
            "a | aaaaa",
            "ab | aaaaa,aaaab",
            "abc | aaaaa,aaaab,aaabc",
            "abcd | aaaaa,aaaab,aaabc,aabcd",
            "abcde | aaaaa,aaaab,aaabc,aabcd,abcde,bcdea"
    }, delimiter = '|')
    void getWordAnalysis_lettersAvailable_matchesWords(String restrictionString, String matches) {
        Set<String> expectedWords = Arrays.stream(matches.split(",")).collect(Collectors.toSet());
        SolvleDTO result = solvleService.getWordAnalysis(restrictionString, DictionaryType.SIMPLE, config, hardMode, requireAnswer);

        Assertions.assertEquals(expectedWords, result.wordList().stream().map(WordFrequencyScore::word).collect(Collectors.toSet()));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "a5bcde | aaaaa,bcdea",
            "a1bcde | aaaaa,aaaab,aaabc,aabcd,abcde",
            "a15bcde | aaaaa",
            "a14bcde | aaaaa,aaaab",
            "ab1cde | bcdea",
            "a1b2c3d4e5 | abcde",
            "a1b3c2de | none "
    }, delimiter = '|')
    void getWordAnalysis_requiredPosition_matchesWords(String restrictionString, String matches) {
        Set<String> expectedWords = Arrays.stream(matches.split(",")).filter(s -> !s.equals("none")).collect(Collectors.toSet());
        SolvleDTO result = solvleService.getWordAnalysis(restrictionString, DictionaryType.SIMPLE, config, hardMode, requireAnswer);

        Assertions.assertEquals(expectedWords, result.wordList().stream().map(WordFrequencyScore::word).collect(Collectors.toSet()));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "a!1bcde | bcdea",
            "a!3bcde | aabcd,abcde,bcdea",
            "a!45bcde | aaabc,aabcd,abcde",
            "a!4b!2cde | aaabc,aabcd,bcdea",
            "a!5b | aaaab",
            "a!15bcde | none"
    }, delimiter = '|')
    void getWordAnalysis_excludedPosition_matchesWords(String restrictionString, String matches) {
        Set<String> expectedWords = Arrays.stream(matches.split(",")).filter(s -> !s.equals("none")).collect(Collectors.toSet());
        SolvleDTO result = solvleService.getWordAnalysis(restrictionString, DictionaryType.SIMPLE, config, hardMode, requireAnswer);

        Assertions.assertEquals(expectedWords, result.wordList().stream().map(WordFrequencyScore::word).collect(Collectors.toSet()));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "a1!3bcde | aabcd,abcde",
            "a4!5bcde | aaaab",
            "a1!3b2cde | abcde",
            "a1!3b!2cde | aabcd",
            "a3!4bc | aaabc",
            "a1!3b3cde | aabcd",
            "a1!2b3cde | none"
    }, delimiter = '|')
    void getWordAnalysis_excludeAndRequired_matchesWords(String restrictionString, String matches) {
        Set<String> expectedWords = Arrays.stream(matches.split(",")).filter(s -> !s.equals("none")).collect(Collectors.toSet());
        SolvleDTO result = solvleService.getWordAnalysis(restrictionString, DictionaryType.SIMPLE, config, hardMode, requireAnswer);

        Assertions.assertEquals(expectedWords, result.wordList().stream().map(WordFrequencyScore::word).collect(Collectors.toSet()));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "abcde | fghij | abcdeklmnopqrstuvwxyz",
            "abcde | abcdf | abcdeghijklmnopqrstuvwxyz",
            "abcde | abcde | abcdefghijklmnopqrstuvwxyz",
    }, delimiter = '|')
    void generateRestrictionsFromGuess_lettersDontMatch_restrictsLetters(String solution, String guess, String output) {
        WordRestrictions restrictions = new WordRestrictions("abcdefghijklmnopqrstuvwxyz");

        Word s = new Word(solution);
        Word g = new Word(guess);

        WordRestrictions newRestrictions = WordRestrictions.generateRestrictions(s, g, restrictions);

        Assertions.assertEquals(output, newRestrictions.word().word());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "abcde | fghij | 0",
            "abcde | abcdf | 4",
            "abcde | abcde | 5",
    }, delimiter = '|')
    void generateRestrictionsFromGuess_matchLetters_requiredLetters(String solution, String guess, int numRequired) {
        WordRestrictions restrictions = new WordRestrictions("abcdefghijklmnopqrstuvwxyz");

        Word s = new Word(solution);
        Word g = new Word(guess);

        WordRestrictions newRestrictions = WordRestrictions.generateRestrictions(s, g, restrictions);

        Assertions.assertEquals(numRequired, newRestrictions.requiredLetters().size());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "abcde | fghij | 0",
            "abcde | abcdf | 4",
            "abcde | fabcd | 0",
            "abcde | abcde | 5",
            "abcde | abced | 3",
    }, delimiter = '|')
    void generateRestrictionsFromGuess_matchPositions_addKnownLetters(String solution, String guess, int knownPositions) {
        WordRestrictions restrictions = new WordRestrictions("abcdefghijklmnopqrstuvwxyz");

        Word s = new Word(solution);
        Word g = new Word(guess);

        WordRestrictions newRestrictions = WordRestrictions.generateRestrictions(s, g, restrictions);

        Assertions.assertEquals(knownPositions, newRestrictions.letterPositions().keySet().size());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "abcde | fghij | 0",
            "abcde | abcdf | 0",
            "abcde | fabcd | 4",
            "abcde | abcde | 0",
            "abcde | abced | 2",
    }, delimiter = '|')
    void generateRestrictionsFromGuess_excludePositions_addExcludedLetters(String solution, String guess, int excludedPositions) {
        WordRestrictions restrictions = new WordRestrictions("abcdefghijklmnopqrstuvwxyz");

        Word s = new Word(solution);
        Word g = new Word(guess);

        WordRestrictions newRestrictions = WordRestrictions.generateRestrictions(s, g, restrictions);

        Assertions.assertEquals(excludedPositions, newRestrictions.positionExclusions().keySet().size());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "abcde | abcde",
            "aaaab | abcde,aaaab",
            "bcdea | abcde,bcdea",
    }, delimiter = '|')
    void solveWord_wordIsTopChoice_solves(String solution, String expectedResultString) {
        List<String> results = solvleService.solveWord(new Word(solution), DictionaryType.SIMPLE);
        List<String> expectedResults = Arrays.stream(expectedResultString.split(",")).toList();

        Assertions.assertEquals(expectedResults, results);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "aaaaa | aaaaa | 5",  // Five A's in both - should find all five
            "aaaaa | aaaab | 4",  // Five A's in solution, four in guess - should find four
            "aaaab | aaaaa | 4",  // Four A's in solution, five in guess - should find four
            "aabcd | aaaaa | 2",  // Two A's in solution, five in guess - should find two
            "abcde | aaaaa | 1"   // One A in solution, five in guess - should find one
    }, delimiter = '|')
    void generateRestrictionsFromGuess_multipleLetters_correctMinimumFrequency(String solution, String guess, int expectedAFrequency) {
        WordRestrictions restrictions = new WordRestrictions("abcdefghijklmnopqrstuvwxyz");

        Word s = new Word(solution);
        Word g = new Word(guess);

        WordRestrictions newRestrictions = WordRestrictions.generateRestrictions(s, g, restrictions);

        Assertions.assertEquals(expectedAFrequency, newRestrictions.minimumLetterFrequencies().getOrDefault('a', 0));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "a^5bcde | aaaaa",                                  // Exactly 5 A's
            "a^4bcde | aaaaa,aaaab",                            // At least 4 A's
            "a^3bcde | aaaaa,aaaab,aaabc",                      // At least 3 A's
            "a^2bcde | aaaaa,aaaab,aaabc,aabcd",                // At least 2 A's
            "a^1bcde | aaaaa,aaaab,aaabc,aabcd,abcde,bcdea"     // At least 1 A
    }, delimiter = '|')
    void getWordAnalysis_letterFrequency_matchesWords(String restrictionString, String matches) {
        Set<String> expectedWords = Arrays.stream(matches.split(",")).filter(s -> !s.equals("none")).collect(Collectors.toSet());
        SolvleDTO result = solvleService.getWordAnalysis(restrictionString, DictionaryType.SIMPLE, config, hardMode, requireAnswer);

        Assertions.assertEquals(expectedWords, result.wordList().stream().map(WordFrequencyScore::word).collect(Collectors.toSet()));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1 | a,b,c,d,e",
            "2 | ab,ac,ad,ae,bc,bd,be,cd,ce,de",
            "3 | abc,abd,abe,acd,ace,ade,bcd,bce,bde,cde"
    }, delimiter = '|')
    void generateNWordLists(int n, String expectedString) {
        Set<Word> dictionaryWords = Arrays.stream("abcde".split("")).map(Word::new).collect(Collectors.toSet());
        Dictionary d = new Dictionary(Map.of(1, dictionaryWords));
        Set<Word> dictionary = d.wordsBySize().get(1);
        Set<Set<Word>> expected = Arrays.stream(expectedString.split(",")).map(s -> Arrays.stream(s.split("")).map(Word::new).collect(Collectors.toSet())).collect(Collectors.toSet());
        var result = solvleService.generateNWordLists(dictionary, dictionary, n);
        Assertions.assertEquals(expected, result);
    }


}
