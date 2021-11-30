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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementMessage;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ResponseMessageUtil;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;

/**
 * This class handles and processes incoming IDS {@link ContractAgreementMessage}s.
 */
public class ContractAgreementHandler implements Handler {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final String connectorId;

    public ContractAgreementHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ObjectMapper objectMapper) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractAgreementMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull VerificationResult verificationResult) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(verificationResult);

        var message = (ContractAgreementMessage) multipartRequest.getHeader();

        ContractAgreement contractAgreement = null;
        try {
            contractAgreement = objectMapper.readValue(multipartRequest.getPayload(), ContractAgreement.class);
        } catch (IOException e) {
            monitor.debug("ContractAgreementHandler: Contract Agreement is invalid");
            return createBadParametersErrorMultipartResponse(message);
        }

        // TODO
        // var correlationId = contractOfferMessage.getTransferContract();
        // Create contract agreement
        // Start negotiation process: ProviderContractNegotiationManagerImpl.confirmed

        return MultipartResponse.Builder.newInstance()
                .header(ResponseMessageUtil.createMessageProcessedNotificationMessage(connectorId, message))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }
}
