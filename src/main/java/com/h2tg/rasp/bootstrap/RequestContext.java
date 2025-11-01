package com.h2tg.rasp.bootstrap;

/**
 * RequestContext holds HTTP request information in ThreadLocal for context-aware protection.
 * MUST be injected to Bootstrap ClassLoader for cross-classloader access.
 */
public class RequestContext
{

    /**
     * ThreadLocal to store current HTTP request for context-aware protection.
     * MUST be public for direct access from Advice methods.
     */
    public static final ThreadLocal<Object> currentRequest = new ThreadLocal<>();

    public static final ThreadLocal<Object> currentResponse = new ThreadLocal<>();

    public static Object getCurrentRequest()
    {
        return currentRequest.get();
    }

    /**
     * Log HTTP request details to System.err.
     * Extracts and prints method, URI, and parameters from HttpServletRequest.
     *
     * @param request HttpServletRequest object (accessed via reflection)
     */
    public static void logRequestInfo(Object request)
    {
        if (request == null) {
            return;
        }

        try {
            java.lang.reflect.Method getMethodMethod = request.getClass().getMethod("getMethod");
            java.lang.reflect.Method getRequestURIMethod = request.getClass().getMethod("getRequestURI");
            java.lang.reflect.Method getParameterMapMethod = request.getClass().getMethod("getParameterMap");

            String method = (String) getMethodMethod.invoke(request);
            String uri = (String) getRequestURIMethod.invoke(request);
            java.util.Map<String, String[]> params =
                (java.util.Map<String, String[]>) getParameterMapMethod.invoke(request);

            System.err.println("  Request: " + method + " " + uri);
            System.err.print("  Params: ");

            if (params.isEmpty()) {
                System.err.println("(none)");
            } else {
                System.err.println();
                for (java.util.Map.Entry<String, String[]> entry : params.entrySet()) {
                    String[] values = entry.getValue();
                    if (values != null && values.length > 0) {
                        System.err.println("    " + entry.getKey() + "=" + java.util.Arrays.toString(values));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("  Request: (failed to extract details)");
        }
    }

}
