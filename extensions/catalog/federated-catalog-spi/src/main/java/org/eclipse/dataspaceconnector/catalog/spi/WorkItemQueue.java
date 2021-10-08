package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.concurrent.BlockingQueue;

public interface WorkItemQueue extends BlockingQueue<WorkItem> {
}
