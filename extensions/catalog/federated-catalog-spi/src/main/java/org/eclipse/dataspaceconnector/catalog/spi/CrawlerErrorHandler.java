package org.eclipse.dataspaceconnector.catalog.spi;


import java.util.function.Consumer;

/**
 * Whenever a {@link Crawler} encounters an error, e.g. when a node is not reachable, a node does not respond within a given time frame,
 * or the response is invalid, it calls out to the {@code CrawlerErrorHandler} to signal that error.
 */
@FunctionalInterface
public interface CrawlerErrorHandler extends Consumer<WorkItem> {
}
