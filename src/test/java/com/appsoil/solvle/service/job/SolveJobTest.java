package com.appsoil.solvle.service.job;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class SolveJobTest {

    @Test
    void constructor_initializesPendingJobMetadata() {
        SolveJob<Set<String>> job = new SolveJob<>();

        Assertions.assertNotNull(job.getId());
        Assertions.assertEquals(JobStatus.PENDING, job.getStatus());
        Assertions.assertNotNull(job.getStartTime());
        Assertions.assertNotNull(job.getLastUpdate());
        Assertions.assertEquals(Duration.ZERO, job.runTime());
    }

    @Test
    void runTime_usesCurrentTimeForRunningJobsAndLastUpdateForTerminalJobs() {
        SolveJob<String> job = new SolveJob<>();
        LocalDateTime start = LocalDateTime.now().minusSeconds(10);

        job.setStartTime(start);
        job.setStatus(JobStatus.RUNNING);

        Assertions.assertFalse(job.runTime().isNegative());

        job.setStatus(JobStatus.COMPLETED);
        job.setLastUpdate(start.plusSeconds(7));

        Assertions.assertEquals(Duration.ofSeconds(7), job.runTime());
    }

    @Test
    void lombokAccessors_allowProgressAndResultUpdates() {
        SolveJob<String> job = new SolveJob<>();

        job.setTasks(10);
        job.setCompletedTasks(new AtomicInteger(3));
        job.setEvaluatedTuples(new AtomicInteger(4));
        job.setResult("done");
        job.setStatus(JobStatus.FAILED);
        job.setError("boom");

        Assertions.assertEquals(10, job.getTasks());
        Assertions.assertEquals(3, job.getCompletedTasks().get());
        Assertions.assertEquals(4, job.getEvaluatedTuples().get());
        Assertions.assertEquals("done", job.getResult());
        Assertions.assertEquals(JobStatus.FAILED, job.getStatus());
        Assertions.assertEquals("boom", job.getError());
    }
}
