package org.eclipse.dataspaceconnector.catalog.cache;

public class TestWorkQueue extends InMemoryWorkItemQueue {
    public TestWorkQueue(int cap) {
        super(cap);
    }
}
