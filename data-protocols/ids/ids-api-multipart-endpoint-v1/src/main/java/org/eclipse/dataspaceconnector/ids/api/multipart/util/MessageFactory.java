/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.util;

import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageBuilder;
import de.fraunhofer.iais.eis.NotificationMessage;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.RequestInProcessMessageBuilder;
import de.fraunhofer.iais.eis.ResponseMessage;
import de.fraunhofer.iais.eis.ResponseMessageBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

public class MessageFactory {

    private final URI connectorId;
    private final IdentityService identityService;

    public MessageFactory(@NotNull URI connectorId, @NotNull IdentityService identityService) {
        this.connectorId = Objects.requireNonNull(connectorId);
        this.identityService = Objects.requireNonNull(identityService);
    }

    @NotNull
    public DescriptionResponseMessage createDescriptionResponseMessage(@NotNull Message correlationMessage) {
        Objects.requireNonNull(correlationMessage);

        URI messageId = URI.create(String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.MESSAGE.getValue(), UUID.randomUUID().toString()));
        DescriptionResponseMessageBuilder builder = new DescriptionResponseMessageBuilder(messageId);

        builder._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);

        builder._issuerConnector_(connectorId);
        builder._senderAgent_(connectorId);
        builder._securityToken_(getToken());

        builder._issued_(CalendarUtil.gregorianNow());

        URI id = correlationMessage.getId();
        if (id != null) {
            builder._correlationMessage_(id);
        }

        URI senderAgent = correlationMessage.getSenderAgent();
        if (senderAgent != null) {
            builder._recipientAgent_(new ArrayList<>(Collections.singletonList(senderAgent)));
        }

        URI issuerConnector = correlationMessage.getIssuerConnector();
        if (issuerConnector != null) {
            builder._recipientConnector_(new ArrayList<>(Collections.singletonList(issuerConnector)));
        }

        return builder.build();
    }

    @NotNull
    public RejectionMessage rejectNotFound(@NotNull Message correlationMessage) {
        return createRejectionMessageBuilder(correlationMessage)
                ._rejectionReason_(RejectionReason.NOT_FOUND)
                .build();
    }

    @NotNull
    public RejectionMessage rejectNotAuthenticated(@NotNull Message correlationMessage) {
        return createRejectionMessageBuilder(correlationMessage)
                ._rejectionReason_(RejectionReason.NOT_AUTHENTICATED)
                .build();
    }

    @NotNull
    public RejectionMessage notAuthorized(@NotNull Message correlationMessage) {
        return createRejectionMessageBuilder(correlationMessage)
                ._rejectionReason_(RejectionReason.NOT_AUTHORIZED)
                .build();
    }

    @NotNull
    public RejectionMessage malformedMessage(@NotNull Message correlationMessage) {
        return createRejectionMessageBuilder(correlationMessage)
                ._rejectionReason_(RejectionReason.MALFORMED_MESSAGE)
                .build();
    }

    @NotNull
    public RejectionMessage messageTypeNotSupported(@NotNull Message correlationMessage) {
        return createRejectionMessageBuilder(correlationMessage)
                ._rejectionReason_(RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED)
                .build();
    }

    @NotNull
    public RejectionMessage badParameters(@NotNull Message correlationMessage) {
        return createRejectionMessageBuilder(correlationMessage)
                ._rejectionReason_(RejectionReason.BAD_PARAMETERS)
                .build();
    }

    @NotNull
    public RejectionMessage internalRecipientError(@NotNull Message correlationMessage) {
        return createRejectionMessageBuilder(correlationMessage)
                ._rejectionReason_(RejectionReason.INTERNAL_RECIPIENT_ERROR)
                .build();
    }

    @NotNull
    private RejectionMessageBuilder createRejectionMessageBuilder(@NotNull Message correlationMessage) {
        Objects.requireNonNull(correlationMessage);
        Objects.requireNonNull(connectorId);

        String id = String.join(
                IdsIdParser.DELIMITER,
                IdsIdParser.SCHEME,
                IdsType.MESSAGE.getValue(),
                UUID.randomUUID().toString());

        RejectionMessageBuilder builder = new RejectionMessageBuilder(URI.create(id));

        builder._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._issued_(CalendarUtil.gregorianNow());
        builder._securityToken_(getToken());

        builder._issuerConnector_(connectorId);
        builder._senderAgent_(connectorId);

        URI correlationMessageId = correlationMessage.getId();
        if (correlationMessageId != null) {
            builder._correlationMessage_(correlationMessageId);
        }

        URI senderAgent = correlationMessage.getSenderAgent();
        if (senderAgent != null) {
            builder._recipientAgent_(new ArrayList<>(Collections.singletonList(senderAgent)));
        }

        URI issuerConnector = correlationMessage.getIssuerConnector();
        if (issuerConnector != null) {
            builder._recipientConnector_(new ArrayList<>(Collections.singletonList(issuerConnector)));
        }

        return builder;
    }

    public ResponseMessage createDummyResponse(@Nullable Message correlationMessage) {

        URI messageId = URI.create(String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.MESSAGE.getValue(), UUID.randomUUID().toString()));
        ResponseMessageBuilder builder = new ResponseMessageBuilder(messageId);

        builder._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);

        builder._issuerConnector_(connectorId);
        builder._senderAgent_(connectorId);
        builder._securityToken_(getToken());

        if (correlationMessage != null) {
            URI id = correlationMessage.getId();
            if (id != null) {
                builder._correlationMessage_(id);
            }

            URI senderAgent = correlationMessage.getSenderAgent();
            if (senderAgent != null) {
                builder._recipientAgent_(new ArrayList<>(Collections.singletonList(senderAgent)));
            }

            URI issuerConnector = correlationMessage.getIssuerConnector();
            if (issuerConnector != null) {
                builder._recipientConnector_(new ArrayList<>(Collections.singletonList(issuerConnector)));
            }
        }

        return builder.build();
    }

    public NotificationMessage createRequestInProcessMessage(@Nullable Message correlationMessage) {

        var messageId = URI.create(String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.MESSAGE.getValue(), UUID.randomUUID().toString()));
        var builder = new RequestInProcessMessageBuilder(messageId);

        builder._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._issued_(gregorianNow());

        builder._issuerConnector_(connectorId);
        builder._senderAgent_(connectorId);
        builder._securityToken_(getToken());

        if (correlationMessage != null) {
            var id = correlationMessage.getId();
            if (id != null) {
                builder._correlationMessage_(id);
            }

            var senderAgent = correlationMessage.getSenderAgent();
            if (senderAgent != null) {
                builder._recipientAgent_(new ArrayList<>(Collections.singletonList(senderAgent)));
            }

            var issuerConnector = correlationMessage.getIssuerConnector();
            if (issuerConnector != null) {
                builder._recipientConnector_(new ArrayList<>(Collections.singletonList(issuerConnector)));
            }
        }

        return builder.build();
    }

    public NotificationMessage createMessageProcessedNotificationMessage(@Nullable Message correlationMessage) {

        var messageId = URI.create(String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.MESSAGE.getValue(), UUID.randomUUID().toString()));
        var builder = new MessageProcessedNotificationMessageBuilder(messageId);

        builder._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._issued_(gregorianNow());

        builder._issuerConnector_(connectorId);
        builder._senderAgent_(connectorId);
        builder._securityToken_(getToken());

        if (correlationMessage != null) {
            var id = correlationMessage.getId();
            if (id != null) {
                builder._correlationMessage_(id);
            }

            var senderAgent = correlationMessage.getSenderAgent();
            if (senderAgent != null) {
                builder._recipientAgent_(new ArrayList<>(Collections.singletonList(senderAgent)));
            }

            var issuerConnector = correlationMessage.getIssuerConnector();
            if (issuerConnector != null) {
                builder._recipientConnector_(new ArrayList<>(Collections.singletonList(issuerConnector)));
            }
        }

        return builder.build();
    }

    private DynamicAttributeToken getToken() {
        var clientTokenResult = identityService.obtainClientCredentials("");
        if (clientTokenResult.failed()) {
            String message = "Failed to obtain token: " + String.join(",", clientTokenResult.getFailureMessages());
            throw new EdcException(message);
        }
        return new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_(clientTokenResult.getContent().getToken())
                .build();
    }
}
