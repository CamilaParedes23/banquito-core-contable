package com.banquito.core.accounting.shared.util;

public final class JsonUtil {
    private JsonUtil() {}
    public static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
