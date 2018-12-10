package io.omnition.loadgenerator.util;

public class SpanConventions {
    // Span tags as suggested by OpenTracing semantic conventions here:
    // https://github.com/opentracing/specification/blob/master/semantic_conventions.md
    public static String HTTP_METHOD_KEY = "http.method";
    public static String HTTP_STATUS_CODE_KEY = "http.status_code";
    public static String HTTP_URL_KEY = "http.url";
    public static String IS_ERROR_KEY = "error";

    // Omnition conventions
    public static String IS_ROOT_CAUSE_ERROR_KEY = "root_cause_error";
}
