package com.trading.strategy.backtest.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Parses backtest wire dates: ISO-8601 instant, epoch ms string, or date-only in JVM default zone. */
public final class BacktestDateParser {

    private static final Pattern DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern EPOCH_MS = Pattern.compile("^-?\\d{1,19}$");

    private BacktestDateParser() {}

    public static Instant parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("date string is null");
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("date string is empty");
        }
        if (EPOCH_MS.matcher(s).matches()) {
            return Instant.ofEpochMilli(Long.parseLong(s));
        }
        if (DATE_ONLY.matcher(s).matches()) {
            LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            ZoneId z = ZoneId.systemDefault();
            return d.atStartOfDay(z).toInstant();
        }
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException ignored) {
            // e.g. no zone: 2026-04-01T00:00:00
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Unrecognized date format: " + raw, e);
        }
    }
}
