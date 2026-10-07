package com.example.urlshortener.util;

import java.security.SecureRandom;

public final class Base62Encoder {

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = BASE62_CHARS.length();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Base62Encoder() {
        // Utility class
    }

    /**
     * Encodes a numeric ID into a Base62 string.
     */
    public static String encode(long num) {
        if (num <= 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            int remainder = (int) (num % BASE);
            sb.append(BASE62_CHARS.charAt(remainder));
            num /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back into a numeric ID.
     */
    public static long decode(String str) {
        long num = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int charIndex = BASE62_CHARS.indexOf(c);
            if (charIndex == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            num = num * BASE + charIndex;
        }
        return num;
    }

    /**
     * Generates a random Base62 salt/string of given length.
     */
    public static String generateRandom(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(RANDOM.nextInt(BASE)));
        }
        return sb.toString();
    }
}
