package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.ArrayList;
import java.util.List;

public class WorkItem {
    private final String url;
    private final String protocolName;
    private final List<String> errors;

    public WorkItem(String url, String protocolName) {
        this.url = url;
        this.protocolName = protocolName;
        errors = new ArrayList<>();
    }

    public String getProtocol() {
        return protocolName;
    }

    public String getUrl() {
        return url;
    }

    public void error(String message) {
        errors.add(message);
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "WorkItem{" +
                "url='" + url + '\'' +
                ", protocolName='" + protocolName + '\'' +
                ", errors=" + errors +
                '}';
    }
}
