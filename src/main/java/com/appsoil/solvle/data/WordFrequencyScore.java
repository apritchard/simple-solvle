package com.appsoil.solvle.data;

public record WordFrequencyScore(int naturalOrdering, String word, double freqScore, PartitionStats partitionStats) implements Comparable<WordFrequencyScore> {
    @Override
    public int compareTo(WordFrequencyScore other) {
        if(partitionStats != null && other.partitionStats != null) {
            if(partitionStats.entropy() != other.partitionStats().entropy()) {
                //return Double.compare(partitionStats().entropy(), other.partitionStats().entropy()); //reverse
                return Double.compare(other.partitionStats().entropy(), partitionStats().entropy());
            }
        }
        if (freqScore == other.freqScore) {
            //return other.naturalOrdering - naturalOrdering; //reverse
            return naturalOrdering - other.naturalOrdering;
        } else {
            //return Double.compare(freqScore, other.freqScore); //reverse
            return Double.compare(other.freqScore, freqScore);
        }
    }

    // these should always have a unique natural ordering when created, so ignore everything else for faster compares
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return naturalOrdering == ((WordFrequencyScore) o).naturalOrdering;
    }

    @Override
    public int hashCode() {
        return naturalOrdering;
    }
}
