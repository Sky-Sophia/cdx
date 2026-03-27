package org.example.javawebdemo.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class CodeGenerator {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private CodeGenerator() {
    }

    public static String nextWorkOrderNo() {
        return "WO" + TS.format(LocalDateTime.now()) + random2();
    }

    public static String nextBillNo() {
        return "BL" + TS.format(LocalDateTime.now()) + random2();
    }

    private static String random2() {
        return String.format("%02d", ThreadLocalRandom.current().nextInt(0, 100));
    }
}
