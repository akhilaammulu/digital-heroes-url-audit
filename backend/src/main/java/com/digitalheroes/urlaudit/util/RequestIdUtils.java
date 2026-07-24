package com.digitalheroes.urlaudit.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestIdUtils {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = RequestIdUtils.class.getName() + ".requestId";
    public static final String MDC_KEY = "requestId";

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private RequestIdUtils() {
    }

    public static String resolve(String candidate) {
        if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    public static String from(HttpServletRequest request) {
        Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (requestId instanceof String value && !value.isBlank()) {
            return value;
        }
        return resolve(request.getHeader(HEADER_NAME));
    }

    public static String current() {
        String requestId = MDC.get(MDC_KEY);
        return requestId == null || requestId.isBlank() ? "unknown" : requestId;
    }
}
