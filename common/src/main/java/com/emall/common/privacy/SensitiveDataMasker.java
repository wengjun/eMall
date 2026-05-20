package com.emall.common.privacy;

import java.util.regex.Pattern;

public final class SensitiveDataMasker {
    private static final Pattern MOBILE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");
    private static final Pattern JSON_SECRET_PATTERN = Pattern.compile(
            "(?i)(\"(?:authorization|token|accessToken|refreshToken|signature)\"\\s*:\\s*\")(?!\\*{3}\")[^\"]+(\")");
    private static final Pattern JSON_PAYMENT_REFERENCE_PATTERN =
            Pattern.compile("(?i)(\"(?:channelTradeNo|tradeNo|paymentToken)\"\\s*:\\s*\")(?!\\*{3}\")[^\"]+(\")");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/-]+=*");

    private SensitiveDataMasker() {
    }

    public static String mask(SensitiveDataType type, String value) {
        if (value == null) {
            return null;
        }
        return switch (type) {
            case MOBILE -> maskMobile(value);
            case TOKEN -> maskToken(value);
            case PAYMENT_REFERENCE -> maskPaymentReference(value);
            case CARD_NUMBER -> keepEdges(value, 6, 4);
            case EMAIL -> maskEmail(value);
            case GENERAL_IDENTIFIER -> keepEdges(value, 3, 3);
            case NONE -> value;
        };
    }

    public static String maskMobile(String value) {
        return MOBILE_PATTERN.matcher(value).replaceAll("$1****$2");
    }

    public static String maskToken(String value) {
        return keepEdges(value, 4, 4);
    }

    public static String maskPaymentReference(String value) {
        return keepEdges(value, 4, 4);
    }

    public static String maskFreeText(String value) {
        if (value == null) {
            return null;
        }
        String masked = maskMobile(value);
        masked = JSON_SECRET_PATTERN.matcher(masked).replaceAll("$1***$2");
        masked = JSON_PAYMENT_REFERENCE_PATTERN.matcher(masked).replaceAll("$1***$2");
        return BEARER_PATTERN.matcher(masked).replaceAll("$1***");
    }

    public static boolean containsRawSensitiveData(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return MOBILE_PATTERN.matcher(value).find() || JSON_SECRET_PATTERN.matcher(value).find()
                || JSON_PAYMENT_REFERENCE_PATTERN.matcher(value).find() || BEARER_PATTERN.matcher(value).find();
    }

    private static String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    private static String keepEdges(String value, int prefix, int suffix) {
        if (value.isBlank()) {
            return value;
        }
        if (value.length() <= prefix + suffix) {
            return "***";
        }
        return value.substring(0, prefix) + "***" + value.substring(value.length() - suffix);
    }
}
