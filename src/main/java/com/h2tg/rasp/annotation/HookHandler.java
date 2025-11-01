package com.h2tg.rasp.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark Hook handler classes for automatic registration.
 * Used by HookRegistry to scan and register hooks with Byte Buddy.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface HookHandler {
    /**
     * Target class to hook (full qualified name)
     */
    String hookClass();

    /**
     * Target method name to hook
     */
    String hookMethod();

    /**
     * Parameter types of the target method (use "*" for any)
     */
    String[] parameterTypes() default {"*"};

    /**
     * Whether the target method is a constructor
     */
    boolean isConstructor() default false;

    /**
     * Whether the target method is native
     */
    boolean isNative() default false;
}
