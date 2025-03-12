package com.appsoil.solvle.service.job;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class SolveJob<T> {
    private UUID id;
    private int tasks;
    private AtomicInteger completedTasks;
    private AtomicInteger evaluatedTuples;
    private T result;
    private JobStatus status;
    private String error;
    private LocalDateTime startTime;
    private LocalDateTime lastUpdate;

    public SolveJob() {
        this.id = UUID.randomUUID();
        this.status = JobStatus.PENDING;
        this.startTime = LocalDateTime.now();
        this.lastUpdate = LocalDateTime.now();
    }

    public Duration runTime() {
        return switch (status) {
            case PENDING -> Duration.ZERO;
            case RUNNING -> Duration.between(startTime, LocalDateTime.now());
            default -> Duration.between(startTime, lastUpdate);
        };
    }
}
