package com.h2tg.rasp.core;

import com.h2tg.rasp.annotation.HookHandler;
import com.h2tg.rasp.log.MicroLogger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Registry for automatically discovering and registering hooks.
 * Scans for classes annotated with @HookHandler and registers them with Byte Buddy.
 */
public class HookRegistry {

    private static HookRegistry instance;
    private static final String HOOKS_PACKAGE = "com.h2tg.rasp.hooks";
    private Set<Class<?>> handlers;

    private HookRegistry() {}

    /**
     * Initialize the singleton instance
     */
    public static void init() {
        if (instance == null) {
            instance = new HookRegistry();
            MicroLogger.info("HookRegistry", "HookRegistry initialized");
        }
    }

    /**
     * Get singleton instance
     */
    public static HookRegistry getInstance() {
        return instance;
    }

    /**
     * Scan hooks package for @HookHandler annotations
     */
    public void scanHooks() {
        try {
            // Use Reflections to scan for @HookHandler annotations in hooks package
            // Note: Must include SubTypesScanner to avoid ReflectionsException
            Reflections reflections = new Reflections(
                    HOOKS_PACKAGE,
                    new TypeAnnotationsScanner(),
                    new SubTypesScanner(false)
            );
            handlers = reflections.getTypesAnnotatedWith(HookHandler.class);

            MicroLogger.info("HookRegistry", "Discovered " + handlers.size() + " hook handler(s) in package " + HOOKS_PACKAGE);

            // Log each discovered handler
            for (Class<?> handler : handlers) {
                MicroLogger.info("HookRegistry", "  - " + handler.getName());
            }
        } catch (Throwable t) {
            MicroLogger.error("HookRegistry", "Failed to scan for hook handlers", t);
            handlers = null;
        }
    }

    /**
     * Apply all discovered hooks to the AgentBuilder
     */
    public AgentBuilder apply(AgentBuilder agentBuilder) {
        if (handlers == null || handlers.isEmpty()) {
            MicroLogger.warn("HookRegistry", "No hook handlers found, agent may not intercept anything");
            return agentBuilder;
        }

        // Register each handler
        for (Class<?> handlerClass : handlers) {
            try {
                agentBuilder = registerHook(agentBuilder, handlerClass);
            } catch (Throwable t) {
                MicroLogger.error("HookRegistry", "Failed to register hook for " + handlerClass.getName(), t);
            }
        }

        return agentBuilder;
    }

    /**
     * Register a single hook handler
     */
    private AgentBuilder registerHook(AgentBuilder agentBuilder, Class<?> handlerClass) {
        HookHandler anno = handlerClass.getAnnotation(HookHandler.class);
        if (anno == null) {
            return agentBuilder;
        }

        final String targetClassName = anno.hookClass();
        final String targetMethod = anno.hookMethod();
        final String[] parameterTypes = anno.parameterTypes();
        final boolean isConstructor = anno.isConstructor();
        final boolean isNative = anno.isNative();

        // Build type matcher
        ElementMatcher.Junction<TypeDescription> typeMatcher = ElementMatchers.named(targetClassName);

        // Build method matcher
        ElementMatcher.Junction<MethodDescription> methodMatcher;
        if (isConstructor) {
            methodMatcher = isConstructor();
        } else {
            methodMatcher = ElementMatchers.named(targetMethod);
        }

        // Handle parameter matching
        if (parameterTypes.length > 0 && !parameterTypes[0].equals("*")) {
            methodMatcher = methodMatcher.and(takesArguments(parameterTypes.length));
            for (int i = 0; i < parameterTypes.length; i++) {
                String paramType = parameterTypes[i];
                methodMatcher = methodMatcher.and(
                        ElementMatchers.takesArgument(i, ElementMatchers.named(paramType))
                );
            }
        }

        // Add native method matcher if needed
        if (isNative) {
            methodMatcher = methodMatcher.and(ElementMatchers.isNative());
        }

        final ElementMatcher.Junction<MethodDescription> finalMethodMatcher = methodMatcher;
        final Class<?> adviceClass = handlerClass;

        // Register with appropriate strategy (native vs non-native)
        if (isNative) {
            // For native methods, use intercept
            agentBuilder = agentBuilder
                    .type(typeMatcher)
                    .transform((builder, typeDesc, classLoader, module, protectionDomain) ->
                            builder.method(finalMethodMatcher).intercept(Advice.to(adviceClass))
                    );
        } else {
            // For non-native methods, use visit
            agentBuilder = agentBuilder
                    .type(typeMatcher)
                    .transform((builder, typeDesc, classLoader, module, protectionDomain) ->
                            builder.visit(Advice.to(adviceClass).on(finalMethodMatcher))
                    );
        }

        // Log registration
        MicroLogger.info("HookRegistry", String.format("Registered hook: target=%s#%s isNative=%s advice=%s",
                targetClassName,
                isConstructor ? "<init>" : targetMethod,
                isNative,
                adviceClass.getName()));

        return agentBuilder;
    }
}
