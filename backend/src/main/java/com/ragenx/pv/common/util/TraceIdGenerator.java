package com.ragenx.pv.common.util;

import java.security.SecureRandom;

/**
 * Generates a 10-character trace id: 5 chars encoding the epoch-second timestamp
 * (roughly time-ordered) + 5 random chars, from an ambiguity-free charset
 * (no I, O, 0, 1, l). Carried over from a prior project. See docs/conventions.md §5.
 */
public final class TraceIdGenerator {

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TIMESTAMP_LENGTH = 5;
    private static final int RANDOM_LENGTH = 5;
    private static final int TRACE_ID_LENGTH = TIMESTAMP_LENGTH + RANDOM_LENGTH;

    private TraceIdGenerator() {
    }

    public static String generate() {
        String timestampPart = encodeTimestamp(System.currentTimeMillis() / 1000, TIMESTAMP_LENGTH);
        String randomPart = generateRandomString(RANDOM_LENGTH);
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
        if (traceId == null || traceId.length() != TRACE_ID_LENGTH) {
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
