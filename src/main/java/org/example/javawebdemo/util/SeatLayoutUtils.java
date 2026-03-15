package org.example.javawebdemo.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.example.javawebdemo.model.SeatLayout;

public final class SeatLayoutUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SeatLayoutUtils() {}

    public static SeatLayout parse(String json) {
        try {
            if (json == null || json.isBlank()) {
                return null;
            }
            return MAPPER.readValue(json, SeatLayout.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid seat layout JSON", ex);
        }
    }

    public static String buildJson(int rows, int cols, String disabledRaw) {
        try {
            SeatLayout layout = new SeatLayout();
            layout.setRows(rows);
            layout.setCols(cols);
            if (disabledRaw != null && !disabledRaw.isBlank()) {
                String[] parts = disabledRaw.split(",");
                List<String> disabled = new ArrayList<>();
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        disabled.add(trimmed);
                    }
                }
                layout.setDisabled(disabled);
            }
            return MAPPER.writeValueAsString(layout);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to build seat layout JSON", ex);
        }
    }
}
