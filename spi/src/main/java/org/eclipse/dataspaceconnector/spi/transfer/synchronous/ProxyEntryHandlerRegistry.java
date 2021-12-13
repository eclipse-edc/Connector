package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import java.util.Map;

/**
 * Registry that contains {@link ProxyEntryHandler} instances. Those are used in a synchronous data transfer on the client side
 * to process a {@link ProxyEntry}.
 */
public interface ProxyEntryHandlerRegistry extends Map<String, ProxyEntryHandler> {
    String FEATURE = "edc:transfer:sync:proxyhandlerregistry";

    // marker interface that we need for type registration with the ServiceExtensionContext
}
