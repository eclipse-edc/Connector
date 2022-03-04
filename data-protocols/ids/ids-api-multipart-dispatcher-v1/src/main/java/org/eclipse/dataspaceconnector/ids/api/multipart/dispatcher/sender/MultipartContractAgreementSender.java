/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
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
import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartMessageProcessedResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;

import static org.eclipse.dataspaceconnector.ids.spi.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * IdsMultipartSender implementation for contract agreements. Sends IDS ContractAgreementMessages and
 * expects an IDS RequestInProcessMessage as the response.
 */
public class MultipartContractAgreementSender extends IdsMultipartSender<ContractAgreementRequest, MultipartMessageProcessedResponse> {

    private final String idsWebhookAddress;
    private final TransformerRegistry transformerRegistry;

    public MultipartContractAgreementSender(@NotNull String connectorId,
                                            @NotNull OkHttpClient httpClient,
                                            @NotNull ObjectMapper objectMapper,
                                            @NotNull Monitor monitor,
                                            @NotNull IdentityService identityService,
                                            @NotNull TransformerRegistry transformerRegistry,
                                            @NotNull String idsWebhookAddress) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);

        this.transformerRegistry = transformerRegistry;
        this.idsWebhookAddress = idsWebhookAddress;
    }

    @Override
    public Class<ContractAgreementRequest> messageType() {
        return ContractAgreementRequest.class;
    }

    @Override
    protected String retrieveRemoteConnectorAddress(ContractAgreementRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(ContractAgreementRequest request, DynamicAttributeToken token) throws Exception {
        var id = request.getContractAgreement().getId();
        var idsId = IdsId.Builder.newInstance().type(IdsType.CONTRACT_AGREEMENT).value(id).build();
        var idUriResult = transformerRegistry.transform(idsId, URI.class);
        if (idUriResult.failed()) {
            throw new EdcException("Cannot convert contract agreement id to URI");
        }

        var message = new ContractAgreementMessageBuilder(idUriResult.getContent())
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._transferContract_(URI.create(request.getCorrelationId()))
                .build();
        message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, idsWebhookAddress + "/api/v1/ids/data");

        return message;
    }

    @Override
    protected String buildMessagePayload(ContractAgreementRequest request) throws Exception {
        var contractAgreement = request.getContractAgreement();
        var transformationResult = getTransformerRegistry().transform(contractAgreement, ContractAgreement.class);
        if (transformationResult.failed()) {
            throw new EdcException("Failed to create IDS contract agreement");
        }

        var idsContractAgreement = transformationResult.getContent();
        return getObjectMapper().writeValueAsString(idsContractAgreement);
    }

    @Override
    protected MultipartMessageProcessedResponse getResponseContent(IdsMultipartParts parts) throws Exception {
        Message header = getObjectMapper().readValue(parts.getHeader(), Message.class);
        String payload = null;
        if (parts.getPayload() != null) {
            payload = new String(parts.getPayload().readAllBytes());
        }

        return MultipartMessageProcessedResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }


}
