package org.example.javawebdemo.task;

import org.example.javawebdemo.service.SeatService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SeatLockScheduler {
    private final SeatService seatService;

    public SeatLockScheduler(SeatService seatService) {
        this.seatService = seatService;
    }

    @Scheduled(fixedDelay = 60000)
    public void releaseExpiredLocks() {
        seatService.releaseExpired();
    }
}
