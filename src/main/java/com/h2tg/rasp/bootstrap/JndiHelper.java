package com.h2tg.rasp.bootstrap;

/**
 * JndiHelper provides utility methods for JNDI security checks.
 * MUST be injected to Bootstrap ClassLoader for cross-classloader access.
 */
public class JndiHelper {
    /**
     * Dangerous class patterns for JNDI object factory
     */
    public static final String[] denyFactories = {
            "org.apache.naming.factory.",
//            "org.apache.naming.factory.BeanFactory",
//            "org.apache.naming.factory.ResourceFactory",
//            "org.apache.naming.factory.LookupFactory",
//            "org.apache.naming.factory.OpenEjbFactory",

            "org.apache.commons.dbcp.BasicDataSourceFactory",
            "org.apache.commons.dbcp2.BasicDataSourceFactory",
            "com.alibaba.druid.pool.DruidDataSourceFactory",
            "com.zaxxer.hikari.HikariJNDIFactory",
            "org.apache.tomcat.dbcp.",
//            "org.apache.tomcat.dbcp.dbcp.BasicDataSourceFactory",
//            "org.apache.tomcat.dbcp.dbcp2.BasicDataSourceFactory",
            "org.apache.tomcat.jdbc.pool.DataSourceFactory",
            "com.mchange.v2.naming.",
//            "com.mchange.v2.naming.JavaBeanObjectFactory",
            "com.mchange.v2.c3p0."
    };


    public static String getFactoryLocation(Class<?> refClass, Object ref) {
        try {
            java.lang.reflect.Method method = refClass.getMethod("getFactoryClassLocation");
            Object location = method.invoke(ref);
            return location != null ? location.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if a factory class name matches any deny pattern.
     * This method MUST be public and static for direct access from Advice methods.
     *
     * @param factoryClassName The factory class name to check
     * @return The matched pattern if dangerous, null otherwise
     */
    public static String checkDenyFactory(String factoryClassName) {
        if (factoryClassName == null || factoryClassName.isEmpty()) {
            return null;
        }

        for (String pattern : denyFactories) {
            if (pattern.endsWith(".")) {
                // Package prefix match (e.g., "org.apache.naming.factory.")
                if (factoryClassName.startsWith(pattern)) {
                    return pattern;
                }
            } else {
                // Exact class match (e.g., "com.alibaba.druid.pool.DruidDataSourceFactory")
                if (factoryClassName.equals(pattern)) {
                    return pattern;
                }
            }
        }

        return null;
    }
}
