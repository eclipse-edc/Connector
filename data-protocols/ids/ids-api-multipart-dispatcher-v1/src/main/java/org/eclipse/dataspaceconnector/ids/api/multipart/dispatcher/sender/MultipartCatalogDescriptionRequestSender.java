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
import de.fraunhofer.iais.eis.ResourceCatalogBuilder;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * IdsMultipartSender implementation for connector catalog requests. Sends IDS DescriptionRequestMessages and expects an
 * IDS DescriptionResponseMessage as the response.
 */
public class MultipartCatalogDescriptionRequestSender extends IdsMultipartSender<CatalogRequest, Catalog> {

    public MultipartCatalogDescriptionRequestSender(@NotNull String connectorId,
                                                    @NotNull OkHttpClient httpClient,
                                                    @NotNull ObjectMapper objectMapper,
                                                    @NotNull Monitor monitor,
                                                    @NotNull IdentityService identityService,
                                                    @NotNull IdsTransformerRegistry transformerRegistry) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);
    }

    @Override
    public Class<CatalogRequest> messageType() {
        return CatalogRequest.class;
    }

    @Override
    protected String retrieveRemoteConnectorAddress(CatalogRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(CatalogRequest request, DynamicAttributeToken token) {
        var message = new DescriptionRequestMessageBuilder()
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                .build();
        //TODO: IDS REFACTORING: incorporate this into the protocol itself
        message.setProperty(Range.FROM, request.getRange().getFrom());
        message.setProperty(Range.TO, request.getRange().getTo());
        return message;
    }

    @Override
    protected Catalog getResponseContent(IdsMultipartParts parts) {
        if (parts.getPayload() == null) {
            throw new EdcException("Payload was null but connector self-description was expected");
        }

        var baseConnector = getBaseConnector(getObjectMapper(), parts);
        if (baseConnector.getResourceCatalog() == null || baseConnector.getResourceCatalog().isEmpty()) {
            throw new EdcException("Resource catalog is null in connector self-description, should not happen");
        }

        // If there is no resource catalog in connector self-description, we initialize a new empty resource catalog.
        var resourceCatalog = baseConnector.getResourceCatalog().stream()
                .findFirst()
                .orElse(new ResourceCatalogBuilder().build());

        if (catalogDoesNotContainAnyOfferResource(resourceCatalog)) {
            createOfferResourcesFromProperties(resourceCatalog, getObjectMapper());
        }

        var transformResult = getTransformerRegistry().transform(resourceCatalog, Catalog.class);

        if (transformResult.failed()) {
            throw new EdcException(String.format("Could not transform ids data catalog: %s", String.join(", ", transformResult.getFailureMessages())));
        }

        return transformResult.getContent();
    }

    private BaseConnector getBaseConnector(ObjectMapper mapper, IdsMultipartParts parts) {
        try {
            InputStream payload = Objects.requireNonNull(parts.getPayload());
            return mapper.readValue(payload.readAllBytes(), BaseConnector.class);
        } catch (IOException exception) {
            throw new EdcException(String.format("Could not deserialize connector self-description: %s", exception.getMessage()));
        }
    }

    private void createOfferResourcesFromProperties(ResourceCatalog catalog, ObjectMapper mapper) {
        if (catalog.getProperties() != null) {
            for (Map.Entry<String, Object> entry : catalog.getProperties().entrySet()) {
                if ("ids:offeredResource".equals(entry.getKey())) {
                    JsonNode node = mapper.convertValue(entry.getValue(), JsonNode.class);
                    List<Resource> offeredResources = new LinkedList<>();
                    for (JsonNode objNode : node.get("objectList")) {
                        Map<String, Object> resource = new HashMap<>();
                        resource.put("@type", "ids:Resource");
                        resource.putAll(mapper.convertValue(objNode, Map.class));
                        offeredResources.add(mapper.convertValue(resource, Resource.class));
                    }
                    catalog.setOfferedResource(offeredResources);
                }
            }
        }
    }

    private boolean catalogDoesNotContainAnyOfferResource(ResourceCatalog catalog) {
        return catalog.getOfferedResource() == null || catalog.getOfferedResource().isEmpty();
    }
}
