package org.eclipse.dataspaceconnector.catalog.spi;


import java.util.function.Consumer;

@FunctionalInterface
public interface CrawlerErrorHandler extends Consumer<WorkItem> {
}
