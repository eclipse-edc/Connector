package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;

public class TestUtil {

    public static WorkItem createWorkItem() {
        return new WorkItem("test-url", "test-protocol");
    }
}
