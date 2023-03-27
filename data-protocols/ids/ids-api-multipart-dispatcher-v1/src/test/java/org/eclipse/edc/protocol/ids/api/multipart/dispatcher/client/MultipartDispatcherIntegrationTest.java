/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher.client;

import de.fraunhofer.iais.eis.Action;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.BaseConnectorBuilder;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.ContractOfferBuilder;
import de.fraunhofer.iais.eis.PermissionBuilder;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRejection;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.ids.api.multipart.controller.MultipartController;
import org.eclipse.edc.protocol.ids.spi.transform.ContractAgreementTransformerOutput;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.edc.protocol.ids.spi.types.MessageProtocol;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.metadata.MetadataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//@Disabled("needs to be rewritten!!")
@ComponentTest
@ExtendWith(EdcExtension.class)
class MultipartDispatcherIntegrationTest {
    private static final int PORT = getFreePort();
    private static final int IDS_PORT = getFreePort();
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();

    private final IdsTransformerRegistry transformerRegistry = mock(IdsTransformerRegistry.class);
    private final ContractNegotiationStore negotiationStore = mock(ContractNegotiationStore.class);
    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final ConsumerContractNegotiationManager consumerContractNegotiationManager = mock(ConsumerContractNegotiationManager.class);

    @BeforeEach
    void init(EdcExtension extension) {
        extension.registerSystemExtension(ServiceExtension.class, new TestExtension());

        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(PORT),
                "web.http.path", "/api",
                "web.http.ids.port", String.valueOf(IDS_PORT),
                "web.http.ids.path", "/api/v1/ids",
                "edc.ids.id", "urn:connector:" + CONNECTOR_ID,
                "ids.webhook.address", "http://webhook"
        ));

        extension.registerServiceMock(IdsTransformerRegistry.class, transformerRegistry);
        extension.registerServiceMock(ContractNegotiationStore.class, negotiationStore);
        extension.registerServiceMock(ContractValidationService.class, validationService);
        extension.registerServiceMock(ConsumerContractNegotiationManager.class, consumerContractNegotiationManager);
    }

    @Test
    void testSendDescriptionRequestMessage(RemoteMessageDispatcherRegistry dispatcher) {
        var request = MetadataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .build();
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(getBaseConnector()));

        var future = dispatcher.send(BaseConnector.class, request);

        assertThat(future).succeedsWithin(5, SECONDS).isInstanceOf(BaseConnector.class);
    }

    @Test
    void testSendArtifactRequestMessage(RemoteMessageDispatcherRegistry dispatcher) {
        var asset = Asset.Builder.newInstance().id("1").build();
        when(negotiationStore.findContractAgreement(any())).thenReturn(ContractAgreement.Builder.newInstance()
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .assetId("1")
                .policy(Policy.Builder.newInstance().build())
                .contractSigningDate(Instant.now().getEpochSecond())
                .contractStartDate(Instant.now().getEpochSecond())
                .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .id("1:2").build());
        when(validationService.validateAgreement(any(), any(ContractAgreement.class))).thenReturn(Result.success(null));

        var request = TransferRequestMessage.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .contractId("1")
                .assetId(asset.getId())
                .dataDestination(DataAddress.Builder.newInstance().type("test-type").build())
                .build();

        var future = dispatcher.send(null, request);

        assertThat(future).succeedsWithin(5, SECONDS);
    }

    @Test
    void testSendContractOfferMessage(RemoteMessageDispatcherRegistry dispatcher) {
        var contractOffer = contractOffer("id:someId");
        when(transformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(getIdsContractOffer()));

        var request = ContractOfferRequest.Builder.newInstance()
                .type(ContractOfferRequest.Type.COUNTER_OFFER)
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .contractOffer(contractOffer)
                .correlationId("1")
                .build();

        var future = dispatcher.send(null, request);

        assertThat(future).failsWithin(5, SECONDS); // ContractOfferMessageImpl is not supported by MultipartController
        verify(transformerRegistry).transform(any(), any());
    }

    @Disabled("Proper test data and setup is needed")
    @Test
    void testSendContractRequestMessage(RemoteMessageDispatcherRegistry dispatcher, AssetIndex assetIndex) {
        var contractOffer = contractOffer("id:someId");
        assetIndex.accept(Asset.Builder.newInstance().id("1").build(), DataAddress.Builder.newInstance().type("any").build());
        when(transformerRegistry.transform(any(), eq(de.fraunhofer.iais.eis.ContractOffer.class))).thenReturn(Result.success(getIdsContractOffer()));
        when(transformerRegistry.transform(any(), eq(ContractOffer.class))).thenReturn(Result.success(contractOffer));
        when(validationService.validateInitialOffer(any(), any())).thenReturn(Result.success(contractOffer));

        var request = ContractOfferRequest.Builder.newInstance()
                .type(ContractOfferRequest.Type.INITIAL)
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .contractOffer(contractOffer)
                .correlationId("1")
                .build();

        var future = dispatcher.send(null, request);

        assertThat(future).succeedsWithin(5, SECONDS);
        verify(transformerRegistry).transform(any(), eq(de.fraunhofer.iais.eis.ContractOffer.class));
        verify(transformerRegistry).transform(any(), eq(ContractOffer.class));
    }

    @Test
    void testSendContractAgreementMessage(RemoteMessageDispatcherRegistry dispatcher) {
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .id("1:23456").consumerAgentId("consumer").providerAgentId("provider")
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString())
                .build();
        when(transformerRegistry.transform(any(), eq(de.fraunhofer.iais.eis.ContractAgreement.class)))
                .thenReturn(Result.success(getIdsContractAgreement()));
        when(transformerRegistry.transform(any(), eq(ContractAgreementTransformerOutput.class)))
                .thenReturn(Result.success(ContractAgreementTransformerOutput.Builder.newInstance().build()));
        when(consumerContractNegotiationManager.confirmed(any(), any(), any(), any())).thenReturn(StatusResult.success(createContractNegotiation("negotiationId")));

        var request = ContractAgreementRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .contractAgreement(contractAgreement)
                .correlationId("1")
                .policy(Policy.Builder.newInstance().build())
                .build();

        var future = dispatcher.send(null, request);

        assertThat(future).succeedsWithin(5, SECONDS);
        verify(transformerRegistry, times(2)).transform(any(), any());
    }

    @Disabled("Proper test data and setup is needed")
    @Test
    void testSendContractRejectionMessage(RemoteMessageDispatcherRegistry dispatcher) {
        when(consumerContractNegotiationManager.declined(any(), any())).thenReturn(StatusResult.success(createContractNegotiation("negotiationId")));
        var rejection = ContractRejection.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .rejectionReason("Modified policy in contract offer.")
                .correlationId(UUID.randomUUID().toString())
                .build();

        var future = dispatcher.send(null, rejection);

        assertThat(future).succeedsWithin(5, SECONDS);
    }

    protected String getUrl() {
        return String.format("http://localhost:%s/api/v1/ids%s", IDS_PORT, MultipartController.PATH);
    }

    protected ContractOffer contractOffer(String id) {
        return ContractOffer.Builder.newInstance()
                .id(id)
                .consumer(URI.create("consumer"))
                .provider(URI.create("provider"))
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id("test-asset").build())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now().plusMonths(1))
                .build();
    }

    private de.fraunhofer.iais.eis.ContractOffer getIdsContractOffer() {
        return new ContractOfferBuilder(URI.create("urn:contractoffer:1"))
                ._consumer_(URI.create("consumer"))
                ._provider_(URI.create("provider"))
                ._permission_(new PermissionBuilder()
                        ._action_(Action.USE)
                        ._target_(URI.create("urn:artifact:1"))
                        .build())
                .build();
    }

    private de.fraunhofer.iais.eis.ContractAgreement getIdsContractAgreement() {
        return new ContractAgreementBuilder(URI.create("urn:contractagreement:1"))
                ._consumer_(URI.create("consumer"))
                ._provider_(URI.create("provider"))
                ._contractDate_(CalendarUtil.gregorianNow())
                ._contractEnd_(CalendarUtil.gregorianNow())
                ._contractStart_(CalendarUtil.gregorianNow())
                ._permission_(new PermissionBuilder()
                        ._action_(Action.USE)
                        .build())
                .build();
    }

    private BaseConnector getBaseConnector() {
        return new BaseConnectorBuilder()
                .build();
    }

    private ContractNegotiation createContractNegotiation(String id) {
        return ContractNegotiation.Builder.newInstance()
                .id(id)
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol(MessageProtocol.IDS_MULTIPART)
                .build();
    }

    public static class TestExtension implements ServiceExtension {
        @Provider
        public IdentityService identityService() {
            var identityService = mock(IdentityService.class);
            var tokenResult = TokenRepresentation.Builder.newInstance().token("token").build();
            var claimToken = ClaimToken.Builder.newInstance().claim("key", "value").claim("client_id", "some-client").build();
            when(identityService.obtainClientCredentials(any())).thenReturn(Result.success(tokenResult));
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            return identityService;
        }
    }
}
