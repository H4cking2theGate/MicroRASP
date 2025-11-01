package com.h2tg.rasp.hooks;

import com.h2tg.rasp.annotation.HookHandler;
import com.h2tg.rasp.bootstrap.JndiHelper;
import net.bytebuddy.asm.Advice;

import static com.h2tg.rasp.bootstrap.JndiHelper.getFactoryLocation;

/**
 * Hook for JNDI/RMI injection monitoring and blocking.
 * Intercepts JNDI and RMI operations to detect dangerous object factories and remote code loading.
 *
 * IMPORTANT: All logic must be inlined in Advice methods.
 * Do NOT call helper methods as they won't be available in Bootstrap ClassLoader.
 */
public class JndiHook {

    /**
     * Hook for NamingManager.getObjectFactoryFromReference method.
     * This method is called to get an ObjectFactory from a Reference during JNDI lookup.
     *
     * Method signature in JDK:
     * static ObjectFactory getObjectFactoryFromReference(Reference ref, String factoryName)
     */
    @HookHandler(
            hookClass = "javax.naming.spi.NamingManager",
            hookMethod = "getObjectFactoryFromReference"
    )
    public static class GetObjectFactoryFromReferenceAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(0) Object ref,
                           @Advice.Argument(1) String factoryName,
                           @Advice.Origin("#t.#m") String origin)
        {
            if (ref == null) {
                return;
            }

            Class<?> refClass = ref.getClass();

            // Step 1: Check factoryClassLocation (remote code loading)
            String factoryLocation = getFactoryLocation(refClass, ref);
            if (factoryLocation != null && !factoryLocation.isEmpty()) {
                System.err.println("[MicroRASP] [BLOCKED] Remote JNDI factory location: "+ factoryLocation);
                throw new SecurityException("MicroRASP blocked remote JNDI factory location: " + factoryLocation);
            }


            String matchedPattern = JndiHelper.checkDenyFactory(factoryName);
            if (matchedPattern != null) {
                System.err.println("[MicroRASP] [BLOCKED] Dangerous JNDI factory class: "+ factoryName);
                throw new SecurityException("MicroRASP blocked dangerous JNDI factory: " + factoryName);
            }
        }
    }

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
