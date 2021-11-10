package org.eclipse.dataspaceconnector.transfer.httproxy;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntry;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntryHandler;

public class RestProxyEntryHandler implements ProxyEntryHandler {
    private final Monitor monitor;

    public RestProxyEntryHandler(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Object apply(ProxyEntry restProxyEntry) {
        monitor.info("RestProxyEntryHandler accepted entry");
        monitor.info(String.format("Will issue a GET request to %s ", restProxyEntry.getProperties().get("url")));
        return restProxyEntry;
    }

}
