package com.trading.simulate.model;

import java.util.List;

public record JobTimeline(
        String jobId,
        List<JobTimelineCandle> candles,
        List<JobTimelineEvent> events,
        List<JobTimelineBalance> balance,
        JobTimelineSummary summary,
        JobTimelineOpenPosition openPosition) {
}
