package com.h2tg.rasp;

import com.h2tg.rasp.core.HookListener;
import com.h2tg.rasp.core.HookRegistry;
import com.h2tg.rasp.log.MicroLogger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Java Agent entrypoint for MicroRASP.
 * Supports both premain (startup) and agentmain (dynamic attach) modes.
 */
public class Agent {

    /**
     * Premain method called when agent is loaded at JVM startup
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        install(agentArgs, inst);
    }

    /**
     * Agentmain method called when agent is dynamically attached
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        install(agentArgs, inst);
    }

    /**
     * Install the RASP agent
     */
    private static void install(String agentArgs, Instrumentation inst) {
        MicroLogger.info("AgentInstall", "========================================");
        MicroLogger.info("AgentInstall", "MicroRASP Agent Starting...");
        MicroLogger.info("AgentInstall", "========================================");

        try {
            // Step 1: Inject bootstrap classes to Bootstrap ClassLoader
            // This is critical for cross-classloader access to shared utilities
            injectBootstrapClasses(inst, "com.h2tg.rasp.bootstrap.RequestContext", "com.h2tg.rasp.bootstrap.SerialHelper", "com.h2tg.rasp.bootstrap.JndiHelper");


            // Step 2: Initialize HookRegistry and scan for hooks
            HookRegistry.init();
            HookRegistry hookRegistry = HookRegistry.getInstance();
            hookRegistry.scanHooks();

            // Step 3: Build the agent with ByteBuddy configuration
            AgentBuilder agentBuilder = buildAgentBuilder();

            // Step 4: Configure injection strategy for Bootstrap ClassLoader
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            AgentBuilder.InjectionStrategy injection = new AgentBuilder.InjectionStrategy.UsingInstrumentation(inst, tempDir);
            agentBuilder = agentBuilder.with(injection);

            // Step 5: Apply all discovered hooks
            agentBuilder = hookRegistry.apply(agentBuilder);

            // Step 6: Install the agent
            agentBuilder.installOn(inst);

            MicroLogger.info("AgentInstall", "========================================");
            MicroLogger.info("AgentInstall", "MicroRASP Agent Installed Successfully");
            MicroLogger.info("AgentInstall", "========================================");
        } catch (Throwable t) {
            MicroLogger.error("AgentInstall", "Failed to install MicroRASP agent", t);
        }
    }

    /**
     * Inject specified bootstrap classes to Bootstrap ClassLoader.
     * This ensures cross-classloader access to shared utilities and contexts.
     *
     * @param inst Instrumentation instance
     * @param classNames Fully qualified class names to inject (e.g., "com.h2tg.rasp.bootstrap.RequestContext")
     */
    private static void injectBootstrapClasses(Instrumentation inst, String... classNames) {
        if (classNames == null || classNames.length == 0) {
            MicroLogger.warn("BootstrapInject", "No classes specified for bootstrap injection, skipping");
            return;
        }

        try {
            MicroLogger.info("BootstrapInject", "Injecting " + classNames.length + " class(es) to Bootstrap ClassLoader...");

            // Create a temporary JAR to hold all bootstrap classes
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            File tempJar = File.createTempFile("rasp-bootstrap-", ".jar", tempDir);
            tempJar.deleteOnExit();

            // Write all specified classes to the JAR
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar))) {
                for (String className : classNames) {
                    // Convert class name to resource path (e.g., "com.foo.Bar" -> "com/foo/Bar.class")
                    String resourcePath = className.replace('.', '/') + ".class";
                    InputStream is = Agent.class.getClassLoader().getResourceAsStream(resourcePath);

                    if (is == null) {
                        MicroLogger.warn("BootstrapInject", "  - Cannot find " + className + " in classpath, skipping");
                        continue;
                    }

                    try {
                        // Create JAR entry
                        JarEntry entry = new JarEntry(resourcePath);
                        jos.putNextEntry(entry);

                        // Copy class bytes
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            jos.write(buffer, 0, bytesRead);
                        }

                        jos.closeEntry();
                        MicroLogger.info("BootstrapInject", "  - Added " + className);
                    } finally {
                        is.close();
                    }
                }
            }

            // Inject the JAR to Bootstrap ClassLoader
            inst.appendToBootstrapClassLoaderSearch(new JarFile(tempJar));

            MicroLogger.info("BootstrapInject", "Bootstrap classes injected successfully");
            MicroLogger.info("BootstrapInject", "Temporary JAR: " + tempJar.getAbsolutePath());
        } catch (Exception e) {
            MicroLogger.error("BootstrapInject", "Failed to inject bootstrap classes", e);
            throw new RuntimeException("Failed to inject bootstrap classes to Bootstrap ClassLoader", e);
        }
    }

    /**
     * Build the AgentBuilder with proper configuration
     */
    private static AgentBuilder buildAgentBuilder() {
        // Disable type validation to allow instrumentation of JDK classes
        ByteBuddy byteBuddy = new ByteBuddy().with(TypeValidation.DISABLED);

        // Build ignore matcher to avoid instrumenting our own code and dependencies
        ElementMatcher.Junction<net.bytebuddy.description.type.TypeDescription> ignoreMatcher =
                nameStartsWith("net.bytebuddy.")
                .or(nameStartsWith("org.reflections."))
                .or(nameStartsWith("org.javassist."))
                .or(nameStartsWith("com.h2tg.rasp."))
                .or(nameStartsWith("java.util.logging."))
                .or(ElementMatchers.isSynthetic());

        // Create agent builder
        AgentBuilder builder = new AgentBuilder.Default(byteBuddy)
                // Enable native method prefix for native method instrumentation
                .enableNativeMethodPrefix("rasp_")
                // Ignore specified packages
                .ignore(ignoreMatcher)
                // Use retransformation strategy for already loaded classes
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                // No-op initialization strategy for minimal intrusion
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                // Add transformation listener
                .with(new AgentBuilder.Listener.WithTransformationsOnly(new HookListener()));

        MicroLogger.info("AgentBuilder", "Agent builder configured successfully");
        return builder;
    }
}
