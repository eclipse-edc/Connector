package org.eclipse.edc.ids.api.catalog;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.types.domain.metadata.DataEntry;

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
