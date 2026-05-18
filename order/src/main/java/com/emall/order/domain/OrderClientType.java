package com.emall.order.domain;

public enum OrderClientType {
    WEB,
    APP;

    public static OrderClientType defaultIfNull(OrderClientType clientType) {
        return clientType == null ? WEB : clientType;
    }
}
