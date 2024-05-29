package com.appsoil.solvle.service.solvers;

import com.appsoil.solvle.controller.SolvleDTO;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.data.WordFrequencyScore;
import com.appsoil.solvle.data.WordRestrictions;
import com.appsoil.solvle.service.SolvleService;
import com.appsoil.solvle.service.WordCalculationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Guesses the top fishing word until few enough words remain that we can
 * start using the best partition word. Use this until fishing threshold is
 * hit and then switch to top valid word.
 */
public class RemainingSolver implements Solver {

    private SolvleService solvleService;

    WordCalculationConfig config;

    public RemainingSolver(SolvleService solvleService,WordCalculationConfig config) {
        this.solvleService = solvleService;
        this.config = config;
    }

    @Override
    public List<String> solve(Word word, Set<Word> viable, Set<Word> fishing, String firstWord) {
        return solve(word, viable, fishing, new Word(firstWord), WordRestrictions.noRestrictions());
    }

    @Override
    public List<String> solve(Word word, Set<Word> viable, Set<Word> fishing, Word firstWord, WordRestrictions wordRestrictions) {

        if(word.equals(firstWord)) {
            return List.of(firstWord.word());
        }
        if(firstWord != null && firstWord.word().length() == word.word().length()) {
            List<String> solution = new ArrayList<>();
            solution.add(firstWord.word());
            wordRestrictions = WordRestrictions.generateRestrictions(word, firstWord, wordRestrictions);
            solution.addAll(solve(word, viable, fishing, wordRestrictions));
            return solution;
        } else {
            return solve(word, viable, fishing, wordRestrictions);
        }

    }

    public static WordFrequencyScore getNextGuess(WordCalculationConfig config, SolvleDTO analysis, List<String> solution) {

        //if we're above the partition threshold, fish as long as there are still enough words to warrant fishing
        if(analysis.totalWords() > config.partitionThreshold() && analysis.totalWords() > config.fishingThreshold() &&
                !solution.contains(analysis.fishingWords().stream().findFirst().get().word())) {
            return analysis.fishingWords().stream().findFirst().get();
        }
        // partition until we are below the fishing threshold as long as there are words in the partition set
        if(analysis.totalWords() > config.fishingThreshold() && analysis.bestWords()!= null && !analysis.bestWords().isEmpty()) {
            return analysis.bestWords().stream().findFirst().get();
        }
        if(analysis.totalWords() > 0) {
            return analysis.wordList().stream().findFirst().get();
        }
        return null;
    }

    @Override
    public List<String> solve(Word word, Set<Word> viable, Set<Word> fishing, WordRestrictions wordRestrictions) {
        List<String> solution = new ArrayList<>();


        //get the first guess
        SolvleDTO analysis;
        WordFrequencyScore currentGuess;

        do {
            analysis = solvleService.getWordAnalysis(wordRestrictions, viable, fishing, config);
            currentGuess = getNextGuess(config, analysis, solution);
            if(solution.contains(currentGuess.word())) {
                throw new IllegalStateException("Stuck in a loop guessing " + currentGuess.word() + " for " + word + " after " +  solution);
            }
            solution.add(currentGuess.word());

            wordRestrictions = WordRestrictions.generateRestrictions(word, new Word(currentGuess.word(), currentGuess.naturalOrdering()), wordRestrictions);
        }while(!currentGuess.word().equals(word.word()));

        return solution;
    }
}
