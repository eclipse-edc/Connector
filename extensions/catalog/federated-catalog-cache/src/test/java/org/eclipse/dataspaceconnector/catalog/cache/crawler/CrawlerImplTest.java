package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.TestProtocolAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.cache.TestWorkItem;
import org.eclipse.dataspaceconnector.catalog.cache.TestWorkQueue;
import org.eclipse.dataspaceconnector.catalog.spi.CrawlerErrorHandler;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class CrawlerImplTest {
    public static final int QUEUE_CAPACITY = 3;
    public static final int JOIN_WAIT_TIME = 100;
    public static final int WORK_QUEUE_POLL_TIMEOUT = 500;
    private CrawlerImpl crawler;
    private ProtocolAdapter protocolAdapterMock;
    private ArrayBlockingQueue<UpdateResponse> queue;
    private Monitor monitorMock;
    private WorkItemQueue workQueue;
    private TestProtocolAdapterRegistry registry;
    private CrawlerErrorHandler errorHandlerMock;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newSingleThreadExecutor();
        errorHandlerMock = mock(CrawlerErrorHandler.class);
        protocolAdapterMock = strictMock(ProtocolAdapter.class);
        queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        monitorMock = niceMock(Monitor.class);
        workQueue = new TestWorkQueue(10);
        registry = new TestProtocolAdapterRegistry(protocolAdapterMock);
        crawler = new CrawlerImpl(workQueue, monitorMock, queue, createRetryPolicy(), registry, () -> Duration.ofMillis(WORK_QUEUE_POLL_TIMEOUT), errorHandlerMock);
    }

    @AfterEach
    void teardown() {
        executorService.shutdown();
    }

    @Test
    @DisplayName("Should insert one item into queue when request succeeds")
    void shouldInsertInQueue_whenSucceeds() throws InterruptedException {
        var l = new CountDownLatch(1);
        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class)))
                .andAnswer(() -> {
                    l.countDown();
                    return CompletableFuture.completedFuture(new UpdateResponse());
                });
        replay(protocolAdapterMock);

        workQueue.put(new TestWorkItem());
        executorService.submit(crawler);
        assertThat(l.await(JOIN_WAIT_TIME, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(1);
    }

    @Test
    @DisplayName("Should not insert into queue when the request fails")
    void shouldNotInsertInQueue_whenRequestFails() throws InterruptedException {

        var l = new CountDownLatch(1);

        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).andAnswer(() -> {
            l.countDown();
            return CompletableFuture.failedFuture(new EdcException("not reachable"));
        });
        replay(protocolAdapterMock);

        monitorMock.severe(anyString());
        expectLastCall().once();
        replay(monitorMock);


        workQueue.put(new TestWorkItem());
        executorService.submit(crawler);

        assertThat(l.await(JOIN_WAIT_TIME, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(crawler.join()).isTrue();
        assertThat(queue).isEmpty();
        verify(protocolAdapterMock, monitorMock);
    }

    @Test
    @DisplayName("Should insert only those items into queue that have succeeded")
    void shouldInsertInQueue_onlySuccessfulProtocolRequests() throws InterruptedException {

        var l = new CountDownLatch(2);
        ProtocolAdapter secondAdapter = strictMock(ProtocolAdapter.class);
        registry.register("test-protocol", secondAdapter);

        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).andAnswer(() -> {
            l.countDown();
            return CompletableFuture.completedFuture(new UpdateResponse());
        });
        replay(protocolAdapterMock);

        expect(secondAdapter.sendRequest(isA(UpdateRequest.class))).andAnswer(() -> {
            l.countDown();
            return CompletableFuture.failedFuture(new RuntimeException());
        });
        replay(secondAdapter);

        monitorMock.severe(anyString());
        expectLastCall().once();
        replay(monitorMock);

        workQueue.put(new TestWorkItem());
        executorService.submit(crawler);

        assertThat(l.await(JOIN_WAIT_TIME, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(1);
        verify(protocolAdapterMock, secondAdapter, monitorMock);
    }

    @Test
    @DisplayName("Should not insert when Queue is at capacity")
    void shouldLogError_whenQueueFull() throws InterruptedException {
        queue.add(new UpdateResponse());
        queue.add(new UpdateResponse());
        queue.add(new UpdateResponse()); //queue is full now

        var l = new CountDownLatch(1);
        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).andAnswer(() -> {
            l.countDown();
            return CompletableFuture.completedFuture(new UpdateResponse());
        });
        replay(protocolAdapterMock);

        monitorMock.severe(anyString());
        expectLastCall().atLeastOnce();
        replay(monitorMock);


        workQueue.put(new TestWorkItem());
        executorService.submit(crawler);

        assertThat(l.await(JOIN_WAIT_TIME, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(3);
        verify(protocolAdapterMock, monitorMock);
    }

    @Test
    void shouldPauseWhenNoWorkItem() throws InterruptedException {
        replay(protocolAdapterMock);

        executorService.submit(crawler);

        // wait until the queue has likely been polled at least once
        Thread.sleep(WORK_QUEUE_POLL_TIMEOUT);
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(0);
        verify(protocolAdapterMock);
    }

    @Test
    void shouldErrorOut_whenNoProtocolAdapterFound() throws InterruptedException {

        crawler = new CrawlerImpl(workQueue, monitorMock, queue, createRetryPolicy(), new TestProtocolAdapterRegistry(), () -> Duration.ofMillis(500), errorHandlerMock);

        workQueue.put(new TestWorkItem());
        var l = new CountDownLatch(1);
        errorHandlerMock.accept(isA(WorkItem.class));
        expectLastCall().andAnswer(() -> {
            l.countDown();
            return null;
        }).once();
        replay(errorHandlerMock, protocolAdapterMock);


        executorService.submit(crawler);

        assertThat(l.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(workQueue).hasSize(0); //1).allSatisfy(wi -> assertThat(wi.getErrors()).isNotNull().hasSize(1));
        verify(protocolAdapterMock, errorHandlerMock);

    }

    private RetryPolicy<Object> createRetryPolicy() {
        return new RetryPolicy<>().withMaxRetries(1);
    }

}