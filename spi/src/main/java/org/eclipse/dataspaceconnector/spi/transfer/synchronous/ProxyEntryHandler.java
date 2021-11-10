package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import java.util.function.Function;

@FunctionalInterface
public interface ProxyEntryHandler extends Function<ProxyEntry, Object> {
}
