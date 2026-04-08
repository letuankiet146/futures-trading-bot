package com.trading.simulate.model;

import java.time.Instant;

public record JobTimelineBalance(Instant time, double balanceUsdt) {
}
