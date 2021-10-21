package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.concurrent.TimeUnit;

/**
 * A runnable object that performs a task repeatedly.
 * <p>
 * In the catalog space a {@code Crawler}s job is to go through a list of targets (=nodes) and collect their catalog.
 */
public interface Crawler extends Runnable {
    String FEATURE = "edc:catalog:cache:crawler";

    /**
     * Terminates a crawler and waits the given amount of time until it has completed its current run.
     *
     * @param timeout Amount of time to wait before abandoning the crawler's thread
     * @param unit    Time unit of the wait time
     * @return Whether the crawler finished its run within the given time frame
     */
    boolean join(long timeout, TimeUnit unit);


    /**
     * Terminates a crawler and waits some time until it has completed its current run. The default wait time is 10 seconds.
     *
     * @return Whether the crawler finished its run within the given time frame
     */
    default boolean join() {
        return join(10, TimeUnit.SECONDS);
    }
}
