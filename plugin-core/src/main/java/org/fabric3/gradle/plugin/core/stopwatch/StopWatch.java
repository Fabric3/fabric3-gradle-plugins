package org.fabric3.gradle.plugin.core.stopwatch;

import java.util.Map;

/**
 * A stopwatch that measures elapsed time.
 *
 * Note implementations are not thread-safe.
 */
public interface StopWatch {

    /**
     * Starts recording elapsed time.
     */
    void start();

    /**
     * Records a split elapsed time.
     *
     * @param markers the markers to associate with the split, generally one or more identifiers that will be concatenated when {@link #flush()} is called to
     *                output timings.
     */
    void split(String... markers);

    /**
     * Stops recording elapsed time.
     */
    void stop();

    /**
     * Returns the total elapsed time after {@link #stop()} has been called.
     *
     * @return the total elapsed time after {@link #stop()} has been called
     */
    long getTotalTime();

    /**
     * Returns the map of splits with the key the markers and the value elapsed time.
     *
     * @return the map of splits with the key the markers and the value elapsed time
     */
    Map<String[], Long> getSplits();

    /**
     * Outputs timings to a destination such as the console or log file.
     */
    void flush();
}
