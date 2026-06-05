package com.appsoil.solvle.data;

import com.appsoil.solvle.service.WordCalculationConfig;
import com.appsoil.solvle.service.WordCalculationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

/**
 * Edge-case coverage for restriction semantics. This exercises the {@link WordRestrictions}
 * parsing/generation logic together with the {@link WordCalculationService#isValidWord} matcher that
 * consumes those restrictions, with particular attention to duplicate-letter Wordle semantics and
 * combined position/frequency/exclusion cases.
 *
 * <p>Everything here is a context-free unit test, so the tests are kept as flat top-level methods
 * (no {@code @Nested} grouping) to avoid any need for special Surefire include patterns.
 */
class WordRestrictionsTest {

    private static final WordCalculationService matcher =
            new WordCalculationService(WordCalculationConfig.SIMPLE);

    // ---------------------------------------------------------------------
    // Parsing: how raw strings like "g5^2!2" decompose into available
    // letters, known positions, required letters, exclusions, and minimum
    // letter frequencies.
    // ---------------------------------------------------------------------

    @Test
    void parsing_availableLettersOnly_haveNoRequirementsOrPositions() {
        WordRestrictions restrictions = new WordRestrictions("crane");

        Assertions.assertEquals("crane", restrictions.word().word());
        Assertions.assertTrue(restrictions.requiredLetters().isEmpty(),
                "Plain available letters should not be marked required");
        Assertions.assertTrue(restrictions.letterPositions().isEmpty());
        Assertions.assertTrue(restrictions.positionExclusions().isEmpty());
        Assertions.assertTrue(restrictions.minimumLetterFrequencies().isEmpty());
    }

    @Test
    void parsing_knownPositionsUseOneBasedIndexesAndMarkLetterRequired() {
        WordRestrictions restrictions = new WordRestrictions("c1rane");

        Assertions.assertEquals(Map.of(1, 'c'), restrictions.letterPositions());
        Assertions.assertTrue(restrictions.requiredLetters().contains('c'),
                "A letter with a known position must also be required");
    }

    @Test
    void parsing_requiredUnknownLetterUsesExclamationWithoutAddingPositions() {
        WordRestrictions restrictions = new WordRestrictions("cran!e");

        Assertions.assertTrue(restrictions.requiredLetters().contains('n'));
        Assertions.assertTrue(restrictions.letterPositions().isEmpty());
        Assertions.assertTrue(restrictions.positionExclusions().isEmpty());
    }

    @Test
    void parsing_positionExclusionsAreCapturedAfterExclamation() {
        WordRestrictions restrictions = new WordRestrictions("cran!23e");

        Assertions.assertTrue(restrictions.requiredLetters().contains('n'));
        Assertions.assertTrue(restrictions.positionExclusions().get(2).contains('n'));
        Assertions.assertTrue(restrictions.positionExclusions().get(3).contains('n'));
    }

    @Test
    void parsing_minimumLetterFrequencyUsesCaret() {
        WordRestrictions restrictions = new WordRestrictions("crane^2");

        Assertions.assertEquals(2, restrictions.minimumLetterFrequencies().get('e'));
        Assertions.assertTrue(restrictions.requiredLetters().contains('e'));
    }

    @Test
    void parsing_combinedPositionFrequencyAndExclusion_g5caret2excl2() {
        WordRestrictions restrictions = new WordRestrictions("cranebg5^2!2");

        Assertions.assertEquals('g', restrictions.letterPositions().get(5));
        Assertions.assertEquals(2, restrictions.minimumLetterFrequencies().get('g'));
        Assertions.assertTrue(restrictions.positionExclusions().get(2).contains('g'));
        Assertions.assertTrue(restrictions.requiredLetters().contains('g'));
    }

    // ---------------------------------------------------------------------
    // generateRestrictions: deriving new knowledge from a guess vs solution,
    // including the duplicate-letter rule.
    // ---------------------------------------------------------------------

