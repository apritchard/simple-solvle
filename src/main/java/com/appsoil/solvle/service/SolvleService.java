package com.appsoil.solvle.service;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.controller.GameScoreDTO;
import com.appsoil.solvle.controller.KnownPositionDTO;
import com.appsoil.solvle.data.*;
import com.appsoil.solvle.controller.SolvleDTO;
import com.appsoil.solvle.controller.WordScoreDTO;
import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.service.solvers.RemainingSolver;
import com.appsoil.solvle.service.solvers.Solver;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
public class SolvleService {

    private final Map<DictionaryType, Dictionary> dictionaries;

    private final int MAX_RESULT_LIST_SIZE = 100;
    private final int FISHING_WORD_SIZE = 200;
    private final int SHARED_POSITION_LIMIT = 1000;
    private final int DEFAULT_LENGTH = 5;

    private Map<DictionaryType, Map<Word, PartitionStats>> firstPartitionData = new ConcurrentHashMap<>();

    public SolvleService(Map<DictionaryType, Dictionary> dictionaries) {
        this.dictionaries = dictionaries;
    }


    private void timestamp(String name, LocalDateTime start){
        log.info(name + " took " + Duration.between(start, LocalDateTime.now()));
    }


    @Cacheable("validWords")
    public SolvleDTO getWordAnalysis(String restrictionString, DictionaryType wordList, WordConfig wordConfig, boolean hardMode) {

        log.debug("Searching for words using {}", wordConfig);

        // parse the string to identify required letters and position exclusions
        WordRestrictions wordRestrictions = new WordRestrictions(restrictionString.toLowerCase());
        SolvleDTO result = getWordAnalysis(wordRestrictions, wordList, wordConfig, hardMode);

        log.info("Found {} matches for {}", result.totalWords(), restrictionString);

        return result;
    }

    public SolvleDTO getWordAnalysis(WordRestrictions wordRestrictions, DictionaryType wordList, WordConfig wordConfig, boolean hardMode) {
        Set<Word> wordSet = getPrimarySet(wordList);
        Set<Word> fishingSet = getFishingSet(wordList);

        return getWordAnalysis(wordRestrictions, wordSet, fishingSet, wordConfig, hardMode);

    }

    public SolvleDTO getWordAnalysis(WordRestrictions wordRestrictions, Set<Word> wordSet, Set<Word> fishingSet, WordConfig wordConfig, boolean hardMode) {
        return getWordAnalysis(wordRestrictions, wordSet, fishingSet, wordConfig.config.withHardMode(hardMode));
    }

