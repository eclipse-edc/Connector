package org.eclipse.dataspaceconnector.catalog.cache.management;

import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.PartitionManager;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PartitionManagerImpl implements PartitionManager {
    private final Queue<ExecutionPlan> scheduledUpdates;
    private final Monitor monitor;
    private final Function<WorkItemQueue, Crawler> crawlerGenerator;
    private final List<Crawler> crawlers;
    private final WorkItemQueue workQueue;
    private final List<WorkItem> staticWorkLoad;

    /**
     * Instantiates a new PartitionManagerImpl.
     *
     * @param workQueue        An implementation of a blocking {@link WorkItemQueue}
     * @param monitor          A {@link Monitor}
     * @param crawlerGenerator A generator function that MUST create a new instance of a {@link Crawler}
     * @param numCrawlers      A number indicating how many {@code Crawler} instances should be generated. Note that the PartitionManager may choose to
     * @param staticWorkLoad   A list of {@link WorkItem} instances that need to be processed on every scheduled run.
     */
    public PartitionManagerImpl(Monitor monitor, WorkItemQueue workQueue, Function<WorkItemQueue, Crawler> crawlerGenerator, int numCrawlers, List<WorkItem> staticWorkLoad) {
        this.monitor = monitor;
        this.staticWorkLoad = staticWorkLoad;
        this.workQueue = workQueue;
        this.crawlerGenerator = crawlerGenerator;
        scheduledUpdates = new ConcurrentLinkedQueue<>();

        // create "numCrawlers" crawlers using the generator function
        crawlers = createCrawlers(numCrawlers);
        // crawlers will start running as soon as the workQueue gets populated
        startCrawlers(crawlers);
    }

    @Override
    public void update(ExecutionPlan newPlan) {
        if (!scheduledUpdates.offer(newPlan)) {
            monitor.severe("PartitionManager Update was not scheduled!");
        }

        waitForCrawlersAndDo((unused, throwable) -> {
            var collatedPlan = collateUpdates(scheduledUpdates);
            schedule(collatedPlan);
        });
    }

    @Override
    public void schedule(ExecutionPlan executionPlan) {
        //todo: should we really discard updates?
        executionPlan.run(() -> workQueue.addAll(staticWorkLoad));
    }

    @Override
    public CompletableFuture<Void> stop() {
        var completable = new CompletableFuture<Void>();
        waitForCrawlersAndDo((unused, throwable) -> completable.complete(null));
        return completable;
    }

    private List<Crawler> createCrawlers(int numCrawlers) {
        return IntStream.range(0, numCrawlers).mapToObj(i -> crawlerGenerator.apply(workQueue)).collect(Collectors.toList());
    }

    private @NotNull ExecutionPlan collateUpdates(Collection<ExecutionPlan> scheduledUpdates) {
        return scheduledUpdates.stream().reduce(ExecutionPlan::merge).orElseThrow();
    }

    private void waitForCrawlersAndDo(BiConsumer<Void, Throwable> consumer) {
        if (crawlers == null || crawlers.isEmpty()) {
            consumer.accept(null, null);
            return;
        }
        var allStopSignals = crawlers.stream().map(Crawler::join).collect(Collectors.toList());
        CompletableFuture.allOf(allStopSignals.toArray(CompletableFuture[]::new))
                .whenComplete(consumer);
    }

    private void startCrawlers(List<Crawler> crawlers) {
        var scheduler = Executors.newFixedThreadPool(crawlers.size());
        crawlers.forEach(scheduler::submit);
    }
}
