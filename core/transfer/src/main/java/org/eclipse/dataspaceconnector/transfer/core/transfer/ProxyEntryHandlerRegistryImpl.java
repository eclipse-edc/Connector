package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.proxy.ProxyEntryHandler;
import org.eclipse.dataspaceconnector.spi.proxy.ProxyEntryHandlerRegistry;

import java.util.concurrent.ConcurrentHashMap;

public class ProxyEntryHandlerRegistryImpl extends ConcurrentHashMap<String, ProxyEntryHandler> implements ProxyEntryHandlerRegistry {
}
