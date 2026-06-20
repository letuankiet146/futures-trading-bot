package com.trading.contracts.util;

/** Binance kline interval string to step length in milliseconds (open-to-open). */
public final class IntervalMillis {

    private IntervalMillis() {}

    public static long parse(String interval) {
        if (interval == null || interval.isBlank()) {
            throw new IllegalArgumentException("interval is blank");
        }
        String s = interval.trim();
        char unit = s.charAt(s.length() - 1);
        long n = Long.parseLong(s.substring(0, s.length() - 1));
        return switch (unit) {
            case 'm', 'M' -> n * 60_000L;
            case 'h', 'H' -> n * 3_600_000L;
            case 'd', 'D' -> n * 86_400_000L;
            case 'w', 'W' -> n * 7L * 86_400_000L;
            default -> throw new IllegalArgumentException("Unsupported interval: " + interval);
        };
    }
}
