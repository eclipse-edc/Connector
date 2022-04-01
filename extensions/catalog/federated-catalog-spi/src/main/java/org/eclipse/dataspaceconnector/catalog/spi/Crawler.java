/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.spi.system.Feature;

import java.util.concurrent.TimeUnit;

/**
 * A runnable object that performs a query task repeatedly. Crawlers are used to inquire other Federated Cache Nodes' fetch
 * their Asset list.
 * This interface is not limited to performing catalog queries of course, although currently it is its sole use case.
 * <p>
 * In other words: the {@code Crawler}s job is to go through a list of targets (=nodes) and collect their catalog.
 * <p>
 * Crawlers should not throw exceptions when a crawl process failed, rather the should use the {@link CrawlerErrorHandler} interface!
 */
@Feature(Crawler.FEATURE)
public interface Crawler extends Runnable {
    String FEATURE = "edc:catalog:cache:crawler";

    /**
     * Signals a crawler to terminate and waits the given amount of time until it has completed its current run.
     *
     * @param timeout Amount of time to wait before abandoning the crawler's thread
     * @param unit    Time unit of the wait time
     * @return Whether the crawler finished its run within the given time frame
     */
    boolean join(long timeout, TimeUnit unit);


    /**
     * Signals a crawler to terminate and waits some time until it has completed its current run. The default wait time is 10 seconds.
     *
     * @return Whether the crawler finished its run within the given time frame
     */
    default boolean join() {
        return join(10, TimeUnit.SECONDS);
    }
}
