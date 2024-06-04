package com.appsoil.solvle.data;

import java.util.List;

public record PartitionStats(double wordsRemaining, int groupCount, double entropy, List<String> ruts){
    public static PartitionStats getBlank(){return new PartitionStats(1.0, 1, 0, null);}
}