    public SolvleDTO getWordAnalysis(WordRestrictions wordRestrictions, Set<Word> wordSet, Set<Word> fishingSet, WordCalculationConfig wordCalculationConfig) {

        WordCalculationService wordCalculationService = new WordCalculationService(wordCalculationConfig);

        // find all the valid words in our dictionary for this restriction string
        Set<Word> containedWords = wordCalculationService.findMatchingWords(wordSet, wordRestrictions);
        if(wordCalculationConfig.hardMode()) {
            //for hard mode, we also have to filter the fishing word list the same way
            fishingSet = wordCalculationService.findMatchingWords(fishingSet, wordRestrictions);
        }

        // check for common positions within contained words
        SharedPositions sharedPositions = null;
        Map<Character, DoubleAdder> sharedPositionBonus = new HashMap<>();
        if(wordCalculationConfig.rutBreakThreshold() > 1 && containedWords.size() < SHARED_POSITION_LIMIT) {
            sharedPositions = wordCalculationService.findSharedWordRestrictions(containedWords);

            if(wordCalculationConfig.rutBreakMultiplier() > 0) {
                //generate a per-character bonus score based on their frequency in the shared position sets
                sharedPositionBonus = wordCalculationService.generateSharedCharacterWeights(sharedPositions, wordRestrictions);
            }
        }

        // data needed for the DTO
        Set<WordFrequencyScore> wordFrequencyScores; // scores for possible solution words
        Set<WordFrequencyScore> fishingWordScores;   // scores for non-solution words
        Set<WordFrequencyScore> remainingWords;      // words that reduce the solution set the most
        Map<Character, LongAdder> characterCounts;   // number of words with each character

        // calculate how many words in the valid word set contain each character and
        //   then generate scores for words in the valid list and fishing list
        if(wordCalculationConfig.rightLocationMultiplier() == 0) {
            characterCounts = wordCalculationService.calculateCharacterCounts(containedWords);

            wordFrequencyScores = wordCalculationService
                    .calculateViableWords(containedWords, characterCounts, containedWords.size(), 0, MAX_RESULT_LIST_SIZE, sharedPositionBonus);
            fishingWordScores = containedWords.isEmpty() ? new HashSet<>() : wordCalculationService
                    .calculateFishingWords(fishingSet, characterCounts, containedWords.size(), FISHING_WORD_SIZE, wordRestrictions.requiredLetters(), sharedPositionBonus);
        } else {
            Map<Integer, Map<Character, LongAdder>> positionalCharCounts = wordCalculationService.calculateCharacterCountsByPosition(containedWords);
            wordFrequencyScores = wordCalculationService
                    .calculateViableWordsByPosition(containedWords, positionalCharCounts, containedWords, 0, MAX_RESULT_LIST_SIZE, wordRestrictions, sharedPositionBonus);
            fishingWordScores = containedWords.isEmpty() ? new HashSet<>() : wordCalculationService
                    .calculateFishingWordsByPosition(fishingSet, positionalCharCounts, containedWords, FISHING_WORD_SIZE, wordRestrictions, sharedPositionBonus);

            //merge positional char counts for use in the DTO
            characterCounts = mergeCharacterCounts(positionalCharCounts);
        }

        // generate words that optimally partition the viable set
        if(wordCalculationConfig.partitionThreshold() <= 0) {
            remainingWords = null;
        } else {
            remainingWords = wordCalculationService.calculateRemainingWords(wordRestrictions, containedWords, wordFrequencyScores, fishingWordScores);

            //if partitioning enabled, also calculate recommendations for ruts
            if(sharedPositions != null) {
                Map<KnownPosition, Set<WordFrequencyScore>> recommendations = sharedPositions.knownPositions().entrySet().stream()
                        .filter(es -> es.getValue().size() >= wordCalculationConfig.rutBreakThreshold())
                        .collect(Collectors.toMap(Map.Entry::getKey, es -> {
                            //for each known position, make a new set of restrictions and then find the best partition word for that set
                            WordRestrictions tempRestrictions = wordRestrictions.withAdditionalLetterPositions(es.getKey().pos());
                            return wordCalculationService.calculateRemainingWords(tempRestrictions, containedWords, wordFrequencyScores, fishingWordScores).stream().limit(5).collect(Collectors.toCollection(TreeSet::new));
                        }));
                sharedPositions = sharedPositions.withRecommendations(recommendations);
            }
        }

        List<KnownPositionDTO> knownPositions = sharedPositions == null ? new ArrayList<>() : sharedPositions.toKnownPositionDTOList(wordCalculationConfig.rutBreakThreshold());
        return new SolvleDTO("", wordFrequencyScores, fishingWordScores, remainingWords, containedWords.size(), characterCounts, knownPositions);
    }

    public GameScoreDTO rateGame(String solution, List<String> guesses, DictionaryType wordList, WordConfig config, boolean hardMode) {
        WordCalculationConfig wordCalculationConfig = config.config.withHardMode(hardMode);
        WordCalculationService wordCalculationService = new WordCalculationService(wordCalculationConfig);
        Set<Word> wordSet = getPrimarySet(wordList);

        GameScoreDTO gameScoreDTO = new GameScoreDTO();

        WordRestrictions restrictions = WordRestrictions.NO_RESTRICTIONS;
        Word solutionWord = new Word(solution);
        List<String> currentGuesses = new ArrayList<>();

        for(String guess : guesses) {
            log.info("Evaluating " + guess);
            //score player's guess
            WordScoreDTO playerScore = getScore(restrictions, guess, wordList, config, hardMode);

            //get Solvle's guess and score it as well
            SolvleDTO analysis = getWordAnalysis(restrictions, wordList, config, hardMode);
            WordFrequencyScore solvleGuess = RemainingSolver.getNextGuess(wordCalculationConfig, analysis, currentGuesses);
            WordScoreDTO solvleScore = getScore(restrictions, solvleGuess.word(), wordList, config, hardMode);


            restrictions = WordRestrictions.generateRestrictions(solutionWord, new Word(guess), restrictions);
            Set<Word> newWords = wordCalculationService.findMatchingWords(wordSet, restrictions);
            int actualRemaining = newWords.size();

            log.info("Adding scores: {}:{} {}:{} actualRemaining:{}", guess, playerScore, solvleGuess.word(), solvleScore, actualRemaining );
            gameScoreDTO.addRow(guess, playerScore, solvleGuess.word(), solvleScore, actualRemaining, analysis.fishingWords().stream().findFirst().get());
            currentGuesses.add(guess); //to make sure Solvle doesn't guess an already chosen word
        }

        return gameScoreDTO;
    }

    @Cacheable("wordScore")
    public WordScoreDTO getScore(String restrictionString, String wordToScore, DictionaryType wordList, WordConfig wordConfig, boolean hardMode) {
        WordRestrictions wordRestrictions = new WordRestrictions(restrictionString.toLowerCase());
        return getScore(wordRestrictions, wordToScore, wordList, wordConfig, hardMode);
    }

