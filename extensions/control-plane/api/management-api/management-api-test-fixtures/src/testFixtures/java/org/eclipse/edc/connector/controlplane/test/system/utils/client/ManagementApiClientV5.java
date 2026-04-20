/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils.client;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.authentication.OauthTokenProvider;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.AssetsApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.CatalogApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.CelExpressionApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.ContractDefApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.ContractNegotiationApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.DataPlaneApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.ParticipantContextApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.ParticipantContextConfigApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.PolicyDefApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.TransferApi;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.AssetDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ContractDefinitionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ContractNegotiationDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ContractRequestDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.CriterionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DatasetDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DatasetRequestDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.OfferDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ParticipantContextConfigDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ParticipantContextDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PolicyDefinitionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.QuerySpectDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.TransferRequestDto;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.utils.LazySupplier;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class ManagementApiClientV5 {

    protected static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String PROTOCOL = "dataspace-protocol-http:2025-1";
    private static final JsonLdNamespace NS = new JsonLdNamespace(EDC_NAMESPACE);
    private final OauthTokenProvider oauthServer;
    private final LazySupplier<URI> managementEndpoint;

    private final AssetsApi assets;
    private final PolicyDefApi policies;
    private final ContractDefApi contractDefinitions;
    private final ParticipantContextApi participantContexts;
    private final ParticipantContextConfigApi participantConfigurations;
    private final CatalogApi catalogs;
    private final ContractNegotiationApi negotiations;
    private final TransferApi transfers;
    private final CelExpressionApi expressions;
    private final DataPlaneApi dataplanes;

    public ManagementApiClientV5(OauthTokenProvider oauthServer,
                                 LazySupplier<URI> managementEndpoint) {
        this.oauthServer = oauthServer;
        this.managementEndpoint = managementEndpoint;
        this.assets = new AssetsApi(this);
        this.policies = new PolicyDefApi(this);
        this.contractDefinitions = new ContractDefApi(this);
        this.participantContexts = new ParticipantContextApi(this);
        this.participantConfigurations = new ParticipantContextConfigApi(this);
        this.catalogs = new CatalogApi(this);
        this.negotiations = new ContractNegotiationApi(this);
        this.transfers = new TransferApi(this);
        this.expressions = new CelExpressionApi(this);
        this.dataplanes = new DataPlaneApi(this);
    }

    public static ManagementApiClientV5 forContext(ComponentRuntimeContext ctx, OauthTokenProvider authServer) {
        return new ManagementApiClientV5(
                authServer,
                ctx.getEndpoint("management")
        );
    }

    public AssetsApi assets() {
        return assets;
    }

    public PolicyDefApi policies() {
        return policies;
    }

    public ContractDefApi contractDefinitions() {
        return contractDefinitions;
    }

    public ParticipantContextApi participantContexts() {
        return participantContexts;
    }

    public ParticipantContextConfigApi participantConfigurations() {
        return participantConfigurations;
    }

    public CatalogApi catalogs() {
        return catalogs;
    }

    public ContractNegotiationApi negotiations() {
        return negotiations;
    }

    public TransferApi transfers() {
        return transfers;
    }

    public CelExpressionApi expressions() {
        return expressions;
    }

    public DataPlaneApi dataplanes() {
        return dataplanes;
    }

    private String startTransferProcess(String participantContext, String contractAgreementId, String providerAddress, String transferType) {
        var request = new TransferRequestDto(PROTOCOL, providerAddress, transferType, contractAgreementId);
        var transferId = transfers.initTransfer(participantContext, request);

        waitTransferInState(participantContext, transferId, STARTED);

        return transferId;

    }

    public void waitTransferInState(String participantContextId, String transferId, TransferProcessStates state) {
        await().atMost(TIMEOUT).untilAsserted(() -> {
            var currentState = transfers.getState(participantContextId, transferId);
            assertThat(currentState).isEqualTo(state.name());
        });
    }

    public String initContractNegotiation(String participantContext, String assetId, String providerAddress, String providerId) {

        try {
            var dataset = fetchDataset(participantContext, assetId, providerAddress, providerId);
            var offer = dataset.offers().stream().findFirst().orElseThrow();

            return initContractNegotiation(participantContext, assetId, offer, providerAddress, providerId);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DatasetDto fetchDataset(String participantContext, String assetId, String providerAddress, String providerId) {
        return catalogs.getDataset(participantContext, new DatasetRequestDto(assetId, PROTOCOL, providerAddress, providerId));
    }

    public void waitForContractNegotiationState(String participantContextId, String negotiationId, String state) {
        await().atMost(TIMEOUT).untilAsserted(() -> {
            var currentState = negotiations.getState(participantContextId, negotiationId);
            assertThat(currentState).isEqualTo(state);
        });
    }

    public String getNegotiationError(String participantContextId, String negotiationId) {
        var contractNegotiation = negotiations.getNegotiation(participantContextId, negotiationId);
        return Optional.of(contractNegotiation).map(ContractNegotiationDto::getErrorDetail)
                .orElse(null);
    }

    public String startContractNegotiation(String participantContext, String providerContextId, String assetId, OfferDto offerDto, String providerAddress, String providerId) {
        var negotiationId = initContractNegotiation(participantContext, assetId, offerDto, providerAddress, providerId);


        waitForContractNegotiationState(participantContext, negotiationId, FINALIZED.name());

        await().atMost(TIMEOUT).untilAsserted(() -> {
            var query = new QuerySpectDto(List.of(new CriterionDto("correlationId", "=", negotiationId)));
            var state = negotiations.search(providerContextId, query).stream()
                    .map(ContractNegotiationDto::getState)
                    .findFirst();

            assertThat(state).isEqualTo(Optional.of(FINALIZED.name()));
        });

        return negotiations.getNegotiation(participantContext, negotiationId).getContractAgreementId();

    }

    public String initContractNegotiation(String participantContext, String assetId, OfferDto offerDto, String providerAddress, String providerId) {
        var offer = new OfferDto(offerDto.getId(), providerId, assetId, offerDto.getPermissions());

        var request = new ContractRequestDto(PROTOCOL, providerAddress, providerId, offer);

        return negotiations.initContractNegotiation(participantContext, request);

    }

    public String startTransfer(String participantContext, String providerContextId, String providerAddress, String providerId, String assetId, String transferType) {
        try {
            var dataset = fetchDataset(participantContext, assetId, providerAddress, providerId);
            var offer = dataset.offers().stream().findFirst().orElseThrow();

            var agreementId = startContractNegotiation(participantContext, providerContextId, assetId, offer, providerAddress, providerId);
            return startTransferProcess(participantContext, agreementId, providerAddress, transferType);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String setupResources(String participantContext, AssetDto asset, PolicyDefinitionDto accessPolicy, PolicyDefinitionDto contractPolicy) {

        var assetId = assets.createAsset(participantContext, asset);

        var accessPolicyId = policies.createPolicyDefinition(participantContext, accessPolicy);
        var contractPolicyId = policies.createPolicyDefinition(participantContext, contractPolicy);

        var selector = List.of(new CriterionDto(NS.toIri("id"), "=", assetId));
        var contractDefinition = new ContractDefinitionDto(accessPolicyId, contractPolicyId, selector);
        contractDefinitions.createContractDefinition(participantContext, contractDefinition);

        return assetId;
    }


    public void createParticipant(String participantContextId, String participantId, Map<String, String> cfg) {

        participantContexts.createParticipant(new ParticipantContextDto(participantContextId, participantId));

        // to remove once it's not needed for iam mock
        var configuration = new HashMap<>(cfg);
        configuration.put("edc.participant.id", participantId);
        participantConfigurations.saveConfig(participantContextId, new ParticipantContextConfigDto(configuration));

    }

    public RequestSpecification baseManagementRequest(String participantContextId) {
        if (participantContextId == null) {
            return baseManagementRequest(null, ParticipantPrincipal.ROLE_ADMIN);
        } else {
            return baseManagementRequest(participantContextId, ParticipantPrincipal.ROLE_PARTICIPANT);
        }
    }

    public RequestSpecification baseManagementRequest(String participantContextId, String role) {
        var token = oauthServer.createToken(participantContextId, role);
        return given().baseUri(managementEndpoint.get().toString())
                .header("Authorization", "Bearer " + token);
    }
}
