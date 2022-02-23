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

import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResourceCatalogBuilder;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

/**
 * IdsMultipartSender implementation for connector catalog requests. Sends IDS DescriptionRequestMessages and
 * expects an IDS DescriptionResponseMessage as the response.
 */
public class MultipartCatalogDescriptionRequestSender extends IdsMultipartSender<CatalogRequest, Catalog> {

    public MultipartCatalogDescriptionRequestSender(@NotNull String connectorId,
                                                    @NotNull String senderAgent,
                                                    @NotNull OkHttpClient httpClient,
                                                    @NotNull Serializer serializer,
                                                    @NotNull Monitor monitor,
                                                    @NotNull IdentityService identityService,
                                                    @NotNull TransformerRegistry transformerRegistry) {
        super(connectorId, senderAgent, httpClient, monitor, identityService, transformerRegistry, serializer);
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
                ._issued_(gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getSenderAgentURI())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                .build();
    }

    @Override
    protected Catalog getResponseContent(IdsMultipartParts parts) {
        if (parts.getPayload() == null) {
            throw new EdcException("Payload was null but connector self-description was expected");
        }

        BaseConnector baseConnector = getBaseConnector(getSerializer(), parts);
        if (baseConnector.getResourceCatalog() == null || baseConnector.getResourceCatalog().isEmpty()) {
            throw new EdcException("Resource catalog is null in connector self-description, should not happen");
        }

        ResourceCatalog catalog = baseConnector.getResourceCatalog().stream().findFirst().orElse(new ResourceCatalogBuilder().build());
        Result<Catalog> transformResult = getTransformerRegistry().transform(catalog, Catalog.class);

        if (transformResult.failed()) {
            throw new EdcException(String.format("Could not transform ids data catalog: %s", String.join(", ", transformResult.getFailureMessages())));
        }

        return transformResult.getContent();
    }

    private static BaseConnector getBaseConnector(Serializer serializer, IdsMultipartParts parts) {
        try {
            String payload = Objects.requireNonNull(parts.getPayload());
            return serializer.deserialize(payload, BaseConnector.class);
        } catch (IOException exception) {
            throw new EdcException(String.format("Could not deserialize connector self-description: %s", exception.getMessage()));
        }
    }
}
