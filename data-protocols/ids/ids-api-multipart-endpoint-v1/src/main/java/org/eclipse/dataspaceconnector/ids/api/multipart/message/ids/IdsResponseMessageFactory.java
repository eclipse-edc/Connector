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
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.message.ids;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import de.fraunhofer.iais.eis.RejectionReason;
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
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
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

    /**
     * It may be necessary to reject messages with a missing message ID.
     * In this case this constant can be used as correlation message instead.
     */
    public static final URI NULL_CORRELATION_MESSAGE_ID = URI.create(String.join(
            IdsIdParser.DELIMITER,
            IdsIdParser.SCHEME,
            IdsType.MESSAGE.getValue(),
            "null"));

    /**
     * It may be necessary to reject messages with a missing sender agent.
     * In this case this constant can be used as recipient agent instead.
     */
    public static final URI NULL_RECIPIENT_AGENT = URI.create(String.join(
            IdsIdParser.DELIMITER,
            IdsIdParser.SCHEME,
            IdsType.CONNECTOR.getValue(),
            "null"));

    /**
     * It may be necessary to reject messages with a missing issuer connector.
     * In this case this constant can be used as recipient connector instead.
     */
    public static final URI NULL_RECIPIENT_CONNECTOR = URI.create(String.join(
            IdsIdParser.DELIMITER,
            IdsIdParser.SCHEME,
            IdsType.CONNECTOR.getValue(),
            "null"));

    /**
     * It may be necessary to reject messages, even when the EDC is not able to provide client credentials for itself.
     * In this case this constant can be used as security token value instead.
     */
    public static final String NULL_TOKEN = "null.null.null";

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
     * In general using IDS it should always be tried to answer with an IDS message. Therefore, in case an exception occurs
     * while creating an IDS response message, this method should be called.
     * In comparison to the other message creating methods, this one will not throw an exception in case
     * there are issues with the correlation message.
     *
     * @param correlationMessage message the rejection is referring to
     * @param reason             exception that was thrown when creating a response message
     * @return RejectionMessage
     */
    public RejectionMessage createRejectionMessage(@NotNull Message correlationMessage, @NotNull Exception reason) {
        Objects.requireNonNull(correlationMessage);
        Objects.requireNonNull(reason);

        String randomMessageId = String.join(
                IdsIdParser.DELIMITER,
                IdsIdParser.SCHEME,
                IdsType.MESSAGE.getValue(),
                UUID.randomUUID().toString());

        RejectionMessageBuilder builder = new RejectionMessageBuilder(URI.create(randomMessageId));

        builder._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        // builder._issued_(CalendarUtil.gregorianNow()); // TODO enable with IDS-Serializer from issue 236
        builder._issuerConnector_(connectorId);
        builder._senderAgent_(connectorId);

        URI correlationMessageId = correlationMessage.getId();
        if (correlationMessageId == null) {
            correlationMessageId = NULL_CORRELATION_MESSAGE_ID;
        }
        builder._correlationMessage_(correlationMessageId);

        URI recipientAgent = correlationMessage.getSenderAgent();
        if (recipientAgent == null) {
            recipientAgent = NULL_RECIPIENT_AGENT;
        }
        builder._recipientAgent_(new ArrayList<>(Collections.singletonList(recipientAgent)));

        URI recipientConnector = correlationMessage.getIssuerConnector();
        if (recipientConnector == null) {
            recipientConnector = NULL_RECIPIENT_CONNECTOR;
        }
        builder._recipientConnector_(new ArrayList<>(Collections.singletonList(recipientConnector)));

        TokenParameters tokenParameters = TokenParameters.Builder.newInstance()
                .scope(IdsClientCredentialsScope.ALL)
                .audience(recipientConnector.toString())
                .build();
        Result<TokenRepresentation> tokenResult = identityService.obtainClientCredentials(tokenParameters);
        if (tokenResult.failed()) {
            tokenResult = Result.success(TokenRepresentation.Builder.newInstance().token(NULL_TOKEN).build());
        }
        DynamicAttributeToken token = new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_(tokenResult.getContent().getToken())
                .build();
        builder._securityToken_(token);

        if (reason instanceof InvalidCorrelationMessageException) {
            builder._rejectionReason_(RejectionReason.BAD_PARAMETERS);
        } else if (reason instanceof MissingClientCredentialsException) {
            builder._rejectionReason_(RejectionReason.INTERNAL_RECIPIENT_ERROR);
        } else {
            builder._rejectionReason_(RejectionReason.INTERNAL_RECIPIENT_ERROR);
        }

        return builder.build();
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

        URI recipientAgent = correlationMessage.getSenderAgent();
        if (recipientAgent == null) {
            throw InvalidCorrelationMessageException.createExceptionForSenderAgentMissing();
        }
        builder._recipientAgent_(new ArrayList<>(Collections.singletonList(recipientAgent)));

        URI recipientConnector = correlationMessage.getIssuerConnector();
        if (recipientConnector == null) {
            throw InvalidCorrelationMessageException.createExceptionForIssuerConnectorMissing();
        }
        builder._recipientConnector_(new ArrayList<>(Collections.singletonList(recipientConnector)));

        Result<TokenRepresentation> tokenResult = identityService.obtainClientCredentials(TokenParameters.Builder.newInstance()
                .scope(IdsClientCredentialsScope.ALL)
                .audience(recipientConnector.toString())
                .build());
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
