package org.eclipse.dataspaceconnector.catalog.spi;


import java.util.function.Consumer;

/**
 * Whenever a {@link Crawler} encounters an error, e.g. when a node is not reachable, does not answer within a given amount of time,
 * or the response is invalid, it calls out to the {@code CrawlerErrorHandler} to signal that error.
 */
@FunctionalInterface
public interface CrawlerErrorHandler extends Consumer<WorkItem> {
}
