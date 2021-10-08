package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;

public class TestWorkItem implements WorkItem {
    private final Class<? extends ProtocolAdapter> protocolAdapterType;
    private String error;

    public TestWorkItem(Class<? extends ProtocolAdapter> protocolAdapterType) {
        this.protocolAdapterType = protocolAdapterType;
    }

    @Override
    public <T extends ProtocolAdapter> Class<T> getProtocolType() {
        return (Class<T>) protocolAdapterType;
    }

    @Override
    public String getUrl() {
        return "test-url";
    }

    @Override
    public void error(String message) {
        error = message;
    }

    public String getError() {
        return error;
    }
}
