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

package org.eclipse.dataspaceconnector.catalog.cache.management;

import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.PartitionManager;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PartitionManagerImpl implements PartitionManager {
    private final Monitor monitor;
    private final Function<WorkItemQueue, Crawler> crawlerGenerator;
    private final List<Crawler> crawlers;
    private final WorkItemQueue workQueue;
    private final Supplier<List<WorkItem>> workloadSource;
    private ExecutorService crawlerScheduler;

    /**
     * Instantiates a new PartitionManagerImpl.
     *
     * @param monitor          A {@link Monitor}
     * @param workQueue        An implementation of a blocking {@link WorkItemQueue}
     * @param crawlerGenerator A generator function that MUST create a new instance of a {@link Crawler}
     * @param numCrawlers      A number indicating how many {@code Crawler} instances should be generated.
     *                         Note that the PartitionManager may choose to generate more or less, e.g. because of constrained system resources.
     * @param workloadSource   A fixed list of {@link WorkItem} instances that need to be processed on every execution run. This list is treated as immutable,
     */
    public PartitionManagerImpl(Monitor monitor, WorkItemQueue workQueue, Function<WorkItemQueue, Crawler> crawlerGenerator, int numCrawlers, Supplier<List<WorkItem>> workloadSource) {
        this.monitor = monitor;
        this.workloadSource = workloadSource;
        this.workQueue = workQueue;
        this.crawlerGenerator = crawlerGenerator;

        // create "numCrawlers" crawlers using the generator function
        crawlers = createCrawlers(numCrawlers);

        // crawlers will start running as soon as the workQueue gets populated
        startCrawlers(crawlers);
    }


    @Override
    public void schedule(ExecutionPlan executionPlan) {
        //todo: should we really discard updates?
        var currentList = workloadSource.get();
        executionPlan.run(() -> {
            monitor.debug("Partition manager: execute plan - waiting for queue lock");
            workQueue.lock();
            monitor.debug("Partition manager: execute plan - adding workload " + currentList.size());
            workQueue.addAll(currentList);
            monitor.debug("Partition manager: execute release queue lock");
            workQueue.unlock();
        });
    }

    @Override
    public void stop() {
        waitForCrawlers();
    }

    private List<Crawler> createCrawlers(int numCrawlers) {
        return IntStream.range(0, numCrawlers).mapToObj(i -> crawlerGenerator.apply(workQueue)).collect(Collectors.toList());
    }

    private void waitForCrawlers() {
        if (crawlers == null || crawlers.isEmpty()) {
            return;
        }
        crawlers.forEach(Crawler::join);
    }

    private void startCrawlers(List<Crawler> crawlers) {
        if (crawlerScheduler != null) {
            crawlerScheduler.shutdownNow();
        }

        crawlerScheduler = Executors.newScheduledThreadPool(crawlers.size());
        crawlers.forEach(crawlerScheduler::submit);
    }
}
