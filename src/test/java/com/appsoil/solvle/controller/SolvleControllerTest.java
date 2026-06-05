package com.appsoil.solvle.controller;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.data.PartitionStats;
import com.appsoil.solvle.data.PlayOut;
import com.appsoil.solvle.data.TupleScore;
import com.appsoil.solvle.data.Word;
import com.appsoil.solvle.data.WordFrequencyScore;
import com.appsoil.solvle.service.SolvleService;
import com.appsoil.solvle.service.WordConfig;
import com.appsoil.solvle.service.job.SolveJob;
import com.appsoil.solvle.service.solvers.Solver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolvleController.class)
class SolvleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SolvleService solvleService;

    @Test
    void getWordAnalysis_lowercasesRestrictionsPassesQueryParamsAndEchoesOriginalRestriction() throws Exception {
        when(solvleService.getWordAnalysis("abc", DictionaryType.EXTENDED, WordConfig.TWO_OR_LESS, true, true))
                .thenReturn(analysisDto());

        mockMvc.perform(get("/solvle/ABC")
                        .param("wordList", "EXTENDED")
                        .param("wordConfig", "TWO_OR_LESS")
                        .param("hardMode", "true")
                        .param("requireAnswer", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restrictionString").value("ABC"))
                .andExpect(jsonPath("$.totalWords").value(1))
                .andExpect(jsonPath("$.wordList[0].word").value("crane"))
                .andExpect(jsonPath("$.fishingWords[0].word").value("slate"))
                .andExpect(jsonPath("$.bestWords[0].word").value("raise"));

        verify(solvleService).getWordAnalysis("abc", DictionaryType.EXTENDED, WordConfig.TWO_OR_LESS, true, true);
    }

    @Test
    void getWordAnalysis_usesSharedDefaultQueryParams() throws Exception {
        when(solvleService.getWordAnalysis("abc", DictionaryType.SIMPLE, WordConfig.SIMPLE, false, false))
                .thenReturn(analysisDto());

        mockMvc.perform(get("/solvle/abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restrictionString").value("abc"));

        verify(solvleService).getWordAnalysis("abc", DictionaryType.SIMPLE, WordConfig.SIMPLE, false, false);
    }

    @Test
    void getWordScore_lowercasesPathVariablesAndUsesDefaults() throws Exception {
        when(solvleService.getScore("abc", "crane", DictionaryType.SIMPLE, WordConfig.SIMPLE, false, false))
                .thenReturn(new WordScoreDTO(4.5, 2.25, 1.75));

        mockMvc.perform(get("/solvle/score/ABC/CRANE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingWords").value(4.5))
                .andExpect(jsonPath("$.fishingScore").value(2.25))
                .andExpect(jsonPath("$.entropy").value(1.75));

        verify(solvleService).getScore("abc", "crane", DictionaryType.SIMPLE, WordConfig.SIMPLE, false, false);
    }

    @Test
    void getScoreTuple_parsesCommaSeparatedTupleAndPassesWordList() throws Exception {
        Set<Word> expectedTuple = Set.of(new Word("arise"), new Word("pound"));
        when(solvleService.scoreTuple(expectedTuple, DictionaryType.EXTENDED)).thenReturn(tupleScore(expectedTuple));

        mockMvc.perform(get("/solvle/scoreTuple/ARISE,POUND")
                        .param("wordList", "EXTENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partitionStats.wordsRemaining").value(3.5))
                .andExpect(jsonPath("$.partitionStats.entropy").value(2.25));

        verify(solvleService).scoreTuple(expectedTuple, DictionaryType.EXTENDED);
    }

    @Test
    void getBestNWords_passesTupleSizeWordListConfigAndRequireAnswer() throws Exception {
        Set<TupleScore> response = Set.of(tupleScore(Set.of(new Word("arise"), new Word("pound"))));
        when(solvleService.findBestNWords(2, DictionaryType.SPANISH, WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING, true))
                .thenReturn(response);

        mockMvc.perform(get("/solvle/abc/best/2")
                        .param("wordList", "SPANISH")
                        .param("wordConfig", "OPTIMAL_MEAN_EXTENDED_PARTITIONING")
                        .param("hardMode", "true")
                        .param("requireAnswer", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partitionStats.entropy").value(2.25));

        verify(solvleService).findBestNWords(2, DictionaryType.SPANISH, WordConfig.OPTIMAL_MEAN_EXTENDED_PARTITIONING, true);
    }

    @Test
    void submitTupleJob_parsesTupleAndReturnsJobStatus() throws Exception {
        Set<Word> expectedTuple = Set.of(new Word("arise"), new Word("pound"));
        SolveJob<Set<TupleScore>> job = new SolveJob<>();
        job.setTasks(10);
        when(solvleService.submitTupleJob(expectedTuple, DictionaryType.EXTENDED, true)).thenReturn(job);

        mockMvc.perform(get("/solvle/submitTupleJob/ARISE,POUND")
                        .param("wordList", "EXTENDED")
                        .param("requireAnswer", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.tasks").value(10));

        verify(solvleService).submitTupleJob(expectedTuple, DictionaryType.EXTENDED, true);
    }

    @Test
    void playOutSolution_lowercasesRestrictionsAndPassesGuessNumber() throws Exception {
        when(solvleService.playOutSolutions("abc", DictionaryType.REDUCED, WordConfig.TWO_OR_LESS, true, 3))
                .thenReturn(Set.of(new PlayOut("crane", 2.0, "{2=1}", List.of())));

        mockMvc.perform(get("/solvle/ABC/playout")
                        .param("wordList", "REDUCED")
                        .param("wordConfig", "TWO_OR_LESS")
                        .param("hardMode", "true")
                        .param("guess", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].word").value("crane"))
                .andExpect(jsonPath("$[0].average").value(2.0));

        verify(solvleService).playOutSolutions("abc", DictionaryType.REDUCED, WordConfig.TWO_OR_LESS, true, 3);
    }

    @Test
    void solvePuzzle_lowercasesSolutionAndFirstWord() throws Exception {
        when(solvleService.solveWord(any(Solver.class), eq(new Word("crane")), eq("slate"), eq(DictionaryType.EXTENDED)))
                .thenReturn(List.of("slate", "crane"));

        mockMvc.perform(get("/solvle/solve/CRANE")
                        .param("firstWord", "SLATE")
                        .param("wordList", "EXTENDED")
                        .param("wordConfig", "TWO_OR_LESS")
                        .param("hardMode", "true")
                        .param("requireAnswer", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("slate"))
                .andExpect(jsonPath("$[1]").value("crane"));

        verify(solvleService).solveWord(any(Solver.class), eq(new Word("crane")), eq("slate"), eq(DictionaryType.EXTENDED));
    }

    @Test
    void rateGame_lowercasesSolutionAndRepeatedGuesses() throws Exception {
        GameScoreDTO response = new GameScoreDTO();
        response.addRow(
                "slate",
                new WordScoreDTO(10.0, 2.0, 1.0),
                "crane",
                new WordScoreDTO(5.0, 3.0, 2.0),
                4,
                20,
                new WordFrequencyScore(1, "crane", 4.0, null)
        );
        when(solvleService.rateGame("crane", List.of("slate", "adieu"), DictionaryType.EXTENDED, WordConfig.OPTIMAL_MEAN, true, true))
                .thenReturn(response);

        mockMvc.perform(get("/solvle/rate/CRANE")
                        .param("guesses", "SLATE", "ADIEU")
                        .param("wordList", "EXTENDED")
                        .param("wordConfig", "OPTIMAL_MEAN")
                        .param("hardMode", "true")
                        .param("requireAnswer", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].playerWord").value("slate"))
                .andExpect(jsonPath("$.rows[0].solvleWord").value("crane"))
                .andExpect(jsonPath("$.rows[0].actualRemaining").value(4));

        verify(solvleService).rateGame("crane", List.of("slate", "adieu"), DictionaryType.EXTENDED, WordConfig.OPTIMAL_MEAN, true, true);
    }

    @Test
    void invalidEnumQueryParam_returnsBadRequestBeforeCallingService() throws Exception {
        mockMvc.perform(get("/solvle/abc")
                        .param("wordList", "NOT_A_DICTIONARY"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(solvleService);
    }

    private static SolvleDTO analysisDto() {
        return new SolvleDTO(
                "",
                Set.of(new WordFrequencyScore(1, "crane", 1.5, null)),
                Set.of(new WordFrequencyScore(2, "slate", 1.25, null)),
                Set.of(new WordFrequencyScore(3, "raise", 1.0, new PartitionStats(3.5, 2, 2.25, List.of()))),
                1,
                Map.of(),
                List.of()
        );
    }

    private static TupleScore tupleScore(Set<Word> tuple) {
        return new TupleScore(tuple, new PartitionStats(3.5, 2, 2.25, List.of()));
    }
}
