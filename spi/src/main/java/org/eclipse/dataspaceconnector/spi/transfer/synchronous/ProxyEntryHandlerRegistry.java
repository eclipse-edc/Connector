package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import java.util.Map;

public interface ProxyEntryHandlerRegistry extends Map<String, ProxyEntryHandler> {
    String FEATURE = "edc:transfer:sync:proxyhandlerregistry";

    // marker interface that we need for type registration with the ServiceExtensionContext
}
