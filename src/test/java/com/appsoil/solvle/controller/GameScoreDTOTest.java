package com.appsoil.solvle.controller;

import com.appsoil.solvle.data.WordFrequencyScore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GameScoreDTOTest {

    @Test
    void addRow_standardGuess_calculatesRowAndAggregateScores() {
        GameScoreDTO dto = new GameScoreDTO();
        WordScoreDTO playerScore = new WordScoreDTO(10.0, 2.0, 1.0);
        WordScoreDTO solvleScore = new WordScoreDTO(5.0, 3.0, 2.0);
        WordFrequencyScore bestFishing = new WordFrequencyScore(1, "crane", 4.0, null);

        dto.addRow("slate", playerScore, "crane", solvleScore, 4, 20, bestFishing);

        GameScoreDTO.GameScoreRow row = dto.getRows().get(0);
        double expectedLuck = 1 / (1 + Math.exp(-1.2));

        Assertions.assertEquals("slate", row.playerWord());
        Assertions.assertEquals("crane", row.solvleWord());
        Assertions.assertEquals(4, row.actualRemaining());
        Assertions.assertEquals(0.5, row.skill());
        Assertions.assertEquals(0.5, row.heuristic());
        Assertions.assertEquals(expectedLuck, row.luck(), 0.000000001);
        Assertions.assertEquals(0.5, dto.getSkill());
        Assertions.assertEquals(0.5, dto.getHeuristic());
        Assertions.assertEquals(expectedLuck, dto.getLuck(), 0.000000001);
    }

    @Test
    void addRow_noProgress_usesLowSkillAndNeutralLuck() {
        GameScoreDTO dto = new GameScoreDTO();
        WordScoreDTO playerScore = new WordScoreDTO(8.0, 1.5, 1.0);
        WordScoreDTO solvleScore = new WordScoreDTO(4.0, 2.5, 2.0);
        WordFrequencyScore bestFishing = new WordFrequencyScore(1, "crane", 3.0, null);

        dto.addRow("zzzzz", playerScore, "crane", solvleScore, 12, 12, bestFishing);

        GameScoreDTO.GameScoreRow row = dto.getRows().get(0);
        Assertions.assertEquals(0.01, row.skill());
        Assertions.assertEquals(0.01, row.heuristic());
        Assertions.assertEquals(0.5, row.luck());
        Assertions.assertEquals(0.01, dto.getSkill());
        Assertions.assertEquals(0.01, dto.getHeuristic());
        Assertions.assertEquals(0.5, dto.getLuck());
    }

    @Test
    void addRow_solvedGuess_recordsPerfectRowWithoutAddingAggregateStats() {
        GameScoreDTO dto = new GameScoreDTO();
        WordScoreDTO playerScore = new WordScoreDTO(0.0, 0.0, 0.0);
        WordScoreDTO solvleScore = new WordScoreDTO(1.0, 1.0, 1.0);
        WordFrequencyScore bestFishing = new WordFrequencyScore(1, "crane", 0.0, null);

        dto.addRow("slate", playerScore, "crane", solvleScore, 1, 2, bestFishing);

        GameScoreDTO.GameScoreRow row = dto.getRows().get(0);
        Assertions.assertEquals(1.0, row.skill());
        Assertions.assertEquals(1.0, row.heuristic());
        Assertions.assertEquals(0.0, row.luck());
        Assertions.assertTrue(Double.isNaN(dto.getSkill()));
        Assertions.assertTrue(Double.isNaN(dto.getHeuristic()));
        Assertions.assertTrue(Double.isNaN(dto.getLuck()));
    }
}
