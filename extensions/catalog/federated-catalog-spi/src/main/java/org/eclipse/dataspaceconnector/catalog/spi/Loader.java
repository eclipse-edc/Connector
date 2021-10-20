package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.Collection;

/**
 * Puts the result of a catalog request (i.e. {@link org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse} into
 * whatever storage backend or database is used.
 */
@FunctionalInterface
public interface Loader {
    void load(Collection<UpdateResponse> batch);
}
