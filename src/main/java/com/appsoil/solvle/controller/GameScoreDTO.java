package com.appsoil.solvle.controller;

import com.appsoil.solvle.data.WordFrequencyScore;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

public class GameScoreDTO {

    public List<GameScoreRow> getRows() {return rows;}
    public double getSkill() {
        return skillStats.getMean();
    }
    public double getLuck() {
        return luckStats.getMean();
    }
    public double getHeuristic() {
        return heuristicStats.getMean();
    }

    private List<GameScoreRow> rows = new ArrayList<>();

    private DescriptiveStatistics skillStats = new DescriptiveStatistics();
    private DescriptiveStatistics luckStats = new DescriptiveStatistics();
    private DescriptiveStatistics heuristicStats = new DescriptiveStatistics();

    public void addRow(String playerWord, WordScoreDTO playerScore, String solvleWord, WordScoreDTO solvleScore, int actualRemaining, int previousRemaining, WordFrequencyScore bestFishing) {
        double skill, luck, heuristic;
        if (playerScore.remainingWords() <= 0) {
            skill = 1;
            heuristic = 1;
            luck = 0;
        } else if (actualRemaining == previousRemaining) {
            skill = 0.01;
            heuristic = 0.01;
            luck = .5;
            skillStats.addValue(skill);
            luckStats.addValue(luck);
            heuristicStats.addValue(heuristic);
        } else {
            skill = solvleScore.remainingWords() / playerScore.remainingWords();
            heuristic = bestFishing.freqScore() > 0 ? playerScore.fishingScore() /  bestFishing.freqScore() : 1.0;
            luck = calculateLuck(playerScore.remainingWords(), actualRemaining);
            if(bestFishing.freqScore() > 0) {
                skillStats.addValue(skill);
                luckStats.addValue(luck);
                heuristicStats.addValue(heuristic);
            }
        }
        rows.add(new GameScoreRow(playerWord, playerScore, solvleWord, solvleScore, actualRemaining, skill, luck, heuristic));

    }

    private static double calculateLuck(double expectedRemaining, double actualRemaining) {

        double discrepancy = expectedRemaining - actualRemaining;
        double scaledDiscrepancy = discrepancy / expectedRemaining;
        double luck = 1 / (1 + Math.exp(-scaledDiscrepancy * 2));

        return luck;
    }

    public record GameScoreRow(String playerWord, WordScoreDTO playerScore, String solvleWord, WordScoreDTO solvleScore,
                               int actualRemaining, double skill, double luck, double heuristic) {
    }
}
