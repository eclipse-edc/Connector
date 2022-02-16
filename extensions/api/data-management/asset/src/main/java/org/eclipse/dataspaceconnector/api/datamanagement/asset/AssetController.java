package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/assets")
public class AssetController {

    private final Monitor monitor;

    public AssetController(Monitor monitor) {
        this.monitor = monitor;
    }

    @POST
    @Path("")
    public void createAsset(AssetEntryDto assetEntryDto){
        monitor.debug(format("Asset created %s", assetEntryDto.getAssetDto()));
    }

    @GET
    @Path("")
    public List<AssetDto> getAllAssets(@QueryParam("offset") Integer offset,
                                       @QueryParam("limit") Integer limit,
                                       @QueryParam("filter") String filterExpression,
                                       @QueryParam("sort") SortOrder sortOrder,
                                       @QueryParam("sortField") String sortField){

        var spec = QuerySpec.Builder.newInstance()
                .offset(offset)
                .limit(limit)
                .sortField(sortField)
                .filter(filterExpression)
                .sortOrder(sortOrder).build();

        monitor.debug(format("get all Assets from %s", spec));

        return Collections.emptyList();
    }

    @GET
    @Path("{id}")
    public AssetDto getAsset(@PathParam("id") String id){

        monitor.debug(format("Attempting to return Asset with id %s", id));
        return null;
    }

    @DELETE
    @Path("{id}")
    public void removeAsset(@PathParam("id") String id){

        monitor.debug(format("Attempting to delete Asset with id %s", id));
    }
}
