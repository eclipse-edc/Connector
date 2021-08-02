/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.api.catalog;

import com.microsoft.dagx.ids.spi.descriptor.IdsDescriptorService;
import com.microsoft.dagx.spi.metadata.MetadataStore;
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
    private MetadataStore metadataStore;

    public DescriptionRequestController(IdsDescriptorService descriptorService, MetadataStore metadataStore) {
        this.descriptorService = descriptorService;
        this.metadataStore = metadataStore;
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
