package com.appsoil.solvle.data;

import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Log4j2
public record WordRestrictions(Word word,
                               Set<Character> requiredLetters,
                               Map<Integer, Character> letterPositions,
                               Map<Integer, Set<Character>> positionExclusions,
                               Map<Character, Integer> minimumLetterFrequencies,
                               Map<Character, Integer> maximumLetterFrequencies)  {

    //parses a string of letters. Letters may be followed by numbers, an exclamation mark, or both
    // for example abc3d!e!45fg5!2 will be parsed into (a)(b)(c3)(d!)(e!45)(f)(g5!2)
    // - e^2!45 -> e must appear at least twice, but not in positions 4 or 5
    // - g5^2!2 -> g must be in position 5, appear at least twice, and not in position 2
    // - e^1$1  -> e must appear at least once and at most once (exactly one e)
    // - o$0    -> o must appear at most zero times (an alternative to simply omitting it)
    // group order: (1) letter (2) known positions (3) ^min (4) $max (5) !excluded-positions
    private static Pattern restrictionsRegex = Pattern.compile("(\\S)(\\d*)(\\^\\d+)?(\\$\\d+)?(\\!\\d*)*");

    private static int MAX_WORD_LENGTH = 9;

    public static final WordRestrictions NO_RESTRICTIONS = new WordRestrictions("aáäbcdðeéfghiíjklmnñoópqrsßtuúüvwxyýzþæö");

    /**
     * Creates a description of known restriction knowledge based on provided input string.
     * @param word String containing all available letters from which to guess. If a letter is followed by
     *             numbers, those numbers indicate required positions for the letter. If a letter is followed
     *             by an exclamation (!), that letter is required, but we don't know where. If the ! is followed
     *             by numbers, those indicate positions we know are not available.
     *
     *             A letter followed by {@code ^N} must appear at least N times; a letter followed by
     *             {@code $N} must appear at most N times. The {@code $N} (maximum) token is what lets a
     *             restriction string capture duplicate-letter knowledge such as "exactly one e" that is
     *             learned when a guess plays a letter more times than the solution contains it.
     *
     *             For example: ac1t!2u3!4 - this string tells us:
     *                  a    - available
     *                  c1   - required in position 1
     *                  t!12 - required, but NOT in positions 1 or 2
     *                  u3!4 - required in 3, not allowed in 4
     */
    public WordRestrictions(String word) {
        this(new Word(word.replaceAll("[^\\p{L}]", ""), 0),
                new HashSet<>(MAX_WORD_LENGTH),
                new HashMap<>(MAX_WORD_LENGTH),
                new HashMap<>(MAX_WORD_LENGTH),
                new HashMap<>(MAX_WORD_LENGTH),
                new HashMap<>(MAX_WORD_LENGTH));

        log.debug("Parsing restriction characters " + word);

        Matcher matcher = restrictionsRegex.matcher(word);
        while(matcher.find()) {
            char c = matcher.group(1).charAt(0);
            boolean hasPos = matcher.group(2) != null && !matcher.group(2).isEmpty();
            boolean hasMinFreq = matcher.group(3) != null;   // ^N -> minimum count
            boolean hasMaxFreq = matcher.group(4) != null;   // $N -> maximum count
            boolean required = matcher.group(5) != null;     // !  -> required, with optional excluded positions

            // A known position, a minimum frequency, or a '!' all assert the letter is present, so the
            // letter is required. A maximum-frequency token alone does not assert presence (e.g. "$0"),
            // so it must not add the letter to requiredLetters.
            if(required || hasPos || hasMinFreq) {
                requiredLetters.add(c);
            }
            if(hasPos) {
                matcher.group(2).chars().mapToObj(i -> Character.getNumericValue(i)).forEach(pos ->  {
                    letterPositions.put(pos, c);
                });
            }
            if(hasMinFreq) {
                int freq = Integer.parseInt(matcher.group(3).substring(1));
                minimumLetterFrequencies.put(c, freq);
            }
            if(hasMaxFreq) {
                int freq = Integer.parseInt(matcher.group(4).substring(1));
                maximumLetterFrequencies.put(c, freq);
            }
            //if we have position numbers after the '!', add those to the position exclusions map
            if(required) {
                matcher.group(5).chars().mapToObj(i -> Character.getNumericValue(i)).skip(1).forEach(pos -> {
                    if(!positionExclusions.containsKey(pos)) {
                        positionExclusions.put(pos, new HashSet<>());
                    }
                    positionExclusions.get(pos).add(c);
                });
            }
            log.debug("Final minimumLetterFrequencies: {} maximumLetterFrequencies: {}", minimumLetterFrequencies, maximumLetterFrequencies);
        }
    }

    /**
     * Returns a copy of these restrictions that override the known letter positions with a new set of positions
     * and adds those positions to the required letters list. Used to produce new requirements for a word rut.
     * @param letterPositions
     * @return
     */
    public WordRestrictions withAdditionalLetterPositions(Map<Integer, Character> letterPositions) {
        Set<Character> newRequiredLetters = new HashSet<>(requiredLetters);
        newRequiredLetters.addAll(letterPositions.values());
        return new WordRestrictions(word, newRequiredLetters, letterPositions, positionExclusions, minimumLetterFrequencies, maximumLetterFrequencies);
    }

    public static WordRestrictions noRestrictions() {
        return NO_RESTRICTIONS;
    }

    public static WordRestrictions generateRestrictions(Word solution, Word guess, WordRestrictions currentRestrictions) {

        String restrictionWord = currentRestrictions.word().word();
        Set<Character> newRequiredLetters = new HashSet<>(currentRestrictions.requiredLetters());
        Map<Character, Integer> newMinimumLetterFrequencies = new HashMap<>(currentRestrictions.minimumLetterFrequencies());
        Map<Character, Integer> newMaximumLetterFrequencies = new HashMap<>(currentRestrictions.maximumLetterFrequencies());

        Map<Integer, Character> newLetterPositions = new HashMap<>(currentRestrictions.letterPositions());

        Map<Integer, Set<Character>> newPositionExclusions = new HashMap<>();
        currentRestrictions.positionExclusions().forEach((pos, cs) -> {
            Set<Character> newCs = new HashSet<>(currentRestrictions.positionExclusions.get(pos));
            newPositionExclusions.put(pos, newCs);
        });
        
        for(int i = 0; i < guess.getLength(); i++) {
            char c = guess.word().charAt(i);

            //if solution contains this letter, add it to required, otherwise remove it from the available chars
            if(solution.letters().containsKey(c)) {
                newRequiredLetters.add(c);
                newMinimumLetterFrequencies.put(c, Math.min(solution.letters().get(c), guess.letters().get(c)));
            } else {
                restrictionWord = restrictionWord.replace("" + c, "");
                continue;
            }
            // if the letter is in the correct spot, put it in solutions, otherwise, exclude this position
            if(c == solution.word().charAt(i)) {
                newLetterPositions.put(i + 1, c);
            } else {
                newPositionExclusions.putIfAbsent(i + 1, new HashSet<>());
                newPositionExclusions.get(i + 1).add(c);
            }
        }

        // Upper-bound (duplicate-letter capacity): when the guess uses a present letter more times
        // than the solution contains it, the surplus tiles come back gray, which proves an exact
        // maximum count for that letter. Record it, tightening any previously-known maximum. The
        // solutionCount == 0 case needs no entry here: that letter is already stripped from the
        // available pool above, which is an implicit max of zero.
        for (Map.Entry<Character, Integer> guessLetter : guess.letters().entrySet()) {
            char c = guessLetter.getKey();
            int solutionCount = solution.letters().getOrDefault(c, 0);
            if (solutionCount > 0 && guessLetter.getValue() > solutionCount) {
                newMaximumLetterFrequencies.merge(c, solutionCount, Math::min);
            }
        }

        return new WordRestrictions(new Word(restrictionWord), newRequiredLetters, newLetterPositions, newPositionExclusions, newMinimumLetterFrequencies, newMaximumLetterFrequencies);
    }
}
