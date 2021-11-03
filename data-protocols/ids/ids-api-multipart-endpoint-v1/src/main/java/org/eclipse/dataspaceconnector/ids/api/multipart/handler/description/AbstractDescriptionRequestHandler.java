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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessageBuilder;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.version.IdsProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.messageTypeNotSupported;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.notFound;

abstract class AbstractDescriptionRequestHandler implements DescriptionRequestHandler {
    protected final TransformerRegistry transformerRegistry;

    public AbstractDescriptionRequestHandler(@NotNull TransformerRegistry transformerRegistry) {
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
    }

    protected MultipartResponse createBadParametersErrorMultipartResponse(@Nullable String connectorId, @Nullable Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }

    protected MultipartResponse createNotFoundErrorMultipartResponse(@Nullable String connectorId, @Nullable Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(notFound(message, connectorId))
                .build();
    }

    protected MultipartResponse createErrorMultipartResponse(@Nullable String connectorId, @Nullable Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageTypeNotSupported(message, connectorId))
                .build();
    }

    protected DescriptionResponseMessage createDescriptionResponseMessage(
            @Nullable String connectorId,
            @Nullable Message correlationMessage) {

        IdsId messageId = IdsId.Builder.newInstance()
                .type(IdsType.MESSAGE)
                .value(UUID.randomUUID().toString())
                .build();

        DescriptionResponseMessageBuilder builder;
        TransformResult<URI> transformResult = transformerRegistry.transform(messageId, URI.class);
        if (transformResult.hasProblems()) {

            // TODO: handle transformer problems
            builder = new DescriptionResponseMessageBuilder();
        } else {
            builder = new DescriptionResponseMessageBuilder(transformResult.getOutput());
        }

        builder._contentVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);
        builder._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION);

        String connectorIdUrn = String.join(
                IdsIdParser.DELIMITER,
                IdsIdParser.SCHEME,
                IdsType.CONNECTOR.getValue(),
                connectorId);

        URI connectorIdUri = URI.create(connectorIdUrn);

        builder._issuerConnector_(connectorIdUri);
        builder._senderAgent_(connectorIdUri);

        builder._issued_(CalendarUtil.gregorianNow());

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
}
