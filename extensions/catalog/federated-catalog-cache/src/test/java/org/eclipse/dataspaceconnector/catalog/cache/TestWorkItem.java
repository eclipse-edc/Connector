package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;

public class TestWorkItem extends WorkItem {
    public TestWorkItem() {
        super("test-url", "test-protocol");
    }
}
