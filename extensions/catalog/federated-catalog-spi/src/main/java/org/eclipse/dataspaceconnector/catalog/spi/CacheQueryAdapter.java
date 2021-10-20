package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Adapter to translate a {@link CacheQuery} into whatever query language the underlying data store uses.
 */
public interface CacheQueryAdapter {
    /**
     * Executes the query.
     *
     * @return A stream of {@link Asset} objects. Can be empty, can never be null.
     * @throws IllegalArgumentException may be thrown if the implementor cannot translate the query.
     */
    @NotNull Stream<Asset> executeQuery(CacheQuery query);

    /**
     * Checks whether a given query can be run by the implementor. This does not limit itself to whether the query
     * is actually translatable, this could even go as far as perform semantic checks on the query.
     *
     * @param query The Query
     * @return true if the query can be run, false otherwise.
     */
    boolean canExecute(CacheQuery query);

}
