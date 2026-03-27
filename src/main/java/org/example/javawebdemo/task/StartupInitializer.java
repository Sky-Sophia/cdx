package org.example.javawebdemo.task;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("legacy-cinema")
public class StartupInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // Legacy movie bootstrap is disabled in property-management mode.
    }
}
