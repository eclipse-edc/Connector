package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;

import java.util.concurrent.ArrayBlockingQueue;

public class TestWorkQueue extends ArrayBlockingQueue<WorkItem> implements WorkItemQueue {
    public TestWorkQueue(int cap) {
        super(cap);
    }
}
