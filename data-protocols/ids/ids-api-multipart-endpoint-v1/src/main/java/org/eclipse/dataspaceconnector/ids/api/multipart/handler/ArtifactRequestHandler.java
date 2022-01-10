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
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
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
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;

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

        ArtifactRequestMessage artifactRequestMessage = (ArtifactRequestMessage) multipartRequest.getHeader();

        URI artifactUri = artifactRequestMessage.getRequestedArtifact();
        IdsId artifactIdsId = IdsIdParser.parse(artifactUri.toString());
        if (artifactIdsId.getType() != IdsType.ARTIFACT) {
            monitor.info("ArtifactRequestHandler: Requested artifact URI not of type artifact.");
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        URI contractUri = artifactRequestMessage.getTransferContract();
        IdsId contractIdsId = IdsIdParser.parse(contractUri.toString());
        if (contractIdsId.getType() != IdsType.CONTRACT) {
            monitor.info("ArtifactRequestHandler: Requested artifact URI not of type contract.");
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        ContractAgreement contractAgreement = contractNegotiationStore.findContractAgreement(contractIdsId.getValue());
        if (contractAgreement == null) {
            monitor.info(String.format("ArtifactRequestHandler: No Contract Agreement with Id %s found.", contractIdsId.getValue()));
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        boolean isContractValid = contractValidationService.validate(verificationResult.getContent(), contractAgreement);
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

        DataAddress dataAddress = artifactRequestMessagePayload.getDataDestination();


        var props = new HashMap<String, String>();
        if (artifactRequestMessage.getProperties() != null) {
            artifactRequestMessage.getProperties().forEach((k, v) -> props.put(k, v.toString()));
        }

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol(Protocols.IDS_MULTIPART)
                .dataDestination(dataAddress)
                .connectorId(connectorId)
                .assetId(artifactIdsId.getValue())
                .contractId(contractIdsId.getValue())
                .properties(props)
                .connectorAddress(artifactRequestMessage.getSenderAgent().toString() + "/api/ids/multipart") // TODO Is this correct?
                .build();

        var result = transferProcessManager.initiateProviderRequest(dataRequest);

        if (artifactRequestMessagePayload.getSecret() != null) {
            vault.storeSecret(dataAddress.getKeyName(), artifactRequestMessagePayload.getSecret());
        }

        var multipartResponse = MultipartResponse.Builder.newInstance()
                .header(ResponseMessageUtil.createDummyResponse(connectorId, artifactRequestMessage)) // TODO Change this response so that it matches our UML pictures
                .payload(result.getData())
                .build();
        return multipartResponse;
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }

}
