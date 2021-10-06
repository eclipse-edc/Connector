package org.eclipse.dataspaceconnector.catalog.cache.spi;

import java.util.Collection;

public interface Loader {
    void load(Collection<UpdateResponse> batch);
}
