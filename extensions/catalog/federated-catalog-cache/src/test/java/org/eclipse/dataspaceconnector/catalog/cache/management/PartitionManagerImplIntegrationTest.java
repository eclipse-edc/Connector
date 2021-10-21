package org.eclipse.dataspaceconnector.catalog.cache.management;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.TestProtocolAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.cache.TestWorkItem;
import org.eclipse.dataspaceconnector.catalog.cache.TestWorkQueue;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CrawlerImpl;
import org.eclipse.dataspaceconnector.catalog.spi.CatalogQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;

/**
 * This tests the PartitionManagerImpl with real crawlers and in a real multithreading environment.
 * It uses several dummy classes which are private static classes.
 */
class PartitionManagerImplIntegrationTest {
    public static final int WORK_ITEM_COUNT = 1000;
    private final Monitor monitorMock = niceMock(Monitor.class);
    private WorkItemQueue signallingWorkItemQueue;
    private List<WorkItem> staticWorkLoad;
    private Function<WorkItemQueue, Crawler> generatorFunction;
    private CountDownLatch latch;
    private WorkQueueListener queueListener;

    @BeforeEach
    void setup() {

        latch = new CountDownLatch(WORK_ITEM_COUNT);
        queueListener = niceMock(WorkQueueListener.class);
        signallingWorkItemQueue = new SignalingWorkItemQueue(WORK_ITEM_COUNT + 1, queueListener);
        staticWorkLoad = IntStream.range(0, WORK_ITEM_COUNT).mapToObj(i -> new TestWorkItem()).collect(Collectors.toList());

        CatalogQueryAdapter adapterMock = niceMock(CatalogQueryAdapter.class);
        expect(adapterMock.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.completedFuture(new UpdateResponse())).times(WORK_ITEM_COUNT);
        replay(adapterMock);

        TestProtocolAdapterRegistry registry = new TestProtocolAdapterRegistry(adapterMock);

        BlockingQueue<UpdateResponse> loaderQueueMock = niceMock(BlockingQueue.class);
        generatorFunction = workItemQueue -> CrawlerImpl.Builder.newInstance()
                .retryPolicy(new RetryPolicy<>())
                .monitor(monitorMock)
                .workQueuePollTimeout(() -> Duration.of(1, ChronoUnit.MILLIS)) //basically don't wait during polling the queue
                .workItems(workItemQueue)
                .protocolAdapters(registry)
                .errorReceiver(workItem -> {
                })
                .queue(loaderQueueMock)
                .build();

    }

    @ParameterizedTest
    @ValueSource(ints = { 10, 50, 500 })
    @DisplayName("Verify that " + WORK_ITEM_COUNT + " work items are correctly processed by a number of crawlers")
    void runManyCrawlers_verifyCompletion(int crawlerCount) throws InterruptedException {

        queueListener.unlocked();
        expectLastCall().andAnswer(() -> {
            latch.countDown();
            return null;
        }).anyTimes();
        replay(queueListener);
        var partitionManager = new PartitionManagerImpl(monitorMock, signallingWorkItemQueue, generatorFunction, crawlerCount, staticWorkLoad);
        partitionManager.schedule(new RunOnceExecutionPlan());
        assertThat(latch.await(1, TimeUnit.MINUTES)).withFailMessage("latch was expected to be 0 but was: " + latch.getCount()).isTrue();

    }

    /**
     * listens for events on the {@link SignalingWorkItemQueue}
     */
    private interface WorkQueueListener {
        default void locked() {
        }

        default void unlocked() {
        }

        default void tryLock() {
        }

        default void polled(WorkItem polledItem) {
        }
    }

    /**
     * A test work item queue that informs a registered listener whenever an
     * event like poll() or unlock() occurs.
     * The recommended pattern is to supply {@code mock(WorkQueueListener.class)}
     */
    private static class SignalingWorkItemQueue extends TestWorkQueue {
        private final WorkQueueListener listener;

        public SignalingWorkItemQueue(int cap, WorkQueueListener listener) {
            super(cap);
            this.listener = listener;
        }

        @Override
        public void lock() {
            listener.locked();
            super.lock();
        }

        @Override
        public void unlock() {
            listener.unlocked();
            super.unlock();
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) {
            listener.tryLock();
            return super.tryLock(timeout, unit);
        }

        @Override
        public WorkItem poll(long timeout, TimeUnit unit) throws InterruptedException {
            var polledItem = super.poll(timeout, unit);
            listener.polled(polledItem);
            return polledItem;
        }
    }

    /**
     * An ExecutionPlan that runs a given {@link Runnable} right away and without recurrence
     */
    private static class RunOnceExecutionPlan implements ExecutionPlan {

        public RunOnceExecutionPlan() {
        }

        @Override
        public ExecutionPlan merge(ExecutionPlan other) {
            return other;
        }

        @Override
        public void run(Runnable task) {
            Executors.newSingleThreadScheduledExecutor().submit(task);
        }
    }
}