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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractRequest;
import de.fraunhofer.iais.eis.ContractRequestMessage;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ResponseMessageUtil;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;

/**
 * This class handles and processes incoming IDS {@link ContractRequestMessage}s.
 */
public class ContractRequestHandler implements Handler {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final String connectorId;
    private final ConsumerContractNegotiationManager negotiationManager;

    public ContractRequestHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ObjectMapper objectMapper,
            @NotNull ConsumerContractNegotiationManager negotiationManager) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.negotiationManager = Objects.requireNonNull(negotiationManager);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractRequestMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull VerificationResult verificationResult) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(verificationResult);

        var message = (ContractRequestMessage) multipartRequest.getHeader();

        ContractRequest contractRequest = null;
        try {
            contractRequest = objectMapper.readValue(multipartRequest.getPayload(), ContractRequest.class);
        } catch (IOException e) {
            monitor.debug("ContractRequestHandler: Contract Request is invalid");
            return createBadParametersErrorMultipartResponse(message);
        }

        var correlationId = message.getTransferContract();
        // TODO negotiationManager.initiate(C);
        // Create contract offer request
        // Start negotiation process: ProviderContractNegotiationManagerImpl.requested

        return MultipartResponse.Builder.newInstance()
                .header(ResponseMessageUtil.createRequestInProcessMessage(connectorId, message))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }
}
