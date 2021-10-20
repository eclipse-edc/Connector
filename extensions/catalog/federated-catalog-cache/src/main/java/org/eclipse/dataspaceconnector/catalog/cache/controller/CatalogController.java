package org.eclipse.dataspaceconnector.catalog.cache.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.catalog.cache.query.QueryException;
import org.eclipse.dataspaceconnector.catalog.cache.query.QueryNotAcceptedException;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collection;
import java.util.stream.Collectors;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/")
public class CatalogController {

    private final Monitor monitor;
    private final QueryEngine queryEngine;

    public CatalogController(Monitor monitor, QueryEngine queryEngine) {
        this.monitor = monitor;
        this.queryEngine = queryEngine;
    }

    @POST
    @Path("catalog")
    public Collection<Asset> getCatalog(CacheQuery cacheQuery) {
        monitor.info("Received a catalog request");
        QueryResponse queryResponse = queryEngine.getCatalog(cacheQuery);
        // query not possible
        if (queryResponse.getStatus() == QueryResponse.Status.NO_ADAPTER_FOUND) {
            throw new QueryNotAcceptedException();
        }
        if (!queryResponse.getErrors().isEmpty()) {
            throw new QueryException(queryResponse.getErrors());
        }

        return queryResponse.getAssets().collect(Collectors.toList());
    }
}
