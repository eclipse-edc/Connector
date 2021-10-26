/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.catalog;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessageBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.ids.spi.descriptor.IdsDescriptorService;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataStore;

import java.util.Map;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/ids")
public class DescriptionRequestController {
    private final IdsDescriptorService descriptorService;
    private final MetadataStore metadataStore;

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
