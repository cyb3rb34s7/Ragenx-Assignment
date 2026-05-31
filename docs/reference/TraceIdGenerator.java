package com.cms_monitoring_system.common.util;

import java.security.SecureRandom;

public class TraceIdGenerator {

private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
private static final SecureRandom RANDOM = new SecureRandom();

private TraceIdGenerator() {}

public static String generate() {
    // 5 chars for timestamp + 5 chars random = 10 total
    String timestampPart = encodeTimestamp(System.currentTimeMillis() / 1000, 5);
    String randomPart = generateRandomString(5);
    return timestampPart + randomPart;
}

private static String encodeTimestamp(long timestamp, int length) {
    StringBuilder result = new StringBuilder();
    long value = timestamp;
    
    for (int i = 0; i < length; i++) {
        result.insert(0, CHARSET.charAt((int) (value % CHARSET.length())));
        value /= CHARSET.length();
    }
    
    return result.toString();
}

private static String generateRandomString(int length) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < length; i++) {
        result.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
    }
    return result.toString();
}

public static boolean isValidTraceId(String traceId) {
    if (traceId == null || traceId.length() != 10) {
        return false;
    }
    
    for (char c : traceId.toCharArray()) {
        if (CHARSET.indexOf(c) == -1) {
            return false;
        }
    }
    
    return true;
}
}