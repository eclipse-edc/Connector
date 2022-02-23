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

import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartMessageProcessedResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementMessage;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

/**
 * IdsMultipartSender implementation for contract agreements. Sends IDS ContractAgreementMessages and
 * expects an IDS RequestInProcessMessage as the response.
 */
public class MultipartContractAgreementSender extends IdsMultipartSender<ContractAgreementMessage, MultipartMessageProcessedResponse> {

    private final TransformerRegistry transformerRegistry;

    public MultipartContractAgreementSender(@NotNull String connectorId,
                                            @NotNull OkHttpClient httpClient,
                                            @NotNull Serializer serializer,
                                            @NotNull Monitor monitor,
                                            @NotNull IdentityService identityService,
                                            @NotNull TransformerRegistry transformerRegistry,
                                            @NotNull String idsWebhookAddress) {
        super(connectorId, idsWebhookAddress, httpClient, monitor, identityService, transformerRegistry, serializer);

        this.transformerRegistry = transformerRegistry;
    }

    @Override
    public Class<ContractAgreementMessage> messageType() {
        return ContractAgreementMessage.class;
    }

    @Override
    protected String retrieveRemoteConnectorId(ContractAgreementMessage request) {
        return request.getConnectorId();
    }

    @Override
    protected String retrieveRemoteConnectorAddress(ContractAgreementMessage request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(ContractAgreementMessage request, DynamicAttributeToken token) throws Exception {
        var idsMsgId = IdsId.Builder.newInstance().type(IdsType.MESSAGE).value(UUID.randomUUID().toString()).build();
        var msgUri = transformerRegistry.transform(idsMsgId, URI.class);
        if (msgUri.failed()) {
            throw new EdcException("Cannot convert contract agreement id to URI");
        }

        var correlationMsgId = IdsId.Builder.newInstance().type(IdsType.MESSAGE).value(request.getContractOfferMessageId()).build();
        var correlationMsgUri = transformerRegistry.transform(correlationMsgId, URI.class);
        if (correlationMsgUri.failed()) {
            throw new EdcException("Cannot convert contract offer message id to URI");
        }

        return new ContractAgreementMessageBuilder(msgUri.getContent())
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getSenderAgentURI())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._correlationMessage_(correlationMsgUri.getContent())
                .build();
    }

    @Override
    protected String buildMessagePayload(ContractAgreementMessage request) throws Exception {
        var contractAgreement = request.getContractAgreement();
        var transformationResult = getTransformerRegistry().transform(contractAgreement, ContractAgreement.class);
        if (transformationResult.failed()) {
            throw new EdcException("Failed to create IDS contract agreement");
        }

        var idsContractAgreement = transformationResult.getContent();
        return getSerializer().serialize(idsContractAgreement);
    }

    @Override
    protected MultipartMessageProcessedResponse getResponseContent(IdsMultipartParts parts) throws Exception {
        Message header = getSerializer().deserialize(parts.getHeader(), Message.class);
        String payload = null;
        if (parts.getPayload() != null) {
            payload = parts.getPayload();
        }

        return MultipartMessageProcessedResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }


}
