package com.appsoil.solvle.config;

public enum DictionaryType {
    BIG("/dict2/enable1.txt"),
    SIMPLE("/dict2/simple-solutions.txt"),
    EXTENDED("/dict2/extended-solutions.txt"),
    REDUCED("/dict2/remaining-solutions.txt"),
    ICELANDIC_FISHING("/dict2/iceland.txt"),
    ICELANDIC("/dict2/iceland-common.txt"),
    SPANISH("/dict2/spanish.txt"),
    GERMAN_6MAL5("/dict2/german-6mal5.txt"),
    GERMAN_WORDLE_GLOBAL("/dict2/german-wordle-global.txt");

    private final String path;

    DictionaryType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}


