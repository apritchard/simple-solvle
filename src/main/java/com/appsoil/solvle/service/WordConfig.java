package com.appsoil.solvle.service;

public enum WordConfig {
    SIMPLE(WordCalculationConfig.SIMPLE),
    SIMPLE_WITH_FISHING(WordCalculationConfig.SIMPLE_WITH_FISHING),
    SIMPLE_WITH_PARTITIONING(WordCalculationConfig.SIMPLE_WITH_PARTITIONING),
    OPTIMAL_MEAN(WordCalculationConfig.OPTIMAL_MEAN),
    LOWEST_MAX(WordCalculationConfig.LOWEST_MAX),
    THREE_OR_LESS(WordCalculationConfig.THREE_OR_LESS),
    FOUR_OR_LESS(WordCalculationConfig.FOUR_OR_LESS),
    TWO_OR_LESS(WordCalculationConfig.TWO_OR_LESS);

    public WordCalculationConfig config;

    WordConfig(WordCalculationConfig config) {
        this.config = config;
    }

}
