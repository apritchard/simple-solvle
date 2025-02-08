package com.appsoil.solvle.data;

import java.util.Comparator;
import java.util.Set;

public record TupleScore(Set<Word> tuple, PartitionStats partitionStats) implements Comparable<TupleScore> {
    @Override
    public int compareTo(TupleScore o) {
        return Comparator.comparing(TupleScore::partitionStats).compare(this, o);
    }
}

