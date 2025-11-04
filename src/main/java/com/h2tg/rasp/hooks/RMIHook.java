package com.h2tg.rasp.hooks;

import com.h2tg.rasp.annotation.HookHandler;
import net.bytebuddy.asm.Advice;

public class RMIHook
{
    /**
     * Hook for sun.rmi.server.LoaderHandler.lookupLoader
     * Blocks any RMI codebase (remote class loading)
     */
    @HookHandler(
            hookClass = "sun.rmi.server.LoaderHandler",
            hookMethod = "lookupLoader"
    )
    public static class RMILookupLoaderAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(0) Object urls) {
            if (urls == null) {
                return;
            }

            if (urls instanceof java.net.URL[]) {
                java.net.URL[] urlArray = (java.net.URL[]) urls;
                if (urlArray.length == 0) {
                    return;
                }

                System.err.println("[MicroRASP] [BLOCKED] RMI codebase: " + java.util.Arrays.toString(urlArray));
                throw new SecurityException("MicroRASP blocked RMI codebase: " + java.util.Arrays.toString(urlArray));
            }
        }
    }
}
