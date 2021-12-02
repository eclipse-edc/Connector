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
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;

public class ArtifactRequestHandler implements Handler {

    private final TransferProcessManager transferProcessManager;
    private final String connectorId;
    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final ContractValidationService contractValidationService;
    private final ContractDefinitionStore contractDefinitionStore;
    private final Vault vault;

    public ArtifactRequestHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ObjectMapper objectMapper,
            @NotNull ContractDefinitionStore contractDefinitionStore,
            @NotNull ContractValidationService contractValidationService,
            @NotNull TransferProcessManager transferProcessManager,
            @NotNull Vault vault) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.contractDefinitionStore = Objects.requireNonNull(contractDefinitionStore);
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
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull VerificationResult verificationResult) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(verificationResult);

        ArtifactRequestMessage artifactRequestMessage = (ArtifactRequestMessage) multipartRequest.getHeader();

        URI artifactUri = artifactRequestMessage.getRequestedArtifact();
        IdsId artifactIdsId = IdsIdParser.parse(artifactUri.toString());
        if (artifactIdsId.getType() != IdsType.ARTIFACT) {
            monitor.info("ArtifactRequestHandler: Requested artifact URI not of type artifact.");
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        // NOTICE
        //
        // Please note that the code below is just a workaround until the contract negotiation and the corresponding
        // contracts are in place. Until then this workaround makes it possible to initiate a data transfer
        // based on a valid contract offer.

        URI contractUri = artifactRequestMessage.getTransferContract();
        IdsId contractIdsId = IdsIdParser.parse(contractUri.toString());
        if (contractIdsId.getType() != IdsType.CONTRACT) {
            monitor.info("ArtifactRequestHandler: Requested artifact URI not of type contract.");
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }
        Optional<ContractDefinition> contractDefinition = contractDefinitionStore.findAll().stream().filter(d -> d.getId().equals(contractIdsId.getValue())).findFirst();

        if (contractDefinition.isEmpty()) {
            monitor.info(String.format("ArtifactRequestHandler: No Contract Offer with ID %s found.", contractIdsId.getValue()));
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        ContractAgreement contractAgreement = ContractAgreement.Builder.newInstance()
                .id(contractDefinition.get().getId() + ":" + UUID.randomUUID())
                .asset(Asset.Builder.newInstance().id(artifactIdsId.getValue()).build())
                .policy(contractDefinition.get().getContractPolicy())
                .contractEndDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(Instant.now().getEpochSecond() + 60 * 5), ZoneId.of("UTC")) /* Five Minutes */)
                .contractSigningDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(Instant.now().getEpochSecond() - 60 * 5), ZoneId.of("UTC")) /* Five Minutes */)
                .contractStartDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(Instant.now().getEpochSecond() - 60 * 5), ZoneId.of("UTC")) /* Five Minutes */)
                .consumerAgentId(URI.create("https://example.com"))
                .providerAgentId(URI.create("https://example.com"))
                .build();

        // TODO Assert that the Asset is part of the contract

        boolean isContractValid = contractValidationService.validate(verificationResult.token(), contractAgreement);
        if (!isContractValid) {
            monitor.info("ArtifactRequestHandler: Contract Validation Invalid");
            return createBadParametersErrorMultipartResponse(multipartRequest.getHeader());
        }

        ArtifactRequestMessagePayload artifactRequestMessagePayload = null;
        try {
            artifactRequestMessagePayload =
                    objectMapper.readValue(multipartRequest.getPayload(), ArtifactRequestMessagePayload.class);
        } catch (IOException e) {
            return createBadParametersErrorMultipartResponse(artifactRequestMessage);
        }

        DataAddress dataAddress = artifactRequestMessagePayload.getDataDestination();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol(Protocols.IDS_MULTIPART)
                .dataDestination(dataAddress)
                .connectorId(connectorId)
                .assetId(artifactIdsId.getValue())
                .contractId(contractIdsId.getValue())
                .connectorAddress(artifactRequestMessage.getSenderAgent().toString() + "/api/ids/multipart") // TODO Is this correct?
                .build();

        transferProcessManager.initiateProviderRequest(dataRequest);

        if (artifactRequestMessagePayload.getSecret() != null) {
            vault.storeSecret(dataAddress.getKeyName(), artifactRequestMessagePayload.getSecret());
        }

        return MultipartResponse.Builder.newInstance()
                .header(ResponseMessageUtil.createDummyResponse(connectorId, artifactRequestMessage)) // TODO Change this response so that it matches our UML pictures
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }

}
