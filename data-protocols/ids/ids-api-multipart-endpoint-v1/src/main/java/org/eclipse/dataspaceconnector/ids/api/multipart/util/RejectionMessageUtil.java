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

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import de.fraunhofer.iais.eis.RejectionReason;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public final class RejectionMessageUtil {

    private RejectionMessageUtil() {
    }

    @NotNull
    public static RejectionMessage notFound(
            @Nullable Message correlationMessage, @Nullable String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.NOT_FOUND)
                .build();
    }

    @NotNull
    public static RejectionMessage notAuthenticated(
            @Nullable Message correlationMessage, @Nullable String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.NOT_AUTHENTICATED)
                .build();
    }

    @NotNull
    public static RejectionMessage notAuthorized(
            @Nullable Message correlationMessage, @Nullable String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.NOT_AUTHORIZED)
                .build();
    }

    @NotNull
    public static RejectionMessage malformedMessage(
            @Nullable Message correlationMessage, @Nullable String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.MALFORMED_MESSAGE)
                .build();
    }

    @NotNull
    public static RejectionMessage messageTypeNotSupported(
            @Nullable Message correlationMessage, @Nullable String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED)
                .build();
    }

    @NotNull
    public static RejectionMessage badParameters(
            @Nullable Message correlationMessage, @Nullable String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.BAD_PARAMETERS)
                .build();
    }

    @NotNull
    public static RejectionMessage internalRecipientError(
            @Nullable Message correlationMessage, @Nullable String connectorId) {
        return createRejectionMessageBuilder(correlationMessage, connectorId)
                ._rejectionReason_(RejectionReason.INTERNAL_RECIPIENT_ERROR)
                .build();
    }

    @NotNull
    private static RejectionMessageBuilder createRejectionMessageBuilder(
            @Nullable Message correlationMessage, @Nullable String connectorId) {

        String id = String.join(
                IdsIdParser.DELIMITER,
                IdsIdParser.SCHEME,
                IdsType.MESSAGE.getValue(),
                UUID.randomUUID().toString());

        RejectionMessageBuilder builder = new RejectionMessageBuilder(URI.create(id));

        builder._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        //builder._issued_(CalendarUtil.gregorianNow()); TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done

        if (connectorId != null) {
            connectorId = String.join(
                    IdsIdParser.DELIMITER,
                    IdsIdParser.SCHEME,
                    IdsType.CONNECTOR.getValue(),
                    connectorId);

            URI connectorIdUri = URI.create(connectorId);

            builder._issuerConnector_(connectorIdUri);
            builder._senderAgent_(connectorIdUri);
        }

        if (correlationMessage != null) {
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
        }

        return builder;
    }
}
