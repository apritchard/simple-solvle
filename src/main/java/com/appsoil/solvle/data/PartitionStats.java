package com.appsoil.solvle.data;

import java.util.Comparator;
import java.util.List;

public record PartitionStats(double wordsRemaining, int groupCount, double entropy, List<String> ruts) implements Comparable<PartitionStats> {
    public static PartitionStats getBlank(){return new PartitionStats(1.0, 1, 0, null);}

    @Override
    public int compareTo(PartitionStats o) {
        return Comparator.comparingDouble(PartitionStats::entropy)
                .reversed() // sorts entropy from high to low
                .thenComparingDouble(PartitionStats::wordsRemaining) // sorts wordsRemaining from low to high
                .thenComparingInt(PartitionStats::groupCount)         // sorts groupCount from low to high
                .compare(this, o);
    }

}
