package com.appsoil.solvle.data;

public record PartitionStats(double wordsRemaining, int groupCount, double entropy){
    public static PartitionStats getBlank(){return new PartitionStats(1.0, 1, 0);};
}
