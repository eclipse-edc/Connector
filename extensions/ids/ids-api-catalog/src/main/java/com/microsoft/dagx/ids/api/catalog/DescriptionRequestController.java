package com.microsoft.dagx.ids.api.catalog;

import com.microsoft.dagx.ids.spi.catalog.CatalogService;
import com.microsoft.dagx.ids.spi.descriptor.IdsDescriptorService;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessageBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/ids")
public class DescriptionRequestController {
    private IdsDescriptorService descriptorService;
    private CatalogService catalogService;

    public DescriptionRequestController(IdsDescriptorService descriptorService, CatalogService catalogService) {
        this.descriptorService = descriptorService;
        this.catalogService = catalogService;
    }

    @POST
    @Path("description")
    public DescriptionResponseMessage descriptionRequest(DescriptionRequestMessage request) {
        DescriptionResponseMessage message = new DescriptionResponseMessageBuilder().build();
        for (Map.Entry<String, Object> entry : descriptorService.description().entrySet()) {
            message.setProperty(entry.getKey(), entry.getValue());
        }
        return message;
    }
}