    @Test
    void generate_exactMatchAddsKnownPositionAndRequiredLetter() {
        WordRestrictions next = WordRestrictions.generateRestrictions(
                new Word("crane"), new Word("crane"), WordRestrictions.noRestrictions());

        Assertions.assertEquals('c', next.letterPositions().get(1));
        Assertions.assertEquals('e', next.letterPositions().get(5));
        Assertions.assertTrue(next.requiredLetters().containsAll(Set.of('c', 'r', 'a', 'n', 'e')));
    }

    @Test
    void generate_presentButMisplacedLetterIsRequiredAndExcludedFromGuessedPosition() {
        // guess "early" vs solution "crane": 'r' is present but guessed in position 3 (solution has it
        // in position 2), so it becomes required and excluded from position 3
        WordRestrictions next = WordRestrictions.generateRestrictions(
                new Word("crane"), new Word("early"), WordRestrictions.noRestrictions());

        Assertions.assertTrue(next.requiredLetters().contains('r'));
        Assertions.assertTrue(next.positionExclusions().get(3).contains('r'));
    }

    @Test
    void generate_absentLetterIsRemovedFromAvailableCharacters() {
        // guess "pound" vs solution "crane": p,o,u,d absent; n present
        WordRestrictions next = WordRestrictions.generateRestrictions(
                new Word("crane"), new Word("pound"), WordRestrictions.noRestrictions());

        Assertions.assertFalse(next.word().word().contains("p"));
        Assertions.assertFalse(next.word().word().contains("o"));
        Assertions.assertFalse(next.word().word().contains("u"));
        Assertions.assertFalse(next.word().word().contains("d"));
        Assertions.assertTrue(next.requiredLetters().contains('n'),
                "A present letter should remain required even when its neighbours are removed");
    }

    @Test
    void generate_grayDuplicate_doesNotRemoveLetterConfirmedElsewhere() {
        // guess "geese" vs solution "crane": solution has exactly one 'e' (position 5).
        // The guess has three 'e's; the "extra" e's must NOT strip 'e' from the available set.
        WordRestrictions next = WordRestrictions.generateRestrictions(
                new Word("crane"), new Word("geese"), WordRestrictions.noRestrictions());

        Assertions.assertTrue(next.word().word().contains("e"),
                "A duplicate gray 'e' must not remove the 'e' confirmed by the solution");
        Assertions.assertTrue(next.requiredLetters().contains('e'));
        Assertions.assertEquals('e', next.letterPositions().get(5));
        // minimum frequency reflects the solution count (1), not the guess count (3)
        Assertions.assertEquals(1, next.minimumLetterFrequencies().get('e'));
        // g,s are absent and must be removed
        Assertions.assertFalse(next.word().word().contains("g"));
        Assertions.assertFalse(next.word().word().contains("s"));
    }

    @Test
    void generate_minimumFrequencyForDuplicateInSolution_usesMinOfSolutionAndGuessCounts() {
        // solution "abbey" has two b's; guess "babes" has two b's -> min frequency 2
        WordRestrictions next = WordRestrictions.generateRestrictions(
                new Word("abbey"), new Word("babes"), WordRestrictions.noRestrictions());

        Assertions.assertEquals(2, next.minimumLetterFrequencies().get('b'));
    }

    @Test
    void generate_preservesExistingRestrictionsWhenGeneratingFromNewGuess() {
        // first guess "early" excludes 'r' from position 3
        WordRestrictions first = WordRestrictions.generateRestrictions(
                new Word("crane"), new Word("early"), WordRestrictions.noRestrictions());

        // second guess "acorn" excludes 'r' from position 4
        WordRestrictions second = WordRestrictions.generateRestrictions(
                new Word("crane"), new Word("acorn"), first);

        // exclusion learned from the first guess is preserved through the second
        Assertions.assertTrue(second.positionExclusions().get(3).contains('r'),
                "Position exclusions from earlier guesses must be preserved");
        // exclusion learned from the second guess is also present
        Assertions.assertTrue(second.positionExclusions().get(4).contains('r'),
                "Position exclusions from the new guess must be added");
    }

