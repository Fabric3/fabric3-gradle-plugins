package org.fabric3.gradle.plugin.core.stopwatch;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A stopwatch that sends timing measurements to an output stream.
 * <p/>
 * Note that this implementation does not close the provided stream; clients are responsible for closing the stream.
 */
public class StreamStopWatch extends AbstractStopWatch {
    private PrintStream stream;

    public StreamStopWatch(String id, TimeUnit unit, PrintStream stream) {
        super(id, unit);
        this.stream = stream;
    }

    public void flush() {
        StringBuilder builder = new StringBuilder();
        if (id != null) {
            builder.append("\nStopwatch (").append(unit.toString()).append("):").append(id).append("\n ");
        } else {
            builder.append("\nStopwatch (").append(unit.toString()).append("): ").append(Thread.currentThread().getName()).append("\n");
        }
        builder.append("Total time: ").append(getTotalTime()).append("\n");
        if (splits != null && !splits.isEmpty()) {

            for (Map.Entry<String[], Long> entry : getSplits().entrySet()) {
                String[] markers = entry.getKey();
                long elapsed = unit.convert(entry.getValue(), TimeUnit.NANOSECONDS);
                if (markers.length == 1) {
                    builder.append("  ").append(markers[0]).append(":").append(elapsed).append("\n");
                } else {
                    builder.append("  ");
                    for (String marker : markers) {
                        builder.append(marker).append(" ");
                    }
                    builder.append(":").append(elapsed);
                }

            }
        }
        stream.println(builder.toString());
    }

}
