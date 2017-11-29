package org.fabric3.gradle.plugin.core.util;

import org.gradle.logging.ProgressLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ProgressLoggerCompat {
    private ProgressLoggerCompat() {
    }

    @SuppressWarnings("UnusedReturnValue")
    public static ProgressLogger setDescription(ProgressLogger progressLogger, String description) {
        try {
            invokeMethodWithStringArg(progressLogger, "setDescription", description);
        } catch (Exception e) {
            throw new IllegalStateException("Could not call setDescription(String) on ProgressLogger", e);
        }

        return progressLogger;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static ProgressLogger setLoggingHeader(ProgressLogger progressLogger, String header) {
        try {
            invokeMethodWithStringArg(progressLogger, "setLoggingHeader", header);
        } catch (Exception e) {
            throw new IllegalStateException("Could call setLoggingHeader(String) on ProgressLogger", e);
        }

        return progressLogger;
    }

    private static void invokeMethodWithStringArg(ProgressLogger progressLogger, String methodName, String argument) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = progressLogger.getClass().getMethod(methodName, String.class);
        method.setAccessible(true);
        method.invoke(progressLogger, argument);
    }
}