    public WordScoreDTO getScore(WordRestrictions wordRestrictions, String wordToScore, DictionaryType wordList, WordConfig wordConfig, boolean hardMode) {
        WordCalculationConfig wordCalculationConfig = wordConfig.config.withHardMode(hardMode);

        Word word = new Word(wordToScore, 0);
        Set<Word> wordSet = getPrimarySet(wordList);

        //get the counts
        WordCalculationService wordCalculationService = new WordCalculationService(wordCalculationConfig);
        Set<Word> containedWords = wordCalculationService.findMatchingWords(wordSet, wordRestrictions);

        double score;
        // generate a per-character bonus score based on their frequency in the shared position sets
        Map<Character, DoubleAdder> sharedPositionBonus = new HashMap<>();
        if(wordCalculationConfig.rutBreakThreshold() > 1 && wordCalculationConfig.rutBreakMultiplier() > 0 && containedWords.size() < SHARED_POSITION_LIMIT) {
            sharedPositionBonus = wordCalculationService.generateSharedCharacterWeights(wordCalculationService.findSharedWordRestrictions(containedWords), wordRestrictions);
        }

        if(wordCalculationConfig.rightLocationMultiplier() == 0) {
            var counts = wordCalculationService.removeRequiredLettersFromCounts(wordCalculationService.calculateCharacterCounts(containedWords), wordRestrictions.requiredLetters());
            score = wordCalculationService.calculateFreqScore(word,
                    counts,
                    containedWords.size(),
                    wordToScore.length() - wordRestrictions.letterPositions().keySet().size(), sharedPositionBonus);
        } else {
            var counts = wordCalculationService.removeRequiredLettersFromCountsByPosition(wordCalculationService.calculateCharacterCountsByPosition(containedWords), wordRestrictions);
            score = wordCalculationService.calculateFreqScoreByPosition(word,
                    counts,
                    containedWords,
                    wordToScore.length() - wordRestrictions.letterPositions().keySet().size(),
                    wordRestrictions, sharedPositionBonus
                    );
        }

        PartitionStats stats;
        if(wordRestrictions.equals(WordRestrictions.NO_RESTRICTIONS)) {
            log.info("No restrictions - using cached partition data");
            if (firstPartitionData.containsKey(wordList) && firstPartitionData.get(wordList).containsKey(word)) {
                stats = firstPartitionData.get(wordList).get(word);
            } else {
                stats = wordCalculationService.getPartitionStatsForWord(wordRestrictions, containedWords, word);
                if(!firstPartitionData.containsKey(wordList)) {
                    firstPartitionData.put(wordList, new ConcurrentHashMap<>());
                }
                firstPartitionData.get(wordList).put(word, stats);
            }
        } else {
            stats = wordCalculationService.getPartitionStatsForWord(wordRestrictions, containedWords, word);
        }

       return new WordScoreDTO(stats.wordsRemaining(), score, stats.entropy());
    }

    /**
     * With a given set of restrictions, get the average solve length for top recommended words using a given configuration
     * Takes a long time, use with caution.
     * @param restrictionString         Current known restrictions about the solution
     * @param wordList                  Name of the wordlist for this playout
     * @param guess                     Which number guess this is, used for identifying failure state (exceeding 6 guesses)
     * @return
     */
    public Set<PlayOut> playOutSolutions(String restrictionString, DictionaryType wordList, WordConfig wordConfig, boolean hardMode, int guess) {

        WordCalculationConfig wordCalculationConfig = wordConfig.config.withHardMode(hardMode);

        WordCalculationService wordCalculationService = new WordCalculationService(wordCalculationConfig);
        WordRestrictions wordRestrictions = new WordRestrictions(restrictionString);
        Solver solver = new RemainingSolver(this, wordCalculationConfig);

        Set<Word> wordSet = getPrimarySet(wordList);
        Set<Word> fishingSet = getFishingSet(wordList);

        // get the potential solutions
        Set<Word> containedWords = wordCalculationService.findMatchingWords(wordSet, wordRestrictions);

        // run Solvle's rating assessment and merge the top valid and fishing words
        SolvleDTO result = getWordAnalysis(wordRestrictions, wordSet, fishingSet, wordCalculationConfig);
        Set<Word> wordPool = wordCalculationService.mergeWordPools(result.wordList(), result.fishingWords());


        Set<PlayOut> averageSolveLengths = wordCalculationService.getWordsBySolveLength(
                containedWords, fishingSet, wordPool, solver, wordRestrictions, guess);
        averageSolveLengths.forEach(s -> {
            log.info(s);
        });
        return averageSolveLengths;
    }

