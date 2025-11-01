package com.h2tg.rasp.hooks;

import com.h2tg.rasp.annotation.HookHandler;
import com.h2tg.rasp.bootstrap.RequestContext;
import net.bytebuddy.asm.Advice;

/**
 * Hook for JNI library loading monitoring and blocking.
 * Intercepts native library loading to prevent malicious library injection.
 */
public class JNIHook {

    /**
     * Hook for jdk.internal.loader.NativeLibraries.load (JDK 9+)
     */
    @HookHandler(
            hookClass = "jdk.internal.loader.NativeLibraries",
            hookMethod = "load",
            isNative = true
    )
    public static class NativeLibrariesAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(0) Object nativeLibrary,
                           @Advice.Argument(1) String name) {
            System.err.println("[MicroRASP] [BLOCKED] Native library loading: " + name);
            throw new SecurityException("MicroRASP blocked native library loading: " + name);
        }
    }

    /**
     * Hook for java.lang.ClassLoader.NativeLibrary.load (JDK 8)
     */
    @HookHandler(
            hookClass = "java.lang.ClassLoader.NativeLibrary",
            hookMethod = "load",
            isNative = true
    )
    public static class NativeLibraryAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(0) String name) {
            System.err.println("[MicroRASP] [BLOCKED] Native library loading: " + name);
            throw new SecurityException("MicroRASP blocked native library loading: " + name);
        }
    }
}
