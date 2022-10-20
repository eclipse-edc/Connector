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
 *       Fraunhofer Institute for Software and Systems Engineering - additional message building methods, refactoring
 *       Daimler TSS GmbH - introduce factory to create RequestInProcessMessage
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.util;

import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessageBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.RequestInProcessMessage;
import de.fraunhofer.iais.eis.RequestInProcessMessageBuilder;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static org.eclipse.edc.protocol.ids.util.CalendarUtil.gregorianNow;

/**
 * Provides utility methods for building IDS Multipart responses.
 */
public class ResponseUtil {

    /**
     * Creates a multipart response with the given header.
     *
     * @param header the header.
     * @return a multipart response.
     */
    public static MultipartResponse createMultipartResponse(@NotNull Message header) {
        return MultipartResponse.Builder.newInstance()
                .header(header)
                .build();
    }

    /**
     * Creates a multipart response with the given header and payload.
     *
     * @param header the header.
     * @param payload the payload.
     * @return a multipart response.
     */
    public static MultipartResponse createMultipartResponse(@NotNull Message header, @NotNull Object payload) {
        return MultipartResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }

    /**
     * Creates a MessageProcessedNotificationMessage.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return a MessageProcessedNotificationMessage.
     */
    public static MessageProcessedNotificationMessage messageProcessedNotification(@NotNull Message correlationMessage,
                                                                                   @NotNull IdsId connectorId) {
        var messageId = getMessageId();

        return new MessageProcessedNotificationMessageBuilder(messageId)
                ._contentVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._issuerConnector_(connectorId.toUri())
                ._senderAgent_(connectorId.toUri())
                ._correlationMessage_(correlationMessage.getId())
                ._recipientConnector_(new ArrayList<>(Collections.singletonList(correlationMessage.getIssuerConnector())))
                ._recipientAgent_(new ArrayList<>(Collections.singletonList(correlationMessage.getSenderAgent())))
                .build();
    }

    /**
     * Creates a RequestInProcessMessage.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return a RequestInProcessMessage.
     */
    public static RequestInProcessMessage requestInProcess(@NotNull Message correlationMessage,
                                                           @NotNull IdsId connectorId) {
        var messageId = getMessageId();

        return new RequestInProcessMessageBuilder(messageId)
                ._contentVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._issuerConnector_(connectorId.toUri())
                ._senderAgent_(connectorId.toUri())
                ._correlationMessage_(correlationMessage.getId())
                ._recipientConnector_(new ArrayList<>(Collections.singletonList(correlationMessage.getIssuerConnector())))
                ._recipientAgent_(new ArrayList<>(Collections.singletonList(correlationMessage.getSenderAgent())))
                .build();
    }

    /**
     * Creates a DescriptionResponseMessage.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return a DescriptionResponseMessage.
     */
    public static DescriptionResponseMessage descriptionResponse(@NotNull Message correlationMessage,
                                                                 @NotNull IdsId connectorId) {
        var messageId = getMessageId();

        return new DescriptionResponseMessageBuilder(messageId)
                ._contentVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._issuerConnector_(connectorId.toUri())
                ._senderAgent_(connectorId.toUri())
                ._correlationMessage_(correlationMessage.getId())
                ._recipientConnector_(new ArrayList<>(Collections.singletonList(correlationMessage.getIssuerConnector())))
                ._recipientAgent_(new ArrayList<>(Collections.singletonList(correlationMessage.getSenderAgent())))
                .build();
    }

    /**
     * Creates a response message depending on the status result of a previously executed action.
     * Returns a RequestInProcessMessage, if the result is succeeded and a rejection message otherwise.
     * The rejection reason is BAD_PARAMETERS if the action can be retried and INTERNAL_RECIPIENT_ERROR
     * for a fatal error.
     *
     * @param statusResult the status result.
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the response message depending on the status result.
     */
    public static Message inProcessFromStatusResult(@NotNull StatusResult<?> statusResult,
                                                    @NotNull Message correlationMessage,
                                                    @NotNull IdsId connectorId) {
        if (statusResult.succeeded()) {
            return requestInProcess(correlationMessage, connectorId);
        } else {
            if (statusResult.fatalError()) {
                return badParameters(correlationMessage, connectorId);
            } else {
                return internalRecipientError(correlationMessage, connectorId);
            }
        }
    }

