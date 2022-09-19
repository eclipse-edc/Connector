package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.function.Predicate;

public interface FederatedCacheNodeFilter extends Predicate<FederatedCacheNode> {
    // marker interface to make it easily injectable
}
