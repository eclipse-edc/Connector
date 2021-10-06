package org.eclipse.dataspaceconnector.catalog.cache.loader;

import org.eclipse.dataspaceconnector.catalog.cache.spi.Loader;
import org.eclipse.dataspaceconnector.catalog.cache.spi.LoaderManager;
import org.eclipse.dataspaceconnector.catalog.cache.spi.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.WaitStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class LoaderManagerImpl implements LoaderManager {
    private static final int DEFAULT_BATCH_SIZE = 5;
    private static final int DEFAULT_WAIT_TIME_MILLIS = 2000;
    private final BlockingQueue<UpdateResponse> queue;
    private final List<Loader> loaders;
    private final AtomicBoolean isRunning;
    private final ReentrantLock lock;
    private final int batchSize;
    private final WaitStrategy waitStrategy;
    private ExecutorService executor;

    private LoaderManagerImpl(BlockingQueue<UpdateResponse> queue, List<Loader> loaders, int batchSize, WaitStrategy waitStrategy) {
        this.queue = queue;
        this.loaders = loaders;
        this.batchSize = batchSize;
        this.waitStrategy = waitStrategy;
        isRunning = new AtomicBoolean(false);
        lock = new ReentrantLock();
    }

    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public void start() {
        isRunning.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::beginDequeue);
    }

    @Override
    public void stop() {
        isRunning.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void beginDequeue() {
        while (isRunning.get()) {
            boolean isBatchFull = true;
            try {
                lock.lock();
                isBatchFull = queue.size() >= batchSize;

                if (isBatchFull) {
                    var batch = new ArrayList<UpdateResponse>(batchSize);
                    // take the elements out of the queue and forward to loaders
                    queue.drainTo(batch, batchSize);
                    loaders.forEach(l -> l.load(batch));
                }
                // else wait and retry on next iteration
            } finally {
                if (!isBatchFull) {
                    try {
                        long l = waitStrategy.retryInMillis();
                        Thread.sleep(l);
                    } catch (InterruptedException e) {
                        isRunning.set(false);
                    }
                } else {
                    waitStrategy.success();
                }
                lock.unlock();
            }
        }
    }


    public static final class Builder {
        private BlockingQueue<UpdateResponse> queue;
        private List<Loader> loaders;
        private int batchSize = DEFAULT_BATCH_SIZE;
        private WaitStrategy waitStrategy = () -> DEFAULT_WAIT_TIME_MILLIS;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }


        public Builder queue(BlockingQueue<UpdateResponse> queue) {
            this.queue = queue;
            return this;
        }

        public Builder loaders(List<Loader> loaders) {
            this.loaders = loaders;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder waitStrategy(WaitStrategy waitStrategy) {
            this.waitStrategy = waitStrategy;
            return this;
        }

        public LoaderManagerImpl build() {
            Objects.requireNonNull(queue);
            Objects.requireNonNull(loaders);
            if (batchSize < 0) {
                throw new IllegalArgumentException("Batch Size cannot be negative!");
            }
            return new LoaderManagerImpl(queue, loaders, batchSize, waitStrategy);
        }
    }
}