    /**
     * Creates a response message depending on the status result of a previously executed action.
     * Returns a MessageProcessedNotificationMessage, if the result is succeeded and a rejection message otherwise.
     * The rejection reason is BAD_PARAMETERS if the action can be retried and INTERNAL_RECIPIENT_ERROR
     * for a fatal error.
     *
     * @param statusResult the status result.
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the response message depending on the status result.
     */
    public static Message processedFromStatusResult(@NotNull StatusResult<?> statusResult,
                                                    @NotNull Message correlationMessage,
                                                    @NotNull IdsId connectorId) {
        if (statusResult.succeeded()) {
            return messageProcessedNotification(correlationMessage, connectorId);
        } else {
            if (statusResult.fatalError()) {
                return badParameters(correlationMessage, connectorId);
            } else {
                return internalRecipientError(correlationMessage, connectorId);
            }
        }
    }

    /**
     * Creates a rejection message with reason NOT_FOUND.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the rejection message.
     */
    @NotNull
    public static RejectionMessage notFound(@NotNull Message correlationMessage,
                                            @NotNull IdsId connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.NOT_FOUND)
                .build();
    }

    /**
     * Creates a rejection message with reason NOT_AUTHENTICATED.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the rejection message.
     */
    @NotNull
    public static RejectionMessage notAuthenticated(@NotNull Message correlationMessage,
                                                    @NotNull IdsId connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.NOT_AUTHENTICATED)
                .build();
    }

    /**
     * Creates a rejection message with reason NOT_AUTHORIZED.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the rejection message.
     */
    @NotNull
    public static RejectionMessage notAuthorized(@NotNull Message correlationMessage,
                                                 @NotNull IdsId connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.NOT_AUTHORIZED)
                .build();
    }

    /**
     * Creates a rejection message with reason MALFORMED_MESSAGE.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the rejection message.
     */
    @NotNull
    public static RejectionMessage malformedMessage(@Nullable Message correlationMessage,
                                                    @NotNull IdsId connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.MALFORMED_MESSAGE)
                .build();
    }

    /**
     * Creates a rejection message with reason MESSAGE_TYPE_NOT_SUPPORTED.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the rejection message.
     */
    @NotNull
    public static RejectionMessage messageTypeNotSupported(@NotNull Message correlationMessage,
                                                           @NotNull IdsId connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED)
                .build();
    }

    /**
     * Creates a rejection message with reason BAD_PARAMETERS.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the rejection message.
     */
    @NotNull
    public static RejectionMessage badParameters(@NotNull Message correlationMessage,
                                                 @NotNull IdsId connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.BAD_PARAMETERS)
                .build();
    }

    /**
     * Creates a rejection message with reason INTERNAL_RECIPIENT_ERROR.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the rejection message.
     */
    @NotNull
    public static RejectionMessage internalRecipientError(@NotNull Message correlationMessage,
                                                          @NotNull IdsId connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.INTERNAL_RECIPIENT_ERROR)
                .build();
    }

    /**
     * Creates a generic rejection message builder without rejection reason.
     *
     * @param correlationMessage the request.
     * @param connectorId the connector ID.
     * @return the rejection message builder.
     */
    @NotNull
    private static RejectionMessageBuilder createRejectionMessageBuilder(@Nullable Message correlationMessage,
                                                                         @NotNull IdsId connectorId) {

        var messageId = getMessageId();

        var builder = new RejectionMessageBuilder(messageId)
                ._contentVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._issuerConnector_(connectorId.toUri())
                ._senderAgent_(connectorId.toUri());

        if (correlationMessage != null) {
            builder._correlationMessage_(correlationMessage.getId());
            builder._recipientAgent_(new ArrayList<>(Collections.singletonList(correlationMessage.getSenderAgent())));
            builder._recipientConnector_(new ArrayList<>(Collections.singletonList(correlationMessage.getIssuerConnector())));
        }

        return builder;
    }

    /**
     * Creates an ID for IDS messages.
     *
     * @return the ID.
     */
    private static URI getMessageId() {
        return IdsId.Builder.newInstance().value(UUID.randomUUID().toString()).type(IdsType.MESSAGE).build().toUri();
    }
}
