package com.appsoil.solvle.service.job;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class SolveJob<T> {
    private UUID id;
    private int tasks;
    private AtomicInteger completedTasks;
    private T result;
    private JobStatus status;
    private String error;
    private LocalDateTime finishTime;

    public SolveJob() {
        this.id = UUID.randomUUID();
        this.status = JobStatus.PENDING;
    }
}
