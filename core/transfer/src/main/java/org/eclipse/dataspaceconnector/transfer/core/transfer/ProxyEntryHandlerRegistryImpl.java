package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntryHandler;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntryHandlerRegistry;

import java.util.concurrent.ConcurrentHashMap;

public class ProxyEntryHandlerRegistryImpl extends ConcurrentHashMap<String, ProxyEntryHandler> implements ProxyEntryHandlerRegistry {
}
