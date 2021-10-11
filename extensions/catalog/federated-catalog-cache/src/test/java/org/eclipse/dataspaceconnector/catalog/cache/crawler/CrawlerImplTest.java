package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.TestProtocolAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.cache.TestWorkItem;
import org.eclipse.dataspaceconnector.catalog.cache.TestWorkQueue;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class CrawlerImplTest {
    public static final int QUEUE_CAPACITY = 3;
    private CrawlerImpl crawler;
    private ProtocolAdapter protocolAdapterMock;
    private ArrayBlockingQueue<UpdateResponse> queue;
    private Monitor monitorMock;
    private WorkItemQueue workQueue;
    private TestProtocolAdapterRegistry registry;

    @BeforeEach
    void setUp() {
        protocolAdapterMock = strictMock(ProtocolAdapter.class);
        queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        monitorMock = niceMock(Monitor.class);
        workQueue = new TestWorkQueue(1);
        registry = new TestProtocolAdapterRegistry(protocolAdapterMock);
        crawler = new CrawlerImpl(workQueue, monitorMock, queue, createRetryPolicy(), registry, Duration.ofMillis(500));
    }

    @Test
    @DisplayName("Should insert one item into queue when request succeeds")
    void shouldInsertInQueue_whenSucceeds() throws InterruptedException {
        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.completedFuture(new UpdateResponse()));
        replay(protocolAdapterMock);

        workQueue.put(new TestWorkItem());
        Executors.newFixedThreadPool(1).submit(crawler);
        crawler.join().whenComplete((unused, throwable) -> {
            assertThat(throwable).isNull();
            assertThat(queue).hasSize(1);
            verify(protocolAdapterMock);
        });
    }

    @Test
    @DisplayName("Should not insert into queue when the request fails")
    void shouldNotInsertInQueue_whenRequestFails() throws InterruptedException {


        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.failedFuture(new EdcException("")));
        replay(protocolAdapterMock);

        monitorMock.severe(anyString());
        expectLastCall().once();
        replay(monitorMock);

        workQueue.put(new TestWorkItem());
        Executors.newFixedThreadPool(1).submit(crawler);
        crawler.join().whenComplete((unused, throwable) -> {
            assertThat(throwable).isNull();
            assertThat(queue).isEmpty();
            verify(protocolAdapterMock);
            verify(monitorMock);
        });
    }

    @Test
    @DisplayName("Should insert only those items into queue that have succeeded")
    void shouldInsertInQueue_onlySuccessfulProtocolRequests() throws InterruptedException {

        ProtocolAdapter secondAdapter = strictMock(ProtocolAdapter.class);
        registry.register("test-protocol", secondAdapter);

        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.completedFuture(new UpdateResponse()));
        replay(protocolAdapterMock);
        expect(secondAdapter.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.failedFuture(new RuntimeException()));
        replay(secondAdapter);

        monitorMock.severe(anyString());
        expectLastCall().once();
        replay(monitorMock);

        workQueue.put(new TestWorkItem());
        Executors.newFixedThreadPool(1).submit(crawler);
        var l = new CountDownLatch(1);
        crawler.join().whenComplete((unused, throwable) -> {
            assertThat(throwable).isNull();
            assertThat(queue).hasSize(1);
            verify(protocolAdapterMock);
            verify(secondAdapter);
            verify(monitorMock);
            l.countDown();
        });
        assertThat(l.await(10, TimeUnit.SECONDS)).withFailMessage("Crawler did not complete in time").isTrue();
    }

    @Test
    @DisplayName("Should not insert when Queue is at capacity")
    void shouldLogError_whenQueueFull() throws InterruptedException {
        queue.add(new UpdateResponse());
        queue.add(new UpdateResponse());
        queue.add(new UpdateResponse()); //queue is full now

        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.supplyAsync(UpdateResponse::new));
        replay(protocolAdapterMock);

        monitorMock.severe(anyString());
        expectLastCall().once();
        replay(monitorMock);

        workQueue.put(new TestWorkItem());
        Executors.newFixedThreadPool(1).submit(crawler);
        crawler.join().whenComplete((unused, throwable) -> {
            assertThat(throwable).isNull();
            assertThat(queue).hasSize(3);
            verify(protocolAdapterMock);
            verify(monitorMock);
        });


    }

    @Test
    void shouldPauseWhenNoWorkItem() throws InterruptedException {
        replay(protocolAdapterMock);

        var latch = new CountDownLatch(1);
        Executors.newSingleThreadExecutor().submit(crawler);

        assertThat(latch.await(500, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(queue).hasSize(0);

        verify(protocolAdapterMock);

    }

    @Test
    void shouldErrorOut_whenNoProtocolAdapterFound() throws InterruptedException {
        replay(protocolAdapterMock);

        workQueue.put(new TestWorkItem());

        Executors.newFixedThreadPool(1).submit(crawler);
        crawler.join().whenComplete((unused, throwable) -> {
            assertThat(throwable).isNull();
            assertThat(workQueue).hasSize(1).allSatisfy(wi -> assertThat(wi.getErrors()).isNotNull().hasSize(1));
            verify(protocolAdapterMock);
        });
    }

    private RetryPolicy<Object> createRetryPolicy() {
        return new RetryPolicy<>().withMaxRetries(1);
    }

    private static class AnotherProtocolAdapter implements ProtocolAdapter {

        @Override
        public CompletableFuture<UpdateResponse> sendRequest(UpdateRequest request) {
            return null;
        }
    }

}