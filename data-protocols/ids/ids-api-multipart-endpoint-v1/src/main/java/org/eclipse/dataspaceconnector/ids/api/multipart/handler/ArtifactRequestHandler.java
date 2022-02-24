/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.ids.spi.spec.extension.ArtifactRequestMessagePayload;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;
import static org.eclipse.dataspaceconnector.ids.spi.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

public class ArtifactRequestHandler implements Handler {

    private final TransferProcessManager transferProcessManager;
    private final String connectorId;
    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final ContractValidationService contractValidationService;
    private final ContractNegotiationStore contractNegotiationStore;
    private final Vault vault;

    public ArtifactRequestHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ObjectMapper objectMapper,
            @NotNull ContractNegotiationStore contractNegotiationStore,
            @NotNull ContractValidationService contractValidationService,
            @NotNull TransferProcessManager transferProcessManager,
            @NotNull Vault vault) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.contractNegotiationStore = Objects.requireNonNull(contractNegotiationStore);
        this.contractValidationService = Objects.requireNonNull(contractValidationService);
        this.transferProcessManager = Objects.requireNonNull(transferProcessManager);
        this.vault = Objects.requireNonNull(vault);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ArtifactRequestMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull Result<ClaimToken> verificationResult) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(verificationResult);

        var artifactRequestMessage = (ArtifactRequestMessage) multipartRequest.getHeader();

        var artifactUri = artifactRequestMessage.getRequestedArtifact();
        var artifactIdsId = IdsIdParser.parse(artifactUri.toString());
        if (artifactIdsId.getType() != IdsType.ARTIFACT) {
            monitor.info("ArtifactRequestHandler: Requested artifact URI not of type artifact.");
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        var contractUri = artifactRequestMessage.getTransferContract();
        var contractIdsId = IdsIdParser.parse(contractUri.toString());
        if (contractIdsId.getType() != IdsType.CONTRACT) {
            monitor.info("ArtifactRequestHandler: Requested artifact URI not of type contract.");
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        var contractAgreement = contractNegotiationStore.findContractAgreement(contractIdsId.getValue());
        if (contractAgreement == null) {
            monitor.info(String.format("ArtifactRequestHandler: No Contract Agreement with Id %s found.", contractIdsId.getValue()));
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        var isContractValid = contractValidationService.validate(verificationResult.getContent(), contractAgreement);
        if (!isContractValid) {
            monitor.info("ArtifactRequestHandler: Contract Validation Invalid");
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        ArtifactRequestMessagePayload artifactRequestMessagePayload;
        try {
            artifactRequestMessagePayload =
                    objectMapper.readValue(multipartRequest.getPayload(), ArtifactRequestMessagePayload.class);
        } catch (IOException e) {
            return createBadParametersErrorMultipartResponse(artifactRequestMessage);
        }

        var dataAddress = artifactRequestMessagePayload.getDataDestination();

        Map<String, String> props = new HashMap<>();
        if (artifactRequestMessage.getProperties() != null) {
            artifactRequestMessage.getProperties().forEach((k, v) -> props.put(k, v.toString()));
        }

        String idsWebhookAddress = Optional.ofNullable(props.remove(IDS_WEBHOOK_ADDRESS_PROPERTY))
                .map(Object::toString)
                .orElse(null);
        if (StringUtils.isNullOrBlank(idsWebhookAddress)) {
            var msg = "Ids webhook address is invalid";
            monitor.debug(String.format("%s: %s", getClass().getSimpleName(), msg));
            return createBadParametersErrorMultipartResponse(artifactRequestMessage, msg);
        }

        var dataRequest = DataRequest.Builder.newInstance()
                .id(artifactRequestMessage.getId().toString())
                .protocol(Protocols.IDS_MULTIPART)
                .dataDestination(dataAddress)
                .connectorId(connectorId)
                .assetId(artifactIdsId.getValue())
                .contractId(contractIdsId.getValue())
                .properties(props)
                .connectorAddress(idsWebhookAddress)
                .build();

        var result = transferProcessManager.initiateProviderRequest(dataRequest);

        if (artifactRequestMessagePayload.getSecret() != null) {
            vault.storeSecret(dataAddress.getKeyName(), artifactRequestMessagePayload.getSecret());
        }

        return MultipartResponse.Builder.newInstance()
                .header(ResponseMessageUtil.createDummyResponse(connectorId, artifactRequestMessage)) // TODO Change this response so that it matches our UML pictures
                .payload(result.getData())
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
