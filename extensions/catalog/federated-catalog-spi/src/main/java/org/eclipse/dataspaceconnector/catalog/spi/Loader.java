package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.Collection;

public interface Loader {
    void load(Collection<UpdateResponse> batch);
}
