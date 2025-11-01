package com.h2tg.rasp.hooks;

import com.h2tg.rasp.annotation.HookHandler;
import com.h2tg.rasp.bootstrap.RequestContext;
import net.bytebuddy.asm.Advice;

/**
 * Hook for process execution monitoring and blocking.
 * Intercepts process creation on both Windows and Linux platforms.
 *
 * IMPORTANT: All logic must be inlined in Advice methods.
 * Do NOT call helper methods as they won't be available in Bootstrap ClassLoader.
 */
public class ProcessHook {

    /**
     * Hook for Windows ProcessImpl.create native method
     */
    @HookHandler(
            hookClass = "java.lang.ProcessImpl",
            hookMethod = "create",
            isNative = true
    )
    public static class WindowsCreateAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(0) String cmdstr) {
            Object request = RequestContext.getCurrentRequest();
            if (request == null || cmdstr == null) {
                return;
            }

            System.err.println("[MicroRASP] [BLOCKED] Command execution: " + cmdstr);
            RequestContext.logRequestInfo(request);
            throw new SecurityException("MicroRASP blocked command execution: " + cmdstr);
        }
    }

    /**
     * Hook for Linux ProcessImpl.forkAndExec (JDK 9+)
     */
    @HookHandler(
            hookClass = "java.lang.ProcessImpl",
            hookMethod = "forkAndExec",
            isNative = true
    )
    public static class ProcessForkAndExecAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(2) byte[] prog) {
            Object request = RequestContext.getCurrentRequest();
            if (request == null || prog == null) {
                return;
            }

            String cmd = new String(prog).replace("\0", " ").trim();
            System.err.println("[MicroRASP] [BLOCKED] Command execution: " + cmd);
            RequestContext.logRequestInfo(request);
            throw new SecurityException("MicroRASP blocked command execution: " + cmd);
        }
    }

    /**
     * Hook for Linux UNIXProcess.forkAndExec (JDK 8)
     */
    @HookHandler(
            hookClass = "java.lang.UNIXProcess",
            hookMethod = "forkAndExec",
            isNative = true
    )
    public static class UnixForkAndExecAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(2) byte[] prog) {
            Object request = RequestContext.getCurrentRequest();
            if (request == null || prog == null) {
                return;
            }

            String cmd = new String(prog).replace("\0", " ").trim();
            System.err.println("[MicroRASP] [BLOCKED] Command execution: " + cmd);
            RequestContext.logRequestInfo(request);
            throw new SecurityException("MicroRASP blocked command execution: " + cmd);
        }
    }
}
