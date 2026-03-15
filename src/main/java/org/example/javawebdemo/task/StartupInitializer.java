package org.example.javawebdemo.task;

import org.example.javawebdemo.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class StartupInitializer implements ApplicationRunner {
    private final UserService userService;

    public StartupInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.ensureDefaultUsers();
    }
}
