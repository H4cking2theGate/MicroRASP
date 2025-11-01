//package com.h2tg.rasp.hooks;
//
//import com.h2tg.rasp.annotation.HookHandler;
//import net.bytebuddy.asm.Advice;
//
//public class TestHook
//{
//    @HookHandler(
//            hookClass = "org.h2tg.demo.WebController",
//            hookMethod = "hello"
//    )
//    public static class WebControllerAdvice {
//
//        @Advice.OnMethodEnter
//        static void onEnter(@Advice.AllArguments Object[] args,
//                            @Advice.Origin("#t.#m") String origin) {
//            try {
//                if (args != null && args.length > 0 && args[0] != null) {
//                    String cmdstr = (String) args[0];
//
//                    // Log: Intercepted command
//                    System.err.println("[MicroRASP] [INFO] Intercepted hello: " + cmdstr);
//                }
//            } catch (SecurityException e) {
//                throw e;
//            } catch (Throwable t) {
//                System.err.println("[MicroRASP] [ERROR] Error in WindowsCreateAdvice: " + t.getMessage());
//            }
//        }
//    }
//}
