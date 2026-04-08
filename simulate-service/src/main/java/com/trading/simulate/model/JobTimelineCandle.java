package com.trading.simulate.model;

import java.time.Instant;

public record JobTimelineCandle(
        Instant time,
        double open,
        double high,
        double low,
        double close,
        double volume) {
}
