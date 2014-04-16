package org.fabric3.gradle.plugin.core.stopwatch;

import java.util.Map;

/**
 * A no-op stopwatch intended for use in production systems or when performance metrics are turned off. Method calls should be optimized away by the JIT.
 */
public class NoOpStopWatch implements StopWatch {

    public void start() {

    }

    public void split(String... marker) {

    }

    public void stop() {

    }

    public long getTotalTime() {
        return 0;
    }

    public Map<String[], Long> getSplits() {
        return null;
    }

    public void flush() {

    }
}