    private Map<Character, LongAdder> mergeCharacterCounts(Map<Integer, Map<Character, LongAdder>> countsByPos) {
        // this returns slightly higher values than the original 'words with characters' map, because words with
        // duplicate chars are represented multiple times in the countsByPos map. The information about words
        // with duplicates is lost at this point and not worth recalculating.
        return countsByPos.values().stream().flatMap(map -> map.entrySet().stream()).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (v1, v2) -> {
            v1.add(v2.longValue());
            return v1;
        }));
    }

    private Set<Word> getPrimarySet(DictionaryType wordList) {
        return dictionaries.get(wordList).wordsBySize().get(DEFAULT_LENGTH);
    }

    private Set<Word> getFishingSet(DictionaryType wordList) {
        // use the big dictionary for fishing simple words, because answers are not required to be valid
        Dictionary dictionary = switch (wordList) {
            case ICELANDIC -> dictionaries.get(DictionaryType.ICELANDIC_FISHING);
            case SPANISH -> dictionaries.get(DictionaryType.SPANISH);
            case GERMAN_6MAL5 -> dictionaries.get(DictionaryType.GERMAN_6MAL5);
            case GERMAN_WORDLE_GLOBAL -> dictionaries.get(DictionaryType.GERMAN_WORDLE_GLOBAL);
            default -> dictionaries.get(DictionaryType.BIG);
        };
        return dictionary.wordsBySize().get(DEFAULT_LENGTH);
    }

    public SharedPositions findSharedWordRestrictions(DictionaryType wordList) {
        WordCalculationService wordCalculationService= new WordCalculationService(WordCalculationConfig.SIMPLE);
        return wordCalculationService.findSharedWordRestrictions(getPrimarySet(wordList));
    }

    /**
     * Solves for every word in the simple dictionary and returns a map of the guess-route the provided Solver will
     * choose for each word in the simple dictionary.
     * @param solver Solver implementation class that decide when to use words from fishing/partition/valid lists
     * @param firstWord Optional. Overrides the WordCalculationService's first word guess to see how results change.
     *                  Will otherwise use the first fishing word as calculated for the provided Config
     * @param wordCalculationConfig
     * @return
     */
    public Map<String, List<String>> solveDictionary(Solver solver, String firstWord, WordCalculationConfig wordCalculationConfig, DictionaryType wordList) {

        Set<Word> words = getPrimarySet(wordList);

        if(firstWord == null || firstWord.isBlank()) {
            SolvleDTO guess = getWordAnalysis(WordRestrictions.noRestrictions(), words, getFishingSet(wordList), wordCalculationConfig);
            if(guess.bestWords() != null && !guess.bestWords().isEmpty()) {
                firstWord = guess.bestWords().stream().findFirst().get().word();
            } else {
                firstWord = guess.fishingWords().stream().findFirst().get().word();
            }
        }

        final String startingWord = firstWord;

        Map<String, List<String>> outcome = new ConcurrentHashMap<>();
        words.stream().forEach( word -> {
            List<String> guesses = solveWord(solver, word, startingWord, wordList);
            outcome.put(word.word(), guesses);
        });

        return outcome;
    }

    public Map<String, List<String>> solveDictionary(Solver solver, List<String> previousGuesses, WordCalculationConfig wordCalculationConfig, String startingRestrictions, DictionaryType wordList) {
        Set<Word> words = getPrimarySet(DictionaryType.SIMPLE);
        SolvleDTO guess = getWordAnalysis(new WordRestrictions(startingRestrictions.toLowerCase()), words, getFishingSet(DictionaryType.SIMPLE), wordCalculationConfig);
        final String firstWord = guess.fishingWords().stream().findFirst().get().word();

        Map<String, List<String>> outcome = new ConcurrentHashMap<>();
        words.stream().forEach( word -> {
            List<String> guesses = new ArrayList<>(previousGuesses);
            guesses.addAll(solveWord(solver, word, firstWord, wordList));
            outcome.put(word.word(), guesses);
        });
        return outcome;
    }


    public List<String> solveWord(Word word, DictionaryType wordList) {
        return solveWord(new RemainingSolver(this, WordCalculationConfig.SIMPLE), word, "", wordList);
    }

    /**
     * Attempts to guess the correct solution to a word and returns a list of which words
     * would be used to find the solution.
     * @param word The solution
     * @return An ordered list of guesses, of which the final one is the solution
     */
    public List<String> solveWord(Solver solver, Word word, String firstWord, DictionaryType wordList) {

        //get initial restrictions
        Set<Word> wordSet = getPrimarySet(wordList);
        Set<Word> fishingSet = getFishingSet(wordList);

        if(!wordSet.contains(word)) {
            return List.of("Word Not Found");
        } else if ( !"".equals(firstWord) && !fishingSet.contains(new Word(firstWord))) {
            return List.of("First word not valid");
        }

        return solver.solve(word, wordSet, fishingSet, firstWord);
    }
}
