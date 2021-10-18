package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;

import java.util.Collection;

/**
 * Registry where {@link QueryAdapter} instances are stored and maintained.
 */
public interface QueryAdapterRegistry {
    String FEATURE = "edc:catalog:cache:query:registry";

    /**
     * Finds all instances of {@link QueryAdapter} that was registered.
     *
     * @return The full list of query adapters that have been registered.
     */
    Collection<QueryAdapter> getAllAdapters();

    /**
     * Registers a {@link QueryAdapter} for a given storage type
     */
    void register(QueryAdapter adapter);

    /**
     * Attempts to execute a query by forwarding it to all suitable {@link QueryAdapter} implementations.
     *
     * @param query The {@link CacheQuery}
     * @return a {@link QueryResponse} with {@link QueryResponse#getStatus()} is equal to
     * {@link QueryResponse.Status#ACCEPTED} if there was at least one adapter that could accept the query. If no suitable adapter was found, the status will be
     * {@link QueryResponse.Status#NO_ADAPTER_FOUND}.  The actual result of the query, which could be mixed as some adapters might succeed, others might fail, can be
     * obtained from {@link QueryResponse#getAssets()} and {@link QueryResponse#getErrors()}. The earlier returns an aggregated stream of {@link org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset}, the latter
     * contains a list of errors.
     * <p>
     * For example, when 1 of 5 adapters that receive the query times out, there will be results from 4, errors from 1.
     */
    QueryResponse executeQuery(CacheQuery query);
}
