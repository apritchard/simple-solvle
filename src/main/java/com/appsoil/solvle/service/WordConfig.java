package com.appsoil.solvle.service;

public enum WordConfig {
    SIMPLE(WordCalculationConfig.SIMPLE_WITH_FISHING),
    SIMPLE_WITH_PARTITIONING(WordCalculationConfig.SIMPLE_WITH_PARTITIONING),
    OPTIMAL_MEAN(WordCalculationConfig.OPTIMAL_MEAN_WITHOUT_PARTITIONING),
    OPTIMAL_MEAN_WITH_PARTITIONING(WordCalculationConfig.OPTIMAL_MEAN),
    TWO_OR_LESS(WordCalculationConfig.TWO_OR_LESS);

    public WordCalculationConfig config;

    WordConfig(WordCalculationConfig config) {
        this.config = config;
    }

}
