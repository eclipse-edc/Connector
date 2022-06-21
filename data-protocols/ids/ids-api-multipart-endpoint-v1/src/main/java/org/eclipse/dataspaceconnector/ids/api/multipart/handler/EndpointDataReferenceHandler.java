/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.ParticipantUpdateMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiver;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.internalRecipientError;

/**
 * Implementation of the {@link Handler} class for handling of {@link EndpointDataReferenceMessage}.
 * Note that we use the {@link ParticipantUpdateMessage} IDS message to convey the {@link EndpointDataReferenceMessage}.
 */
public class EndpointDataReferenceHandler implements Handler {

    private final Monitor monitor;
    private final String connectorId;
    private final EndpointDataReferenceReceiverRegistry receiverRegistry;
    private final EndpointDataReferenceTransformerRegistry transformerRegistry;
    private final TypeManager typeManager;

    public EndpointDataReferenceHandler(@NotNull Monitor monitor,
                                        @NotNull String connectorId,
                                        @NotNull EndpointDataReferenceReceiverRegistry receiverRegistry,
                                        @NotNull EndpointDataReferenceTransformerRegistry transformerRegistry,
                                        @NotNull TypeManager typeManager) {
        this.monitor = monitor;
        this.connectorId = connectorId;
        this.receiverRegistry = receiverRegistry;
        this.transformerRegistry = transformerRegistry;
        this.typeManager = typeManager;
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        return multipartRequest.getHeader() instanceof ParticipantUpdateMessage;
    }

    /**
     * Handling of the request is as follows:
     * - decode {@link EndpointDataReference} from the request payload,
     * - apply a {@link EndpointDataReferenceTransformer} on the previous EDR,
     * - finally apply {@link EndpointDataReferenceReceiver} to the resulting EDR to dispatch it into the consumer environment.
     */
    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull ClaimToken claimToken) {
        var edr = typeManager.readValue(multipartRequest.getPayload(), EndpointDataReference.class);
        var transformationResult = transformerRegistry.transform(edr);
        if (transformationResult.failed()) {
            monitor.severe("EDR transformation failed: " + String.join(", ", transformationResult.getFailureMessages()));
            return createErrorMultipartResponse(multipartRequest);
        }

        var transformedEdr = transformationResult.getContent();

        var receiveResult = receiverRegistry.receiveAll(transformedEdr).join();
        if (receiveResult.failed()) {
            monitor.severe("EDR dispatch failed: " + String.join(", ", receiveResult.getFailureMessages()));
            return createErrorMultipartResponse(multipartRequest);
        }

        return MultipartResponse.Builder.newInstance()
                .header(ResponseMessageUtil.createMessageProcessedNotificationMessage(connectorId, multipartRequest.getHeader()))
                .build();
    }

    private MultipartResponse createErrorMultipartResponse(MultipartRequest request) {
        return MultipartResponse.Builder.newInstance()
                .header(internalRecipientError(request.getHeader(), connectorId))
                .build();
    }
}
