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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RequestInProcessMessage;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartRequestInProcessResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.spec.extension.ArtifactRequestMessagePayload;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.ids.spi.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * IdsMultipartSender implementation for data requests. Sends IDS ArtifactRequestMessages and
 * expects an IDS RequestInProcessMessage as the response.
 */
public class MultipartArtifactRequestSender extends IdsMultipartSender<DataRequest, MultipartRequestInProcessResponse> {

    private final Vault vault;
    private final String idsWebhookAddress;
    private final String idsApiPath;

    public MultipartArtifactRequestSender(@NotNull String connectorId,
                                          @NotNull OkHttpClient httpClient,
                                          @NotNull ObjectMapper objectMapper,
                                          @NotNull Monitor monitor,
                                          @NotNull Vault vault,
                                          @NotNull IdentityService identityService,
                                          @NotNull TransformerRegistry transformerRegistry,
                                          @NotNull String idsWebhookAddress,
                                          @NotNull String idsApiPath) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);
        this.vault = Objects.requireNonNull(vault);
        this.idsWebhookAddress = idsWebhookAddress;
        this.idsApiPath = idsApiPath;
    }

    @Override
    public Class<DataRequest> messageType() {
        return DataRequest.class;
    }

    @Override
    protected String retrieveRemoteConnectorAddress(DataRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(DataRequest request, DynamicAttributeToken token) {
        var artifactIdsId = IdsId.Builder.newInstance()
                .value(request.getAssetId())
                .type(IdsType.ARTIFACT)
                .build();
        var contractIdsId = IdsId.Builder.newInstance()
                .value(request.getContractId())
                .type(IdsType.CONTRACT)
                .build();
        var artifactTransformationResult = getTransformerRegistry().transform(artifactIdsId, URI.class);
        if (artifactTransformationResult.failed()) {
            throw new EdcException("Failed to create artifact ID from asset.");
        }

        var contractTransformationResult = getTransformerRegistry().transform(contractIdsId, URI.class);
        if (contractTransformationResult.failed()) {
            throw new EdcException("Failed to create contract ID from asset.");
        }

        var artifactId = artifactTransformationResult.getContent();
        var contractId = contractTransformationResult.getContent();

        var artifactRequestId = request.getId() != null ? request.getId() : UUID.randomUUID().toString();
        var message = new ArtifactRequestMessageBuilder(URI.create(artifactRequestId))
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._requestedArtifact_(artifactId)
                ._transferContract_(contractId)
                .build();
        
        var path = idsApiPath + (idsApiPath.endsWith("/") ? "data" : "/data");
        message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, idsWebhookAddress + path);
        
        request.getProperties().forEach(message::setProperty);
        return message;
    }

    @Override
    protected String buildMessagePayload(DataRequest request) throws Exception {

        ArtifactRequestMessagePayload.Builder requestPayloadBuilder = ArtifactRequestMessagePayload.Builder.newInstance()
                .dataDestination(request.getDataDestination());

        if (request.getDataDestination().getKeyName() != null) {
            String secret = vault.resolveSecret(request.getDataDestination().getKeyName());
            requestPayloadBuilder = requestPayloadBuilder.secret(secret);
        }

        ObjectMapper objectMapper = getObjectMapper();
        return objectMapper.writeValueAsString(requestPayloadBuilder.build());
    }

    @Override
    protected MultipartRequestInProcessResponse getResponseContent(IdsMultipartParts parts) throws Exception {
        Message header = getObjectMapper().readValue(parts.getHeader(), Message.class);
        String payload = null;
        if (parts.getPayload() != null) {
            payload = new String(parts.getPayload().readAllBytes());
        }

        if (header instanceof RequestInProcessMessage) {
            // TODO Update TransferProcess State Machine
        } else {
            // TODO Update TransferProcess State Machine
        }

        return MultipartRequestInProcessResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }
}
