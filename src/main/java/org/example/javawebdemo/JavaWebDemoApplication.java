package org.example.javawebdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.javawebdemo.mapper")
public class JavaWebDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaWebDemoApplication.class, args);
    }
}
