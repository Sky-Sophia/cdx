package org.example.javawebdemo.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class OrderNoUtils {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoUtils() {}

    public static String generate() {
        String ts = LocalDateTime.now().format(FORMATTER);
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return ts + rand;
    }
}
