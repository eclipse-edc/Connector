package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.ExtensionPoint;

import java.util.function.Predicate;

@ExtensionPoint
public interface FederatedCacheNodeFilter extends Predicate<FederatedCacheNode> {
    // marker interface to make it easily injectable
}
