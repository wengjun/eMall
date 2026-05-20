package com.emall.common.web;

public enum OutboundClientErrorCategory {
    RATE_LIMITED,
    TIMEOUT,
    DOWNSTREAM_5XX,
    BULKHEAD_REJECTED,
    TRANSPORT_ERROR,
    BUSINESS_REJECTED
}
