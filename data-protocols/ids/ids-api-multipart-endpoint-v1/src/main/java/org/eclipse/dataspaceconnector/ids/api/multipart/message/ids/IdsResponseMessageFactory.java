/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.message.ids;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RequestInProcessMessage;
import de.fraunhofer.iais.eis.RequestInProcessMessageBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.exceptions.InvalidCorrelationMessageException;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.exceptions.MissingClientCredentialsException;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * The {@link  IdsResponseMessageFactory} creates IDS compliant {@link de.fraunhofer.iais.eis.ResponseMessage}.
 */
@SuppressWarnings("DuplicatedCode")
public class IdsResponseMessageFactory {

    private final URI connectorId;
    private final IdentityService identityService;

    public IdsResponseMessageFactory(@NotNull String connectorId, @NotNull IdentityService identityService) {
        Objects.requireNonNull(connectorId);
        this.identityService = Objects.requireNonNull(identityService);

        URI uri;
        try {
            IdsId connectorIdsId = IdsIdParser.parse(connectorId);
            boolean isConnectorUrn = connectorIdsId != null && connectorIdsId.getType() == IdsType.CONNECTOR;
            if (!isConnectorUrn) {
                throw new EdcException("Connector URN not of type IdsType.CONNECTOR");
            }

            uri = URI.create(connectorId);
        } catch (IllegalArgumentException ignore) { // if connectorId is no URN
            String urn = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.CONNECTOR.getValue(), connectorId);
            uri = URI.create(urn);
        }
        this.connectorId = uri;
    }

    /**
     * Create IDS compliant {@link RequestInProcessMessage}.
     *
     * @param correlationMessage message the response is referring to
     * @return RequestInProcessMessage
     */
    public RequestInProcessMessage createRequestInProcessMessage(@NotNull Message correlationMessage) {
        Objects.requireNonNull(correlationMessage);

        String randomMessageId = String.join(
                IdsIdParser.DELIMITER,
                IdsIdParser.SCHEME,
                IdsType.MESSAGE.getValue(),
                UUID.randomUUID().toString());

        RequestInProcessMessageBuilder builder = new RequestInProcessMessageBuilder(URI.create(randomMessageId));

        builder._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        // builder._issued_(CalendarUtil.gregorianNow()); // TODO enable with IDS-Serializer from issue 236
        builder._issuerConnector_(connectorId);
        builder._senderAgent_(connectorId);

        URI correlationMessageId = correlationMessage.getId();
        if (correlationMessageId == null) {
            throw InvalidCorrelationMessageException.createExceptionForCorrelationIdMissing();
        }
        builder._correlationMessage_(correlationMessageId);

        URI senderAgent = correlationMessage.getSenderAgent();
        if (senderAgent == null) {
            throw InvalidCorrelationMessageException.createExceptionForSenderAgentMissing();
        }
        builder._recipientAgent_(new ArrayList<>(Collections.singletonList(senderAgent)));

        URI issuerConnector = correlationMessage.getIssuerConnector();
        if (issuerConnector == null) {
            throw InvalidCorrelationMessageException.createExceptionForIssuerConnectorMissing();
        }
        builder._recipientConnector_(new ArrayList<>(Collections.singletonList(issuerConnector)));

        Result<TokenRepresentation> tokenResult = identityService.obtainClientCredentials(IdsClientCredentialsScope.ALL);
        if (tokenResult.failed()) {
            throw new MissingClientCredentialsException(tokenResult.getFailureMessages());
        }
        DynamicAttributeToken token = new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_(tokenResult.getContent().getToken())
                .build();
        builder._securityToken_(token);

        return builder.build();
    }
}
