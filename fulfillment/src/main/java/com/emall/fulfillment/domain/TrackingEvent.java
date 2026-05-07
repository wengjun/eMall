package com.emall.fulfillment.domain;

import java.time.Instant;

public record TrackingEvent(long eventId, long fulfillmentId, String carrierCode, String trackingNo, String eventCode,
        Instant eventTime, String location, String description, Instant receivedAt) {
}
