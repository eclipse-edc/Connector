package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public interface WorkItemQueue extends BlockingQueue<WorkItem> {
    void lock();

    void unlock();

    boolean tryLock(long timeout, TimeUnit unit);
}