    // ---------------------------------------------------------------------
    // withAdditionalLetterPositions: merges new known positions and required
    // letters without losing existing exclusions or frequencies.
    // ---------------------------------------------------------------------

    @Test
    void withAdditionalLetterPositions_mergesRequiredLettersAndPositionsWithoutLosingExclusionsOrFrequencies() {
        // 'n!2' excludes 'n' from position 2; 'g^2' requires two g's
        WordRestrictions base = new WordRestrictions("cran!2eg^2");

        WordRestrictions merged = base.withAdditionalLetterPositions(Map.of(3, 'a'));

        Assertions.assertEquals('a', merged.letterPositions().get(3));
        Assertions.assertTrue(merged.requiredLetters().contains('a'),
                "New positioned letters must become required");
        // pre-existing exclusions and frequencies survive the merge
        Assertions.assertTrue(merged.positionExclusions().get(2).contains('n'));
        Assertions.assertEquals(2, merged.minimumLetterFrequencies().get('g'));
    }

    // ---------------------------------------------------------------------
    // isValidWord: the consumer of generated restrictions, focusing on
    // combined position/frequency/exclusion checks and duplicate handling.
    // ---------------------------------------------------------------------

    @Test
    void isValidWord_rejectsWordMissingRequiredLetter() {
        WordRestrictions restrictions = new WordRestrictions("cran!e");

        Assertions.assertTrue(matcher.isValidWord(new Word("crane"), restrictions));
        Assertions.assertFalse(matcher.isValidWord(new Word("crars"), restrictions),
                "Missing required 'n' should be rejected");
    }

    @Test
    void isValidWord_rejectsWordWithLetterInExcludedPosition() {
        // the trailing 'n!2' adds 'n' as required and excludes it from position 2
        WordRestrictions restrictions = new WordRestrictions("crane n!2");

        Assertions.assertFalse(matcher.isValidWord(new Word("cnare"), restrictions),
                "'n' in position 2 is excluded");
        Assertions.assertTrue(matcher.isValidWord(new Word("crane"), restrictions));
    }

    @Test
    void isValidWord_rejectsWordViolatingKnownPosition() {
        WordRestrictions restrictions = new WordRestrictions("c1rane");

        Assertions.assertTrue(matcher.isValidWord(new Word("crane"), restrictions));
        Assertions.assertFalse(matcher.isValidWord(new Word("trace"), restrictions),
                "Word without 'c' in position 1 should be rejected");
    }

    @Test
    void isValidWord_enforcesMinimumLetterFrequency() {
        // trailing 'b^2' requires at least two b's
        WordRestrictions restrictions = new WordRestrictions("aeyb^2");

        Assertions.assertTrue(matcher.isValidWord(new Word("abbey"), restrictions));
        Assertions.assertFalse(matcher.isValidWord(new Word("abeey"), restrictions),
                "A single 'b' fails the minimum frequency of two");
    }

    @Test
    void isValidWord_rejectsWordWithUnavailableLetter() {
        WordRestrictions restrictions = new WordRestrictions("crane");

        Assertions.assertFalse(matcher.isValidWord(new Word("crazy"), restrictions),
                "'z' and 'y' are not in the available letter set");
    }

    @Test
    void isValidWord_combinedPositionFrequencyAndExclusion_areAllEnforcedTogether() {
        // 'a' known in position 3, requires at least two a's overall, 'a' excluded from position 1
        WordRestrictions restrictions = new WordRestrictions("anrt a3^2!1");

        Assertions.assertTrue(matcher.isValidWord(new Word("naart"), restrictions),
                "naart: a in pos 3, two a's, no a in pos 1");
        Assertions.assertFalse(matcher.isValidWord(new Word("ttart"), restrictions),
                "only one 'a' fails the minimum frequency");
    }
}
