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

    /**
     * Adds one completed guess to the game rating.
     *
     * skill compares the player's expected remaining words to Solvle's recommendation.
     * heuristic compares the player's information-gain score to the best fishing word.
     * luck compares the expected remaining words to the actual remaining words after the guess.
     *
     * Terminal rows and rows without a fishing baseline are still returned in the row list,
     * but are skipped from aggregate averages so the summary reflects comparable decision points.
     */
    public void addRow(String playerWord, WordScoreDTO playerScore, String solvleWord, WordScoreDTO solvleScore, int actualRemaining, int previousRemaining, WordFrequencyScore bestFishing) {
        double skill, luck, heuristic;
        if (playerScore.remainingWords() <= 0) {
            // Avoid divide-by-zero on terminal/degenerate scores; the row is perfect but not aggregateable.
            skill = 1;
            heuristic = 1;
            luck = 0;
        } else if (actualRemaining == previousRemaining) {
            // No actual progress: keep the row visible, but floor skill/heuristic and use neutral luck.
            skill = 0.01;
            heuristic = 0.01;
            luck = .5;
            skillStats.addValue(skill);
            luckStats.addValue(luck);
            heuristicStats.addValue(heuristic);
        } else {
            // Normal scoring path: compare player choice against Solvle and against the best fishing option.
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
