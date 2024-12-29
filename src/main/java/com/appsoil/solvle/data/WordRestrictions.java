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
                               Map<Character, Integer> minimumLetterFrequencies)  {

    //parses a string of letters. Letters may be followed by numbers, an exclamation mark, or both
    // for example abc3d!e!45fg5!2 will be parsed into (a)(b)(c3)(d!)(e!45)(f)(g5!2)
    // - e^2!45 -> e must appear at least twice, but not in positions 4 or 5
    // - g5^2!2 -> g must be in position 5, appear at least twice, and not in position 2
    private static Pattern restrictionsRegex = Pattern.compile("(\\S)(\\d*)(\\^\\d+)?(\\!\\d*)*");

    private static int MAX_WORD_LENGTH = 9;

    public static final WordRestrictions NO_RESTRICTIONS = new WordRestrictions("aáäbcdðeéfghiíjklmnñoópqrsßtuúüvwxyýzþæö");

    /**
     * Creates a description of known restriction knowledge based on provided input string.
     * @param word String containing all available letters from which to guess. If a letter is followed by
     *             numbers, those numbers indicate required positions for the letter. If a letter is followed
     *             by an exclamation (!), that letter is required, but we don't know where. If the ! is followed
     *             by numbers, those indicate positions we know are not available.
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
                new HashMap<>(MAX_WORD_LENGTH));

        log.debug("Parsing restriction characters " + word);

        Matcher matcher = restrictionsRegex.matcher(word);
        while(matcher.find()) {
            char c = matcher.group(1).charAt(0);
            boolean hasPos = matcher.group(2) != null && matcher.group(2) != "";
            boolean hasFreq = matcher.group(3) != null && matcher.group(3) != "";
            boolean required = matcher.group(4) != null && matcher.group(4) != "";

            if(required || hasPos || hasFreq) {
                requiredLetters.add(c);
            }
            if(hasPos) {
                matcher.group(2).chars().mapToObj(i -> Character.getNumericValue(i)).forEach(pos ->  {
                    letterPositions.put(pos, c);
                });
            }
            if(hasFreq) {
                int freq = Integer.parseInt(matcher.group(3).substring(1));
                minimumLetterFrequencies.put(c, freq);
            }
            //if we have position numbers after the '!', add those to the position exclusions map
            if(required) {
                matcher.group(4).chars().mapToObj(i -> Character.getNumericValue(i)).skip(1).forEach(pos -> {
                    if(!positionExclusions.containsKey(pos)) {
                        positionExclusions.put(pos, new HashSet<>());
                    }
                    positionExclusions.get(pos).add(c);
                });
            }
            log.debug("Final minimumLetterFrequencies: {}", minimumLetterFrequencies);
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
        return new WordRestrictions(word, newRequiredLetters, letterPositions, positionExclusions, minimumLetterFrequencies);
    }

    public static WordRestrictions noRestrictions() {
        return NO_RESTRICTIONS;
    }

    public static WordRestrictions generateRestrictions(Word solution, Word guess, WordRestrictions currentRestrictions) {

        String restrictionWord = currentRestrictions.word().word();
        Set<Character> newRequiredLetters = new HashSet<>(currentRestrictions.requiredLetters());
        Map<Character, Integer> newMinimumLetterFrequencies = new HashMap<>(currentRestrictions.minimumLetterFrequencies());

        Map<Integer, Character> newLetterPositions = new HashMap<>();
        currentRestrictions.letterPositions().forEach((pos, c) -> newLetterPositions.put(pos, c));

        Map<Integer, Set<Character>> newPositionExclusions = new HashMap<>();
        currentRestrictions.positionExclusions().forEach((pos, cs) -> {
            Set<Character> newCs = new HashSet<>();
            currentRestrictions.positionExclusions.get(pos).forEach(c -> newCs.add(c));
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

        return new WordRestrictions(new Word(restrictionWord), newRequiredLetters, newLetterPositions, newPositionExclusions, newMinimumLetterFrequencies);
    }
}
