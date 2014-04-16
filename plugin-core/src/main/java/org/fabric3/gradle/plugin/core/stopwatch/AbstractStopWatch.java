package org.fabric3.gradle.plugin.core.stopwatch;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base stopwatch functionality.
 */
public abstract class AbstractStopWatch implements StopWatch {
    protected String id;
    protected TimeUnit unit;
    protected long start;
    protected Map<String[], Long> splits;
    protected long end;

    public AbstractStopWatch(String id, TimeUnit unit) {
        this.id = id;
        this.unit = unit;
    }

    public void start() {
        start = System.nanoTime();
    }

    public void split(String... markers) {
        if (markers == null) {
            throw new IllegalArgumentException("A marker must be specified");
        }
        long now = System.nanoTime();
        if (splits == null) {
            splits = new LinkedHashMap<>();
        }
        splits.put(markers, now);
    }

    public void stop() {
        end = System.nanoTime();
    }

    public long getTotalTime() {
        return unit.convert(end - start, TimeUnit.NANOSECONDS);
    }

    public Map<String[], Long> getSplits() {
        Map<String[], Long> calculated = new LinkedHashMap<String[], Long>(splits.size());
        for (Map.Entry<String[], Long> entry : splits.entrySet()) {
            calculated.put(entry.getKey(), entry.getValue() - start);
        }
        return calculated;
    }

}
