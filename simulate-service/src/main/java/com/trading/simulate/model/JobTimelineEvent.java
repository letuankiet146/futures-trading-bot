package com.trading.simulate.model;

import java.time.Instant;

public record JobTimelineEvent(
        String type,
        String side,
        double price,
        double quantity,
        Double tp,
        Double sl,
        Instant time) {
}
