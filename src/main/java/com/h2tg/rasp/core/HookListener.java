package com.h2tg.rasp.core;

import com.h2tg.rasp.log.MicroLogger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

/**
 * Listener for Byte Buddy agent transformations.
 * Logs transformation events and errors.
 */
public class HookListener extends AgentBuilder.Listener.Adapter {

    @Override
    public void onTransformation(TypeDescription typeDescription,
                                 ClassLoader classLoader,
                                 JavaModule module,
                                 boolean loaded,
                                 DynamicType dynamicType) {
        MicroLogger.info("ByteBuddy", String.format("TRANSFORM %s [loaded=%s, classLoader=%s]",
                typeDescription.getName(),
                loaded,
                classLoader != null ? classLoader.getClass().getName() : "Bootstrap"));
    }

    @Override
    public void onError(String typeName,
                        ClassLoader classLoader,
                        JavaModule module,
                        boolean loaded,
                        Throwable throwable) {
        MicroLogger.error("ByteBuddy", String.format("ERROR transforming %s [loaded=%s]",
                typeName,
                loaded), throwable);
    }

    @Override
    public void onIgnored(TypeDescription typeDescription,
                          ClassLoader classLoader,
                          JavaModule module,
                          boolean loaded) {
        // Uncomment for debugging
        // RaspLog.info("ByteBuddy", String.format("IGNORED %s", typeDescription.getName()));
    }
}
