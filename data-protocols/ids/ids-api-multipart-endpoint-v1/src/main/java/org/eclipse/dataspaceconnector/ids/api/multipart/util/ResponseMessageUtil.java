/*
 *  Copyright (c) 2021 Daimler TSS GmbH, Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - additional message building methods
 *       Daimler TSS GmbH - introduce factory to create RequestInProcessMessage
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.util;

import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessageBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageBuilder;
import de.fraunhofer.iais.eis.NotificationMessage;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.RequestInProcessMessageBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

public class ResponseMessageUtil {

    public static NotificationMessage createMessageProcessedNotificationMessage(
            @NotNull String connectorId,
            @NotNull Message correlationMessage) {
        var messageId = getMessageId();
        var connectorIdUri = getConnectorUrn(connectorId);
        
        return new MessageProcessedNotificationMessageBuilder(messageId)
                ._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._issuerConnector_(connectorIdUri)
                ._senderAgent_(connectorIdUri)
                ._correlationMessage_(correlationMessage.getId())
                ._recipientConnector_(new ArrayList<>(Collections.singletonList(correlationMessage.getIssuerConnector())))
                .build();
    }
    
    public static NotificationMessage createRequestInProcessMessage(@NotNull String connectorId,
                                                                    @NotNull Message correlationMessage) {
        var messageId = getMessageId();
        var connectorIdUri = getConnectorUrn(connectorId);
        
        return new RequestInProcessMessageBuilder(messageId)
                ._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._issuerConnector_(connectorIdUri)
                ._senderAgent_(connectorIdUri)
                ._correlationMessage_(correlationMessage.getId())
                ._recipientConnector_(new ArrayList<>(Collections.singletonList(correlationMessage.getIssuerConnector())))
                .build();
    }
    
    public static DescriptionResponseMessage createDescriptionResponseMessage(@Nullable String connectorId,
                                                                              @NotNull Message correlationMessage) {
        var messageId = getMessageId();
        var connectorIdUri = getConnectorUrn(connectorId);
    
        return new DescriptionResponseMessageBuilder(messageId)
                ._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._issuerConnector_(connectorIdUri)
                ._senderAgent_(connectorIdUri)
                ._correlationMessage_(correlationMessage.getId())
                ._recipientAgent_(new ArrayList<>(Collections.singletonList(correlationMessage.getSenderAgent())))
                ._recipientConnector_(new ArrayList<>(Collections.singletonList(correlationMessage.getIssuerConnector())))
                .build();
    }
    
    public static Message createResponseMessageForStatusResult(StatusResult<?> statusResult, String connectorId, Message correlationMessage) {
        if (statusResult.succeeded()) {
            return createRequestInProcessMessage(connectorId, correlationMessage);
        } else {
            if (statusResult.fatalError()) {
                return badParameters(correlationMessage, connectorId);
            } else {
                return internalRecipientError(correlationMessage, connectorId);
            }
        }
    }
    
    @NotNull
    public static RejectionMessage notFound(
            @NotNull Message correlationMessage, @NotNull String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.NOT_FOUND)
                .build();
    }
    
    @NotNull
    public static RejectionMessage notAuthenticated(
            @NotNull Message correlationMessage, @NotNull String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.NOT_AUTHENTICATED)
                .build();
    }
    
    @NotNull
    public static RejectionMessage notAuthorized(
            @NotNull Message correlationMessage, @NotNull String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.NOT_AUTHORIZED)
                .build();
    }
    
    @NotNull
    public static RejectionMessage malformedMessage(
            @Nullable Message correlationMessage, @NotNull String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.MALFORMED_MESSAGE)
                .build();
    }
    
    @NotNull
    public static RejectionMessage messageTypeNotSupported(
            @NotNull Message correlationMessage, @NotNull String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED)
                .build();
    }
    
    @NotNull
    public static RejectionMessage badParameters(
            @NotNull Message correlationMessage, @NotNull String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.BAD_PARAMETERS)
                .build();
    }
    
    @NotNull
    public static RejectionMessage internalRecipientError(
            @NotNull Message correlationMessage, @NotNull String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.INTERNAL_RECIPIENT_ERROR)
                .build();
    }
    
    @NotNull
    private static RejectionMessageBuilder createRejectionMessageBuilder(
            @Nullable Message correlationMessage, @NotNull String connectorId) {
        
        var messageId = getMessageId();
        var connectorIdUri = getConnectorUrn(connectorId);
        
        var builder = new RejectionMessageBuilder(messageId)
                ._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._issuerConnector_(connectorIdUri)
                ._senderAgent_(connectorIdUri);
        
        if (correlationMessage != null) {
            builder._correlationMessage_(correlationMessage.getId());
            builder._recipientAgent_(new ArrayList<>(Collections.singletonList(correlationMessage.getSenderAgent())));
            builder._recipientConnector_(new ArrayList<>(Collections.singletonList(correlationMessage.getIssuerConnector())));
        }
        
        return builder;
    }
    
    private static URI getMessageId() {
        return URI.create(String.join(
                IdsIdParser.DELIMITER,
                IdsIdParser.SCHEME,
                IdsType.MESSAGE.getValue(),
                UUID.randomUUID().toString()));
    }
    
    private static URI getConnectorUrn(String connectorId) {
        return URI.create(String.join(
                IdsIdParser.DELIMITER,
                IdsIdParser.SCHEME,
                IdsType.CONNECTOR.getValue(),
                connectorId));
    }
}
