package org.eclipse.dataspaceconnector.catalog.cache.loader;

import org.eclipse.dataspaceconnector.catalog.cache.spi.Loader;
import org.eclipse.dataspaceconnector.catalog.cache.spi.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.WaitStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class LoaderManagerImplTest {

    private LoaderManagerImpl loaderManager;
    private BlockingQueue<UpdateResponse> queue;
    private Loader loaderMock;
    private WaitStrategy waitStrategyMock;

    @BeforeEach
    void setup() {
        waitStrategyMock = niceMock(WaitStrategy.class);
        int batchSize = 3;
        queue = new ArrayBlockingQueue<>(batchSize); //default batch size of the loader
        loaderMock = strictMock(Loader.class);
        loaderManager = LoaderManagerImpl.Builder.newInstance()
                .queue(queue)
                .batchSize(batchSize)
                .waitStrategy(waitStrategyMock) //only ever wait for 1 millisecond
                .loaders(Collections.singletonList(loaderMock))
                .build();
    }

    @Test
    @DisplayName("Verify that the loader manager waits one pass when the queue does not yet contain sufficient elements")
    void batchSizeNotReachedWithinTimeframe() throws InterruptedException {
        for (var i = 0; i < loaderManager.getBatchSize() - 1; i++) {
            queue.offer(new UpdateResponse());
        }
        var completionSignal = new CountDownLatch(1);

        // set the completion signal when the wait strategy was called
        expect(waitStrategyMock.retryInMillis()).andAnswer(() -> {
            completionSignal.countDown();
            return 10L;
        });
        replay(waitStrategyMock);

        replay(loaderMock);
        loaderManager.start();

        //wait for completion signal
        assertThat(completionSignal.await(20L, TimeUnit.MILLISECONDS)).isTrue();

        verify(loaderMock);
    }


    @Test
    @DisplayName("Verify that the LoaderManager does not sleep when a complete batch was processed")
    void batchSizeReachedWithinTimeframe() throws InterruptedException {
        for (var i = 0; i < loaderManager.getBatchSize(); i++) {
            queue.offer(new UpdateResponse());
        }
        var completionSignal = new CountDownLatch(1);

        // set the completion signal when the wait strategy was called
        waitStrategyMock.success();
        expectLastCall().andAnswer(() -> {
            completionSignal.countDown();
            return null;
        });
        replay(waitStrategyMock);

        loaderMock.load(anyObject());
        expectLastCall().once();
        replay(loaderMock);

        loaderManager.start();

        //wait for completion signal
        assertThat(completionSignal.await(5, TimeUnit.MILLISECONDS)).isTrue();

        verify(loaderMock);
    }

}