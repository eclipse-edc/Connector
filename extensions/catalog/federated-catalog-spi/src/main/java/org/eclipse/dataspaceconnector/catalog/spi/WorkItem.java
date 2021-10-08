package org.eclipse.dataspaceconnector.catalog.spi;

public interface WorkItem {
    <T extends ProtocolAdapter> Class<T> getProtocolType();

    String getUrl();

    void error(String message);
}
