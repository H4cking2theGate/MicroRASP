package com.h2tg.rasp.hooks;

import com.h2tg.rasp.annotation.HookHandler;
import com.h2tg.rasp.bootstrap.RequestContext;
import com.h2tg.rasp.bootstrap.SerialHelper;
import net.bytebuddy.asm.Advice;

import static com.h2tg.rasp.bootstrap.SerialHelper.getClassName;

/**
 * Hook for Java deserialization monitoring and blocking.
 * Intercepts ObjectInputStream.resolveClass to detect potentially dangerous deserialization.
 *
 * IMPORTANT: All logic must be inlined in Advice methods.
 * Do NOT call helper methods as they won't be available in Bootstrap ClassLoader.
 */
public class SerialHook {

    /**
     * Hook for ObjectInputStream.resolveClass method
     * This method is called during deserialization to resolve class descriptors to actual classes.
     */
    @HookHandler(
            hookClass = "java.io.ObjectInputStream",
            hookMethod = "resolveClass",
            parameterTypes = {"java.io.ObjectStreamClass"}
    )
    public static class ResolveClassAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(0) Object desc) {
            String className = getClassName(desc);
            if (className == null || className.isEmpty()) {
                return;
            }

            String matchedPattern = SerialHelper.checkDenyClass(className);
            if (matchedPattern == null) {
                return;
            }

            Object request = RequestContext.getCurrentRequest();
            System.err.println("[MicroRASP] [BLOCKED] Dangerous deserialization: " + className);
            if (request != null) {
                RequestContext.logRequestInfo(request);
            }

            throw new SecurityException("MicroRASP blocked dangerous deserialization: " + className);
        }
    }
}
