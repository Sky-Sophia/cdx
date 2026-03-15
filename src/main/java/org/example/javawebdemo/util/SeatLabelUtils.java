package org.example.javawebdemo.util;

public final class SeatLabelUtils {
    private SeatLabelUtils() {}

    public static String rowLabel(int row) {
        if (row <= 26) {
            return String.valueOf((char) ('A' + row - 1));
        }
        return "R" + row;
    }

    public static String seatLabel(int row, int col) {
        return rowLabel(row) + col;
    }
}
