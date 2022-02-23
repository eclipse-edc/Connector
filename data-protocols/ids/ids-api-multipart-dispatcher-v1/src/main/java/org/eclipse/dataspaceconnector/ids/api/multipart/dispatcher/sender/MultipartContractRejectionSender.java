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

import de.fraunhofer.iais.eis.ContractRejectionMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartMessageProcessedResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractRejectionMessage;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

/**
 * IdsMultipartSender implementation for contract rejections. Sends IDS ContractRequestMessages and
 * expects an IDS RequestInProcessMessage as the response.
 */
public class MultipartContractRejectionSender extends IdsMultipartSender<ContractRejectionMessage, MultipartMessageProcessedResponse> {

    public MultipartContractRejectionSender(@NotNull String connectorId,
                                            @NotNull String idsWebhookAddress,
                                            @NotNull OkHttpClient httpClient,
                                            @NotNull Serializer serializer,
                                            @NotNull Monitor monitor,
                                            @NotNull IdentityService identityService,
                                            @NotNull TransformerRegistry transformerRegistry) {
        super(connectorId, idsWebhookAddress, httpClient, monitor, identityService, transformerRegistry, serializer);
    }

    @Override
    public Class<ContractRejectionMessage> messageType() {
        return ContractRejectionMessage.class;
    }

    @Override
    protected String retrieveRemoteConnectorId(ContractRejectionMessage rejection) {
        return rejection.getConnectorId();
    }

    @Override
    protected String retrieveRemoteConnectorAddress(ContractRejectionMessage rejection) {
        return rejection.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(ContractRejectionMessage rejection, DynamicAttributeToken token) {

        IdsId rejectionMessageId = IdsId.Builder.newInstance().type(IdsType.MESSAGE).value(rejection.getContractOfferMessageId()).build();
        Result<URI> rejectionMessageUri = getTransformerRegistry().transform(rejectionMessageId, URI.class);
        if (rejectionMessageUri.failed()) {
            throw new EdcException("Cannot convert message id to URI");
        }

        return new ContractRejectionMessageBuilder()
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getSenderAgentURI())
                ._recipientConnector_(Collections.singletonList(URI.create(rejection.getConnectorId())))
                ._recipientAgent_(Collections.singletonList(URI.create(rejection.getConnectorId())))
                ._contractRejectionReason_(new TypedLiteral(rejection.getRejectionReason()))
                ._correlationMessage_(rejectionMessageUri.getContent())
                .build();
    }

    @Override
    protected String buildMessagePayload(ContractRejectionMessage rejection) throws Exception {
        return rejection.getRejectionReason();
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
