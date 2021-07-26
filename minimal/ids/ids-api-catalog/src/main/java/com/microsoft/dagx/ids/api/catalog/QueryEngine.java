package com.microsoft.dagx.ids.api.catalog;

import com.microsoft.dagx.spi.iam.ClaimToken;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;

import java.util.Collection;

/**
 * Executes client queries.
 */
public interface QueryEngine {

    /**
     * Executes a query. Implementations must treat the query as originating from an untrusted source.
     */
    Collection<DataEntry> execute(String correlationId, ClaimToken clientToken, String connectorId, String type, String query);

}
