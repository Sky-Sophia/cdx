package org.example.javawebdemo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JavaWebDemoApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        assertEquals("never", environment.getProperty("spring.sql.init.mode"));
    }

}
