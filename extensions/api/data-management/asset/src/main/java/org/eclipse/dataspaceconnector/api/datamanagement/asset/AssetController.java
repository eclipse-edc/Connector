package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/assets")
public class AssetController {

    private RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry;
    private Monitor monitor;

    public AssetController(@NotNull RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry, Monitor monitor) {
        this.remoteMessageDispatcherRegistry = Objects.requireNonNull(remoteMessageDispatcherRegistry, "remoteMessageDispatcherRegistry");
        this.monitor = monitor;
    }

    @POST
    @Path("")
    public Response createAsset(AssetEntryDto assetEntryDto){
        return Response.ok(assetEntryDto.getAsset()).build();
    }

    @GET
    @Path("")
    public List<AssetDto> getAllAssets(){
        //Creating a dummy list of 4 objects of type AssetEntryDto and

        var spec = QuerySpec.Builder.newInstance()
                .offset(offset)
                .limit(limit)
                .sortField(sortField)
                .filter(filterExpression)
                .sortOrder(sortOrder).build();
        monitor.debug(format("get all contract definitions %s", spec));

        monitor.info("Returning the list of Assets");

        return Collections.emptyList();
    }

    @GET
    @Path("{id}")
    public AssetDto getAsset(@PathParam("id") String id){

        return null;
    }

    @DELETE
    @Path("{id}")
    public Response removeAsset(@PathParam("id") String id){

        //assuming dummy local database with just 1 id with name "asset"
        try{
            if(id.equals("asset")){
                monitor.debug("The asset with the id " + id + " was removed from the list");
            }
        }catch (Exception IllegatlStateException){
            monitor.severe("The asset with the ID " +id+ "does not exist");
        }
        return null;
    }
}
