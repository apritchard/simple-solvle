package com.appsoil.solvle.controller;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.data.PlayOut;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.service.SolvleService;
import com.appsoil.solvle.service.WordConfig;
import com.appsoil.solvle.service.solvers.RemainingSolver;
import com.appsoil.solvle.service.solvers.Solver;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MINUTES;

@RestController
@RequestMapping("/solvle")
@Log4j2
public class SolvleController {

    private final SolvleService solvleService;

    private static long requestsSinceLoading;
    private static final LocalDateTime startTime = LocalDateTime.now();
    private static LocalDateTime lastRequestLogTime = LocalDateTime.now();

    public SolvleController(SolvleService solvleService) {
        this.solvleService = solvleService;
    }

    @GetMapping("/{wordRestrictions}")
    public SolvleDTO getWordAnalysis(@PathVariable String wordRestrictions,
                                               @RequestParam(defaultValue = "SIMPLE") DictionaryType wordList,
                                               @RequestParam(defaultValue = "SIMPLE") WordConfig wordConfig,
                                               @RequestParam(defaultValue = "false") boolean hardMode,
                                               @RequestParam(defaultValue = "false") boolean requireAnswer
                                   ) {

        LocalDateTime start = LocalDateTime.now();
        logRequestsCount(start);
        log.info("Valid words requested with configuration {} wordList {} and hardMode={}", wordConfig, wordList, hardMode);
        SolvleDTO result = solvleService.getWordAnalysis(wordRestrictions.toLowerCase(), wordList, wordConfig, hardMode, requireAnswer);
        log.info("Valid words for {} took {}", wordRestrictions, Duration.between(start, LocalDateTime.now()));
        return SolvleDTO.appendRestrictionString(wordRestrictions, result);
    }

    @GetMapping("/{wordRestrictions}/{wordToScore}")
    public WordScoreDTO getWordScore(@PathVariable String wordRestrictions,
                                     @PathVariable String wordToScore,
                                     @RequestParam(defaultValue = "SIMPLE") DictionaryType wordList,
                                     @RequestParam(defaultValue = "SIMPLE") WordConfig wordConfig,
                                     @RequestParam(defaultValue = "false") boolean hardMode,
                                     @RequestParam(defaultValue = "false") boolean requireAnswer
                                     ) {
        LocalDateTime start = LocalDateTime.now();
        logRequestsCount(start);
        log.info("Word Score requested for {} with configuration {}", wordToScore, wordConfig);
        WordScoreDTO result = solvleService.getScore(wordRestrictions.toLowerCase(), wordToScore.toLowerCase(), wordList, wordConfig, hardMode, requireAnswer);
        log.info("Word Score for {} took {}", wordToScore, Duration.between(start, LocalDateTime.now()));

        return result;
    }

    @GetMapping("/{wordRestrictions}/playout")
    public Set<PlayOut> playOutSolution(@PathVariable String wordRestrictions,
                                        @RequestParam(defaultValue = "SIMPLE") DictionaryType wordList,
                                        @RequestParam(defaultValue = "SIMPLE") WordConfig wordConfig,
                                        @RequestParam(defaultValue = "false") boolean hardMode,
                                        @RequestParam(defaultValue = "0") int guess
    ) {


        logRequestsCount();
        log.info("Playout requested with configuration {}", wordConfig);
        Set<PlayOut> result = solvleService.playOutSolutions(wordRestrictions.toLowerCase(), wordList, wordConfig, hardMode, guess);
        return result;
    }

    @GetMapping("/solve/{solution}")
    public List<String> solvePuzzle(@PathVariable String solution,
                                    @RequestParam(defaultValue = "") String firstWord,
                                    @RequestParam(defaultValue = "SIMPLE") DictionaryType wordList,
                                    @RequestParam(defaultValue = "SIMPLE") WordConfig wordConfig,
                                    @RequestParam(defaultValue = "false") boolean hardMode,
                                    @RequestParam(defaultValue = "false") boolean requireAnswer
                                    ) {
        logRequestsCount();
        log.info("Solution requested for [{}] with first word [{}] and configuration {}", solution, firstWord, wordConfig);
        Solver solver = new RemainingSolver(solvleService, wordConfig.config.withHardMode(hardMode).withRequireAnswer(requireAnswer));
        return solvleService.solveWord(solver, new Word(solution.toLowerCase()), firstWord.toLowerCase(), wordList);
    }

    @GetMapping("/rate/{solution}")
    public GameScoreDTO solvePuzzle(@PathVariable String solution,
                                    @RequestParam(defaultValue = "") List<String> guesses,
                                    @RequestParam(defaultValue = "SIMPLE") DictionaryType wordList,
                                    @RequestParam(defaultValue = "SIMPLE") WordConfig wordConfig,
                                    @RequestParam(defaultValue = "false") boolean hardMode,
                                    @RequestParam(defaultValue = "false") boolean requireAnswer
                                    ) {
        logRequestsCount();
        log.info("Solution requested for [{}] with guesses {} and configuration {}", solution, guesses, wordConfig);
        Solver solver = new RemainingSolver(solvleService, wordConfig.config.withHardMode(hardMode).withRequireAnswer(requireAnswer));
        List<String> lowerGuesses = guesses.stream().map(String::toLowerCase).collect(Collectors.toList());
        return solvleService.rateGame(solution.toLowerCase(), lowerGuesses, wordList, wordConfig, hardMode, requireAnswer);
    }

    private void logRequestsCount() {
        logRequestsCount(LocalDateTime.now());
    }

    private static synchronized void logRequestsCount(LocalDateTime now) {
        if (requestsSinceLoading++ % 1000 == 0 || MINUTES.between(lastRequestLogTime, now) > 29) {
            double requestPerHour = (double) requestsSinceLoading * 60 / (double) MINUTES.between(startTime, now);
            log.info("{} requests made since {} ({} per hour)", requestsSinceLoading, startTime, requestPerHour);
            lastRequestLogTime = now;
        }
    }
}
