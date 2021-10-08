package org.eclipse.dataspaceconnector.catalog.cache.management;

import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;

class PartitionManagerImplTest {
    private final Monitor monitorMock = mock(Monitor.class);
    private PartitionManagerImpl partitionManager;

    @BeforeEach
    void setup() {
    }

    @Test
    void waitForCompletion() throws InterruptedException {
        Crawler crawlerMock = niceMock(Crawler.class);
        var crawlerCompletionDelay = CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS);
        expect(crawlerMock.waitForCompletion()).andReturn(CompletableFuture.supplyAsync(() -> null, crawlerCompletionDelay)).anyTimes();
        replay(crawlerMock);

        var latch = new CountDownLatch(1);

        partitionManager = new PartitionManagerImpl(monitorMock, workItems -> crawlerMock, 2);
        partitionManager.schedule(new ExecutionPlan(Duration.of(10, ChronoUnit.MILLIS)));

        partitionManager.waitForCompletion().whenComplete((unused, throwable) -> {
            assertThat(throwable).isNull();
            latch.countDown();
        });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = { 100, 1000, 10000 })
    void runManyCrawlers_verifyCompletion(int crawlerCount) throws InterruptedException {

        var latch = new CountDownLatch(crawlerCount);
        partitionManager = new PartitionManagerImpl(monitorMock, workItems -> new DelayedCrawler(latch), crawlerCount);
        partitionManager.schedule(new ExecutionPlan(Duration.of(10, ChronoUnit.MILLIS)));
        Thread.sleep(1000);
        partitionManager.waitForCompletion().whenComplete((unused, throwable) -> {
            assertThat(throwable).isNull();
            latch.countDown();
            System.out.println("all crawlers complete. latch: " + latch.getCount());
        });
        assertThat(latch.await(10, TimeUnit.SECONDS)).withFailMessage("latch was expected to be 0 but was: " + latch.getCount()).isTrue();
    }

    private static class DelayedCrawler implements Crawler {
        private static int instanceCount = 0;
        private final ReentrantLock lock = new ReentrantLock();
        private final CountDownLatch latch;
        private final int count;


        public DelayedCrawler(CountDownLatch latch) {
            this.latch = latch;
            count = instanceCount++;
        }

        @Override
        public void addAdapter(ProtocolAdapter adapter) {

        }

        @Override
        public CompletableFuture<Void> waitForCompletion() {

            return CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("crawler " + count + ": waiting for lock to be release");
                    lock.tryLock(10, TimeUnit.SECONDS);
                    System.out.println("crawler " + count + ": lock released. crawler complete!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            });
        }

        @Override
        public void run() {
            try {
                System.out.println("crawler " + count + ": locking...");
                lock.lock();

                var sleep = 500 + new Random().nextInt(2000);
                System.out.println("crawler " + count + " is running (ETA " + sleep + " ms)");
                Thread.sleep(sleep);
                System.out.println("crawler " + count + " has finished");
                latch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("crawler " + count + ": unlocking...");
                lock.unlock();
            }
        }
    }
}