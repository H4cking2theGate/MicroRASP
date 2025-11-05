package com.h2tg.rasp.hooks;

import com.h2tg.rasp.annotation.HookHandler;
import com.h2tg.rasp.bootstrap.RequestContext;
import net.bytebuddy.asm.Advice;

/**
 * Hook for HTTP request tracking in Servlet/Spring Boot applications.
 * Captures HTTP requests and stores them in RequestContext ThreadLocal for context-aware protection.
 */
public class RequestHook {

    /**
     * Hook for javax.servlet.http.HttpServlet.service method (Java 8 / Spring Boot 2.x)
     * Hooks the public service(ServletRequest, ServletResponse) which is the actual entry point
     * for ALL requests including JSP pages.
     */
    @HookHandler(
            hookClass = "javax.servlet.http.HttpServlet",
            hookMethod = "service",
            parameterTypes = {
                "javax.servlet.ServletRequest",
                "javax.servlet.ServletResponse"
            }
    )
    public static class ServletServiceAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(0) Object request) {
            RequestContext.currentRequest.set(request);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        static void onExit() {
            RequestContext.currentRequest.remove();
        }
    }

    /**
     * Hook for jakarta.servlet.http.HttpServlet.service method (Java 11+ / Spring Boot 3.x)
     * Hooks the public service(ServletRequest, ServletResponse) which is the actual entry point
     * for ALL requests including JSP pages.
     */
    @HookHandler(
            hookClass = "jakarta.servlet.http.HttpServlet",
            hookMethod = "service",
            parameterTypes = {
                "jakarta.servlet.ServletRequest",
                "jakarta.servlet.ServletResponse"
            }
    )
    public static class JakartaServletServiceAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(0) Object request) {
            RequestContext.currentRequest.set(request);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        static void onExit() {
            RequestContext.currentRequest.remove();
        }
    }
}
