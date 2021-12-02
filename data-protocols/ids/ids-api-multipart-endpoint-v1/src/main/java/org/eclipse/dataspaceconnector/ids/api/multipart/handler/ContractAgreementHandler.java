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
import de.fraunhofer.iais.eis.ContractAgreementMessage;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResponse;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
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
    private final ConsumerContractNegotiationManager negotiationManager;
    private final TransformerRegistry transformerRegistry;
    private final AssetIndex assetIndex;

    public ContractAgreementHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ObjectMapper objectMapper,
            @NotNull ConsumerContractNegotiationManager negotiationManager,
            @NotNull TransformerRegistry transformerRegistry,
            @NotNull AssetIndex assetIndex) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.negotiationManager = Objects.requireNonNull(negotiationManager);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.assetIndex = Objects.requireNonNull(assetIndex);
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

        de.fraunhofer.iais.eis.ContractAgreement contractAgreement;
        try {
            contractAgreement = objectMapper.readValue(multipartRequest.getPayload(), de.fraunhofer.iais.eis.ContractAgreement.class);
        } catch (IOException e) {
            monitor.severe("ContractAgreementHandler: Contract Agreement is invalid", e);
            return createBadParametersErrorMultipartResponse(message);
        }

        // extract target from contract request
        var permission = contractAgreement.getPermission().get(0);
        if (permission == null) {
            monitor.debug("ContractAgreementHandler: Contract Agreement is invalid");
            return createBadParametersErrorMultipartResponse(message);
        }

        var target = permission.getTarget();
        if (target == null || String.valueOf(target).isBlank()) {
            monitor.debug("ContractAgreementHandler: Contract Agreement is invalid");
            return createBadParametersErrorMultipartResponse(message);
        }

        // search for matching asset
        var assetId = IdsIdParser.parse(String.valueOf(target));
        var asset = assetIndex.findById(assetId.getValue());
        if (asset == null) {
            var msg = "Target id is invalid";
            monitor.debug(String.format("ContractAgreementHandler: %s", msg));
            return createBadParametersErrorMultipartResponse(message, msg);
        }

        // Create contract offer request
        var input = ContractTransformerInput.Builder.newInstance()
                .contract(contractAgreement)
                .asset(asset)
                .build();

        // Create contract agreement
        TransformResult<ContractAgreement> result = transformerRegistry.transform(input, ContractAgreement.class);
        if (result.hasProblems()) {
            monitor.debug(String.format("Could not transform contract agreement: [%s]",
                    String.join(", ", result.getProblems())));
            return createBadParametersErrorMultipartResponse(message);
        }

        // TODO get hash from message
        var agreement = result.getOutput();
        var processId = message.getTransferContract();
        var negotiationResponse = negotiationManager.confirmed(verificationResult.token(),
                String.valueOf(processId), agreement, null);
        if (negotiationResponse.getStatus() == NegotiationResponse.Status.FATAL_ERROR) {
            monitor.debug("ContractAgreementHandler: Could not process contract agreement");
            return createBadParametersErrorMultipartResponse(message);
        }

        return MultipartResponse.Builder.newInstance()
                .header(ResponseMessageUtil.createMessageProcessedNotificationMessage(connectorId, message))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message, String payload) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .payload(payload)
                .build();
    }
}
