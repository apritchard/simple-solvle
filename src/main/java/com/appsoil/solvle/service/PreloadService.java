package com.appsoil.solvle.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
@Log4j2
public class PreloadService {

    private SolvleService solvleService;

    public PreloadService(SolvleService solvleService) {
        this.solvleService = solvleService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
//        solvleService.preloadPartitionData().thenAccept(result -> {
//            log.info("Preload Finished");
//        }).exceptionally(ex -> {
//            log.error("Preload failed", ex);
//            return null;
//        });
    }
}
