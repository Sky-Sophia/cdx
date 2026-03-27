package org.example.javawebdemo.task;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("legacy-cinema")
public class SeatLockScheduler {
    // Legacy scheduler disabled.
}
