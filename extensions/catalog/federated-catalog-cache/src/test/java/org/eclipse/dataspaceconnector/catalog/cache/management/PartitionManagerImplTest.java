package org.eclipse.dataspaceconnector.catalog.cache.management;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.TestProtocolAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.cache.TestWorkItem;
import org.eclipse.dataspaceconnector.catalog.cache.TestWorkQueue;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CrawlerImpl;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.RecurringExecutionPlan;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;

class PartitionManagerImplTest {
    public static final int WORK_ITEM_COUNT = 1000;
    private final Monitor monitorMock = niceMock(Monitor.class);
    private PartitionManagerImpl partitionManager;
    private WorkItemQueue workItemQueue;
    private List<WorkItem> staticWorkLoad;
    private Function<WorkItemQueue, Crawler> generatorFunction;
    private CountDownLatch latch;

    @BeforeEach
    void setup() {
        latch = new CountDownLatch(WORK_ITEM_COUNT);
        workItemQueue = new SignalingWorkItemQueue(WORK_ITEM_COUNT + 1, latch);
        staticWorkLoad = IntStream.range(0, WORK_ITEM_COUNT).mapToObj(i -> new TestWorkItem()).collect(Collectors.toList());

        ProtocolAdapter adapterMock = niceMock(ProtocolAdapter.class);
        expect(adapterMock.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.completedFuture(new UpdateResponse())).anyTimes();
        replay(adapterMock);

        TestProtocolAdapterRegistry registry = new TestProtocolAdapterRegistry(adapterMock);

        generatorFunction = workItemQueue -> CrawlerImpl.Builder.newInstance()
                .retryPolicy(new RetryPolicy<>())
                .monitor(monitorMock)
                .workQueuePollTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .workItems(workItemQueue)
                .protocolAdapters(registry)
                .queue(new ArrayBlockingQueue<>(10))
                .build();

    }

    @Test
    void waitForCompletion() throws InterruptedException {

        partitionManager = new PartitionManagerImpl(monitorMock, workItemQueue, generatorFunction, 1, staticWorkLoad);
        partitionManager.schedule(new RecurringExecutionPlan(Duration.of(100, ChronoUnit.MILLIS))); // start working right away
        assertThat(latch.await(10, TimeUnit.SECONDS)).withFailMessage("latch was expected to be 0 but was: " + latch.getCount()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 10, 50 })
    void runManyCrawlers_verifyCompletion(int crawlerCount) throws InterruptedException {
        partitionManager = new PartitionManagerImpl(monitorMock, workItemQueue, generatorFunction, crawlerCount, staticWorkLoad);
        partitionManager.schedule(new RecurringExecutionPlan(Duration.of(100, ChronoUnit.MILLIS)));
        assertThat(latch.await(10, TimeUnit.SECONDS)).withFailMessage("latch was expected to be 0 but was: " + latch.getCount()).isTrue();
    }

    private static class SignalingWorkItemQueue extends TestWorkQueue {
        private final CountDownLatch latch;

        public SignalingWorkItemQueue(int cap, CountDownLatch latch) {
            super(cap);
            this.latch = latch;
        }

        @Override
        public WorkItem poll(long timeout, TimeUnit unit) throws InterruptedException {

            var polledItem = super.poll(timeout, unit);
            if (polledItem != null) {
                latch.countDown();
            }
            return polledItem;
        }
    }
}