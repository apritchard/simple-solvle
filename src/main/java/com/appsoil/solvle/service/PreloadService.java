package com.appsoil.solvle.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class PreloadService {

    private SolvleService solvleService;

    public PreloadService(SolvleService solvleService) {
        this.solvleService = solvleService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        solvleService.preloadPartitionData();
    }
}
