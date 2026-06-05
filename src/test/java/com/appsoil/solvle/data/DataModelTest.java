package com.appsoil.solvle.data;

import com.appsoil.solvle.controller.KnownPositionDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class DataModelTest {

    @Test
    void partitionStats_sortByHighestEntropyThenFewestRemainingThenFewestGroups() {
        PartitionStats highEntropy = new PartitionStats(8.0, 4, 3.0, List.of());
        PartitionStats lowRemaining = new PartitionStats(4.0, 5, 2.0, List.of());
        PartitionStats lowGroupCount = new PartitionStats(4.0, 2, 2.0, List.of());
        PartitionStats lowEntropy = new PartitionStats(1.0, 1, 1.0, List.of());

        TreeSet<PartitionStats> sorted = new TreeSet<>(Set.of(lowEntropy, lowRemaining, lowGroupCount, highEntropy));

        Assertions.assertEquals(List.of(highEntropy, lowGroupCount, lowRemaining, lowEntropy), new ArrayList<>(sorted));
        Assertions.assertEquals(new PartitionStats(1.0, 1, 0, null), PartitionStats.getBlank());
    }

    @Test
    void wordFrequencyScore_sortByPartitionEntropyThenScoreThenNaturalOrder() {
        WordFrequencyScore bestPartition = new WordFrequencyScore(3, "crane", 0.1, new PartitionStats(5.0, 2, 3.0, List.of()));
        WordFrequencyScore weakerPartition = new WordFrequencyScore(1, "slate", 99.0, new PartitionStats(4.0, 2, 2.0, List.of()));
        WordFrequencyScore highScore = new WordFrequencyScore(2, "adieu", 4.0, null);
        WordFrequencyScore tiedEarlier = new WordFrequencyScore(4, "raise", 1.0, null);
        WordFrequencyScore tiedLater = new WordFrequencyScore(5, "roate", 1.0, null);

        TreeSet<WordFrequencyScore> partitionScores = new TreeSet<>(Set.of(weakerPartition, bestPartition));
        TreeSet<WordFrequencyScore> frequencyScores = new TreeSet<>(Set.of(tiedLater, highScore, tiedEarlier));

        Assertions.assertEquals(List.of(bestPartition, weakerPartition), new ArrayList<>(partitionScores));
        Assertions.assertEquals(List.of(highScore, tiedEarlier, tiedLater), new ArrayList<>(frequencyScores));
        Assertions.assertEquals(new WordFrequencyScore(4, "other", 9.0, null), tiedEarlier);
    }

    @Test
    void knownPosition_sortAndDescribeSharedPositions() {
        KnownPosition moreShared = new KnownPosition(Map.of(1, 's', 3, 'a', 5, 'e'));
        KnownPosition fewerShared = new KnownPosition(Map.of(1, 's', 3, 'a'));
        KnownPosition sameSizeLaterPosition = new KnownPosition(Map.of(2, 't', 3, 'a'));

        Assertions.assertTrue(moreShared.compareTo(fewerShared) < 0);
        Assertions.assertTrue(fewerShared.compareTo(sameSizeLaterPosition) < 0);
        Assertions.assertTrue(fewerShared.toString().startsWith("Shares 2 characters: "));
        Assertions.assertTrue(fewerShared.toString().contains("s"));
        Assertions.assertTrue(fewerShared.toString().contains("a"));
        Assertions.assertEquals(2, fewerShared.getShared());
    }

    @Test
    void knownPosition_compareToCoversAllSameSizeBranches() {
        KnownPosition earlyPos = new KnownPosition(Map.of(1, 's', 3, 'a'));
        KnownPosition latePos = new KnownPosition(Map.of(2, 't', 3, 'a'));
        KnownPosition sameKeysLowerChar = new KnownPosition(Map.of(1, 'a', 3, 'a'));
        KnownPosition sameKeysHigherChar = new KnownPosition(Map.of(1, 'b', 3, 'a'));
        KnownPosition identical = new KnownPosition(Map.of(1, 's', 3, 'a'));

        // -1 branch: this has a position the other doesn't
        Assertions.assertTrue(earlyPos.compareTo(latePos) < 0);
        // +1 branch: other has a position this doesn't
        Assertions.assertTrue(latePos.compareTo(earlyPos) > 0);
        // char-compare branch: same positions, different characters
        Assertions.assertTrue(sameKeysLowerChar.compareTo(sameKeysHigherChar) < 0);
        Assertions.assertTrue(sameKeysHigherChar.compareTo(sameKeysLowerChar) > 0);
        // identical positions throw IllegalStateException - loop completes without returning
        Assertions.assertThrows(IllegalStateException.class, () -> earlyPos.compareTo(identical));
    }

    @Test
    void sharedPositions_sortedPositionStreamOrdersBySharedThenByWordCountThenByPositionKey() {
        KnownPosition threeShared = new KnownPosition(Map.of(1, 's', 3, 'a', 5, 'e'));
        KnownPosition twoSharedManyWords = new KnownPosition(Map.of(1, 's', 3, 'a'));
        KnownPosition twoSharedFewerWords = new KnownPosition(Map.of(2, 't', 3, 'a'));

        SharedPositions positions = new SharedPositions(Map.of(
                threeShared, Set.of(new Word("stare"), new Word("share")),
                twoSharedManyWords, Set.of(new Word("sat"), new Word("sap"), new Word("sad")),
                twoSharedFewerWords, Set.of(new Word("eta"), new Word("ita"))
        ));

        List<KnownPosition> sorted = positions.sortedPositionStream()
                .map(Map.Entry::getKey)
                .toList();

        // larger shared count comes first; among same shared count, more words comes first;
        // tie-break falls through to keyCompare on the KnownPosition itself.
        Assertions.assertEquals(List.of(threeShared, twoSharedManyWords, twoSharedFewerWords), sorted);
    }

    @Test
    void sharedPositions_buildsThresholdedDtosAndDescriptions() {
        KnownPosition position = new KnownPosition(Map.of(1, 's', 3, 'a'));
        Set<Word> words = Set.of(new Word("stare"), new Word("share"));
        Set<WordFrequencyScore> recommendations = Set.of(new WordFrequencyScore(1, "raise", 2.5, null));
        SharedPositions sharedPositions = new SharedPositions(Map.of(position, words), Map.of(position, recommendations));

        List<KnownPositionDTO> dtos = sharedPositions.toKnownPositionDTOList(2);

        Assertions.assertEquals(2, sharedPositions.largestSet());
        Assertions.assertEquals(List.of("s_a__"), sharedPositions.getDescription());
        Assertions.assertEquals(1, dtos.size());
        Assertions.assertEquals("s_a__", dtos.get(0).position());
        Assertions.assertEquals(Set.of("stare", "share"), dtos.get(0).words());
        Assertions.assertEquals(recommendations, dtos.get(0).recommendations());
        Assertions.assertTrue(sharedPositions.toKnownPositionDTOList(3).isEmpty());
        Assertions.assertEquals(recommendations, sharedPositions.withRecommendations(Map.of(position, recommendations)).recommendations().get(position));
    }

    @Test
    void playOut_sortsByAverageAndUsesWordIdentityForEquality() {
        PlayOut lowerAverage = new PlayOut("crane", 2.0, "{2=1}", List.of());
        PlayOut higherAverage = new PlayOut("slate", 3.0, "{3=1}", List.of());
        PlayOut sameAverageAlphabetical = new PlayOut("adieu", 2.0, "{2=1}", List.of());
        PlayOut sameWordDifferentStats = new PlayOut("crane", 9.0, "{9=1}", List.of(List.of("crane")));

        TreeSet<PlayOut> sorted = new TreeSet<>(Set.of(higherAverage, lowerAverage, sameAverageAlphabetical));

        Assertions.assertEquals(List.of(sameAverageAlphabetical, lowerAverage, higherAverage), new ArrayList<>(sorted));
        Assertions.assertEquals(lowerAverage, sameWordDifferentStats);
        Assertions.assertEquals(lowerAverage.hashCode(), sameWordDifferentStats.hashCode());
    }

    @Test
    void tupleScore_delegatesOrderingToPartitionStats() {
        TupleScore better = new TupleScore(Set.of(new Word("crane")), new PartitionStats(5.0, 2, 4.0, List.of()));
        TupleScore worse = new TupleScore(Set.of(new Word("slate")), new PartitionStats(3.0, 2, 2.0, List.of()));

        TreeSet<TupleScore> sorted = new TreeSet<>(Set.of(worse, better));

        Assertions.assertEquals(List.of(better, worse), new ArrayList<>(sorted));
    }
}
