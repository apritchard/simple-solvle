package com.appsoil.solvle.service;

import java.util.Map;

public record  WordCalculationConfig (
        double rightLocationMultiplier,     // multiplies a letter score if this letter is in the correct position. Best range: [3-6]
        double uniquenessMultiplier,        // multiplies a letter score if it is not duplicate in the current word Best range: [4-9]
        int partitionThreshold,             // number of viable words below which word partition calcs are performed. Impacts performance noticeably above 10, very significantly above 100.
        double viableWordPreference,        // flat bonus to words in the viable word set. High values increase StDev. Best range: [0.001 - 0.01].
        boolean useHarmonic,                // scales down impact of more matches to prioritize new letters > important letters. Increases mean, but decreases max.
        int fishingThreshold,               // used by solvers to determine when to switch to only viable word choices. Best value is usually 2 or 3.
        boolean hardMode,                   // disables use of words that do not match restriction string
        double locationAdjustmentScale,     // modifies the scaling of location mult based on number of known positions. 0 = no scaling, 1 = 100% reduction when all letters known
        double uniqueAdjustmentScale,       // modifies the scaling of unique mult based on number of remaining letters available. 0 = no scaling, 1 = 100% reduction when no letters available
        double viableWordAdjustmentScale,   // modifies the scaling of the viable preference bonus based on number of known positions. 0 = no scaling, 1 =
        double vowelMultiplier,             // multiplies the value of vowels by this amount. Seems to improve performance in the [0.6-0.9] range
        double rutBreakMultiplier,          // multiplies the value of characters that can resolve word ruts in advance
        int rutBreakThreshold               // minimum number of shared words in a given KnownPosition in order to consider a potential rut
){

    private static final boolean defaultUseHarmonic = false;
    private static final int defaultFishingThreshold = 2;
    private static final boolean defaultHardMode = false;
    private static final double defaultLocationAdjustmentScale = 0;
    private static final double defaultUniqueAdjustmentScale = 0;
    private static final double defaultViableWordAdjustmentScale = 0;
    private static final double defaultVowelMultiplier = 1;
    private static final double defaultRutBreakMultiplier = 0;
    private static final int defaultRutBreakThreshold = 0;


    public WordCalculationConfig(double rightLocationMultiplier, double uniquenessMultiplier, int partitionThreshold, double viableWordPreference) {
        this(rightLocationMultiplier, uniquenessMultiplier, partitionThreshold, viableWordPreference, defaultUseHarmonic, defaultFishingThreshold, defaultHardMode, defaultLocationAdjustmentScale, defaultUniqueAdjustmentScale, defaultViableWordAdjustmentScale, defaultVowelMultiplier, defaultRutBreakMultiplier, defaultRutBreakThreshold);
    }

    public WordCalculationConfig withFishingThreshold(int fishingThreshold) {
        return new WordCalculationConfig(rightLocationMultiplier, uniquenessMultiplier, partitionThreshold, viableWordPreference, useHarmonic, fishingThreshold, hardMode, locationAdjustmentScale, uniqueAdjustmentScale, viableWordAdjustmentScale, vowelMultiplier, rutBreakMultiplier, rutBreakThreshold);
    }

    public WordCalculationConfig withPartitionThreshold(int partitionThreshold) {
        return new WordCalculationConfig(rightLocationMultiplier, uniquenessMultiplier, partitionThreshold, viableWordPreference, useHarmonic, fishingThreshold, hardMode, locationAdjustmentScale, uniqueAdjustmentScale, viableWordAdjustmentScale, vowelMultiplier, rutBreakMultiplier, rutBreakThreshold);
    }

    public WordCalculationConfig withHardMode(boolean hardMode) {
        return new WordCalculationConfig(rightLocationMultiplier, uniquenessMultiplier, partitionThreshold, viableWordPreference, useHarmonic, fishingThreshold, hardMode, locationAdjustmentScale, uniqueAdjustmentScale, viableWordAdjustmentScale, vowelMultiplier, rutBreakMultiplier, rutBreakThreshold);
    }

    public WordCalculationConfig withFineTuning(double locationAdjustmentScale, double uniqueAdjustmentScale, double viableWordAdjustmentScale, double vowelMultiplier) {
        return new WordCalculationConfig(rightLocationMultiplier, uniquenessMultiplier, partitionThreshold, viableWordPreference, useHarmonic, fishingThreshold, hardMode, locationAdjustmentScale, uniqueAdjustmentScale, viableWordAdjustmentScale, vowelMultiplier, rutBreakMultiplier, rutBreakThreshold);
    }

    public WordCalculationConfig withRutBreak(double rutBreakMultiplier, int rutBreakThreshold) {
        return new WordCalculationConfig(rightLocationMultiplier, uniquenessMultiplier, partitionThreshold, viableWordPreference, useHarmonic, fishingThreshold, hardMode, locationAdjustmentScale, uniqueAdjustmentScale, viableWordAdjustmentScale, vowelMultiplier, rutBreakMultiplier, rutBreakThreshold);
    }

    /**
     * Config that guesses the best of the valid words based on letters based on number of solutions with that word's characters. Does not use letter
     * position bias, remaining word partitioning, or fishing words.
     * {2=128, 3=866, 4=947, 5=273, 6=72, 7=23, 8=6}
     * Mean: 3.7356371490
     * StDv: 0.94813877
     * Median: 4.0
     * @return
     */
    public static WordCalculationConfig SIMPLE = new WordCalculationConfig(0, 0, 0, 0).withFishingThreshold(9999);

    /**
     * Config that guesses the best words based on letters based on number of solutions with that word's characters. Selects from the fishing word list until
     * 2 words remain. Does not use letter position bias, remaining word partitioning.
     * {2=51, 3=740, 4=1243, 5=267, 6=14}
     * Mean: 3.763714902807775
     * StDv: 0.7012003696822036
     * Median: 4.0
     * @return
     */
    public static WordCalculationConfig SIMPLE_WITH_FISHING = new WordCalculationConfig(0, 0, 0, 0);

    /**
     * Config that guesses the best words based on letters based on number of solutions with that word's characters. Selects from the fishing word list until
     * 50 words remain and then switches to selecting the word that removes the most remaining options until 2 words. Does not use letter position bias.
     * {2=62, 3=848, 4=1288, 5=117}
     * Mean: 3.630669546436285
     * StDv: 0.6265442121982954
     * Median: 4.0
     * @return
     */
    public static WordCalculationConfig SIMPLE_WITH_PARTITIONING = new WordCalculationConfig(0, 0, 0, 0).withPartitionThreshold(50);

    /**
     * Returns a config that minimizes the mean score without failing
     * {1=1, 2=71, 3=1168, 4=1019, 5=56}
     *  mean: 3.4570194384449247
     *  std dev: 0.6005159961171692
     *  median: 3.0
     * @return
     */
    public static WordCalculationConfig OPTIMAL_MEAN =  new WordCalculationConfig(3, 8, 110, .007)
            .withFineTuning(1, 0, 0.0, 0.7);

    /**
     * Optimal mean but with partitioning disabled
     * {1=1, 2=56, 3=992, 4=1102, 5=155, 6=9}
     * Mean: 3.596544276457883, StDv:
     * 0.6701347128504703,
     * Median: 4.0
     */
    public static WordCalculationConfig OPTIMAL_MEAN_WITHOUT_PARTITIONING =  new WordCalculationConfig(3, 8, 0, .007)
            .withFineTuning(1, 0, 0.0, 0.7);

    /**
     * Returns a config guaranteed to solve any wordle in 5 or fewer guesses, at the cost of higher mean
     *  {1=1, 2=72, 3=1126, 4=1056, 5=60}
     *  mean: 3.476025917926566
     *  std dev: 0.6051562647557255
     *  median: 4.0
     * @return
     */
    public static WordCalculationConfig LOWEST_MAX = new WordCalculationConfig(5, 5, 100, 0.007)
            .withFineTuning(1, 0, 0, 0.7);

    /**
     * Maximize the number of scores 3 and below
     * {2=46, 3=1142, 4=1008, 5=112, 6=7}
     *  mean: 3.5213822894168474
     *  std dev: 0.6358939347320292
     *  median: 3.0
     * @return
     */
    public static WordCalculationConfig THREE_OR_LESS = new WordCalculationConfig(4, 8, 50, .001)
            .withFineTuning(.06, 0, 1, 0.9)
            .withFishingThreshold(3);

    /**
     * Maximize the number of scores 4 and below
     * {1=1, 2=71, 3=1158, 4=1029, 5=56}
     *  mean: 3.46133909287257
     *  std dev: 0.6008096844494963
     *  median: 3.0
     * @return
     */
    public static WordCalculationConfig FOUR_OR_LESS = new WordCalculationConfig(3, 9, 100, .007)
            .withFineTuning(0.6, 1, 0.0, 0.7);

    /**
     * Maximize the number of scores 2 at all cost
     * {1=1, 2=146, 3=996, 4=953, 5=177, 6=33, 7=7, 8=2}
     *  mean: 3.559827213822895
     *  std dev: 0.8169720669671939
     *  median: 4.0
     * @return
     */
    public static WordCalculationConfig TWO_OR_LESS =  new WordCalculationConfig(3, 4, 50, .4)
            .withFineTuning(0, 0, 0, 0.9);

}
