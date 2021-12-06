/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.Result;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * IdsMultipartSender implementation for connector catalog requests. Sends IDS DescriptionRequestMessages and
 * expects an IDS DescriptionResponseMessage as the response.
 */
public class MultipartCatalogDescriptionRequestSender extends IdsMultipartSender<CatalogRequest, Catalog> {

    public MultipartCatalogDescriptionRequestSender(@NotNull String connectorId,
                                                    @NotNull OkHttpClient httpClient,
                                                    @NotNull ObjectMapper objectMapper,
                                                    @NotNull Monitor monitor,
                                                    @NotNull IdentityService identityService,
                                                    @NotNull TransformerRegistry transformerRegistry) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);
    }

    @Override
    public Class<CatalogRequest> messageType() {
        return CatalogRequest.class;
    }

    @Override
    protected String retrieveRemoteConnectorId(CatalogRequest request) {
        return request.getConnectorId();
    }

    @Override
    protected String retrieveRemoteConnectorAddress(CatalogRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(CatalogRequest request, DynamicAttributeToken token) {
        return new DescriptionRequestMessageBuilder()
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                .build();
    }

    @Override
    protected Catalog getResponseContent(IdsMultipartParts parts) throws Exception {
        ObjectMapper objectMapper = getObjectMapper();

        if (parts.getPayload() == null) {
            throw new EdcException("Payload was null but connector self-description was expected");
        }

        BaseConnector baseConnector;
        try {
            baseConnector = objectMapper.readValue(parts.getPayload().readAllBytes(), BaseConnector.class);
        } catch (IOException exception) {
            throw new EdcException(String.format("Could not deserialize connector self-description: %s", exception.getMessage()));
        }

        ResourceCatalog resourceCatalog = null;
        if (baseConnector.getResourceCatalog() != null) {
            var iterator = baseConnector.getResourceCatalog().iterator();
            if (iterator.hasNext()) {
                resourceCatalog = iterator.next();
            }

            if (resourceCatalog != null) {
                if (resourceCatalog.getOfferedResource() == null || resourceCatalog.getOfferedResource().isEmpty()) {
                    for (Map.Entry<String, Object> entry : resourceCatalog.getProperties().entrySet()) {
                        if ("ids:offeredResource".equals(entry.getKey())) {
                            JsonNode node = objectMapper.convertValue(entry.getValue(), JsonNode.class);
                            List<Resource> offeredResources = new LinkedList<>();
                            for (JsonNode objNode : node.get("objectList")) {
                                Map<String, Object> resource = new HashMap<>();
                                resource.put("@type", "ids:Resource");
                                resource.putAll(objectMapper.convertValue(objNode, Map.class));
                                offeredResources.add(objectMapper.convertValue(resource, Resource.class));
                            }
                            resourceCatalog.setOfferedResource(offeredResources);
                        }
                    }
                }
            }
        }

        Result<Catalog> transformResult = getTransformerRegistry().transform(resourceCatalog, Catalog.class);

        if (transformResult.failed()) {
            throw new EdcException(String.format("Could not transform ids data catalog: %s", String.join(", ", transformResult.getFailures())));
        }

        return transformResult.getContent();
    }
}
