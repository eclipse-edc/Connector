/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
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
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsType;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.util.ResponseUtil.parseMultipartStringResponse;
import static org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * IdsMultipartSender implementation for contract agreements. Sends IDS ContractAgreementMessages and
 * expects an IDS RequestInProcessMessage as the response.
 */
public class MultipartContractAgreementSender extends IdsMultipartSender<ContractAgreementRequest, String> {

    private final String idsWebhookAddress;

    public MultipartContractAgreementSender(@NotNull String connectorId,
                                            @NotNull OkHttpClient httpClient,
                                            @NotNull ObjectMapper objectMapper,
                                            @NotNull Monitor monitor,
                                            @NotNull IdentityService identityService,
                                            @NotNull IdsTransformerRegistry transformerRegistry,
                                            @NotNull String idsWebhookAddress) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);

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

    /**
     * Builds a {@link de.fraunhofer.iais.eis.ContractAgreementMessage} for the given {@link ContractAgreementRequest}.
     *
     * @param request the request.
     * @param token   the dynamic attribute token.
     * @return a ContractAgreementMessage.
     * @throws Exception if the agreement ID cannot be parsed.
     */
    @Override
    protected Message buildMessageHeader(ContractAgreementRequest request, DynamicAttributeToken token) throws Exception {
        var idsId = IdsId.Builder.newInstance()
                .type(IdsType.CONTRACT_AGREEMENT)
                .value(request.getContractAgreement().getId())
                .build();

        var message = new ContractAgreementMessageBuilder(idsId.toUri())
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(CalendarUtil.gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._transferContract_(URI.create(request.getCorrelationId()))
                .build();

        message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, idsWebhookAddress);

        return message;
    }

    /**
     * Builds the payload for the agreement request. The payload contains the {@link ContractAgreement}.
     *
     * @param request the request.
     * @return the contract agreement as JSON-LD.
     * @throws Exception if parsing the agreement fails.
     */
    @Override
    protected String buildMessagePayload(ContractAgreementRequest request) throws Exception {
        var transformationResult = getTransformerRegistry().transform(request, ContractAgreement.class);
        if (transformationResult.failed()) {
            throw new EdcException("Failed to create IDS contract agreement");
        }

        var idsContractAgreement = transformationResult.getContent();
        return getObjectMapper().writeValueAsString(idsContractAgreement);
    }

    /**
     * Parses the response content.
     *
     * @param parts container object for response header and payload InputStreams.
     * @return a MultipartResponse containing the message header and the response payload as string.
     * @throws Exception if parsing header or payload fails.
     */
    @Override
    protected MultipartResponse<String> getResponseContent(IdsMultipartParts parts) throws Exception {
        return parseMultipartStringResponse(parts, getObjectMapper());
    }

    @Override
    protected List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(MessageProcessedNotificationMessageImpl.class);
    }

}
