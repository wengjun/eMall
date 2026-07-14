package com.emall.payment.channel;

public record ChannelRefundResult(String channelRefundNo, ChannelOperationStatus status, String message) {
}
