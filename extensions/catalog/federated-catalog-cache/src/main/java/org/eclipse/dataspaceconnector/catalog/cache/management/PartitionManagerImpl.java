package org.eclipse.dataspaceconnector.catalog.cache.management;

import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.PartitionManager;
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
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PartitionManagerImpl implements PartitionManager {
    private final Queue<ExecutionPlan> scheduledUpdates;
    private final Monitor monitor;
    private final Function<WorkItemQueue, Crawler> crawlerGenerator;
    private final int numCrawlers;
    private List<Crawler> crawlers;
    private WorkItemQueue workload;

    /**
     * Instantiates a new PartitionManagerImpl.
     *
     * @param monitor          A {@link Monitor}
     * @param crawlerGenerator A generator function that MUST create a new instance of a {@link Crawler}
     * @param numCrawlers      A number indicating how many {@code Crawler} instances should be generated. Note that the PartitionManager may choose to
     *                         create less Crawlers, depending on available CPU cores or other system resources.
     */
    public PartitionManagerImpl(Monitor monitor, Function<WorkItemQueue, Crawler> crawlerGenerator, int numCrawlers) {
        this.monitor = monitor;
        this.crawlerGenerator = crawlerGenerator;
        this.numCrawlers = numCrawlers;
        scheduledUpdates = new ConcurrentLinkedQueue<>();
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
        //todo: do we discard updates?


        waitForCrawlersAndDo((unused, throwable) -> {
            // create "numCrawlers" crawlers using the generator function
            crawlers = IntStream.rangeClosed(0, numCrawlers).mapToObj(i -> crawlerGenerator.apply(workload)).collect(Collectors.toList());
            scheduleCrawlers(crawlers, executionPlan);
        });
    }

    @Override
    public CompletableFuture<Void> waitForCompletion() {
        var completable = new CompletableFuture<Void>();
        waitForCrawlersAndDo((unused, throwable) -> completable.complete(null));
        return completable;
    }

    private @NotNull ExecutionPlan collateUpdates(Collection<ExecutionPlan> scheduledUpdates) {
        return scheduledUpdates.stream().reduce(ExecutionPlan::merge).orElseThrow();
    }

    private void waitForCrawlersAndDo(BiConsumer<Void, Throwable> consumer) {
        if (crawlers == null || crawlers.isEmpty()) {
            consumer.accept(null, null);
            return;
        }
        var collect = crawlers.stream().map(Crawler::waitForCompletion).collect(Collectors.toList());
        CompletableFuture.allOf(collect.toArray(CompletableFuture[]::new))
                .whenComplete(consumer);
    }

    private void scheduleCrawlers(List<Crawler> crawlers, ExecutionPlan executionPlan) {
        var scheduler = Executors.newScheduledThreadPool(crawlers.size());

        for (Crawler crawler : crawlers) {
            var schedule = executionPlan.getSchedule();
            var future = scheduler.schedule(crawler, schedule.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
