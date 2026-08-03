package com.doxa.crm.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateFormatter {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MMM d yyyy, h:mma", Locale.ENGLISH);

    private DateFormatter() {
    }

    public static String formatOpportunityDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        String formatted = instant.atZone(DISPLAY_ZONE).format(FORMATTER).toLowerCase(Locale.ENGLISH);
        return formatted + " (GMT-4)";
    }

    public static String formatContactDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return instant.atZone(DISPLAY_ZONE).format(DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH));
    }
}
