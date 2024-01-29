/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Masatake Iwasaki (NTT DATA) - fixed failure due to assertion timeout
 *       SAP SE - refactoring
 *
 */

package org.eclipse.edc.connector.service.transferprocess;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessDeprovisioned;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessProvisioned;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.connector.transfer.spi.retry.TransferWaitStrategy;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.matchers.EventEnvelopeMatcher.isEnvelopeOf;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class TransferProcessEventDispatchTest {

    public static final Duration TIMEOUT = Duration.ofSeconds(30);
    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);
    private final IdentityService identityService = mock();

    @NotNull
    private static RemoteMessageDispatcher getTestDispatcher() {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.dispatch(any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
        return testDispatcher;
    }

    @BeforeEach
    void setUp(EdcExtension extension) {
        var configuration = Map.of(
                "edc.transfer.send.retry.limit", "0",
                "edc.transfer.send.retry.base-delay.ms", "0",
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api"
        );

        extension.setConfiguration(configuration);
        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
        extension.registerServiceMock(EventExecutorServiceContainer.class, new EventExecutorServiceContainer(newSingleThreadExecutor()));
        extension.registerServiceMock(IdentityService.class, identityService);
        extension.registerServiceMock(ProtocolWebhook.class, () -> "http://dummy");
        extension.registerServiceMock(DataPlaneInstanceStore.class, mock(DataPlaneInstanceStore.class));
        extension.registerServiceMock(PolicyArchive.class, mock(PolicyArchive.class));
        extension.registerServiceMock(ContractNegotiationStore.class, mock(ContractNegotiationStore.class));
        extension.registerServiceMock(ParticipantAgentService.class, mock(ParticipantAgentService.class));
        var dataAddressValidatorRegistry = mock(DataAddressValidatorRegistry.class);
        when(dataAddressValidatorRegistry.validateSource(any())).thenReturn(ValidationResult.success());
        when(dataAddressValidatorRegistry.validateDestination(any())).thenReturn(ValidationResult.success());
        extension.registerServiceMock(DataAddressValidatorRegistry.class, dataAddressValidatorRegistry);
    }

    @Test
    void shouldDispatchEventsOnTransferProcessStateChanges(TransferProcessService service,
                                                           TransferProcessProtocolService protocolService,
                                                           EventRouter eventRouter,
                                                           RemoteMessageDispatcherRegistry dispatcherRegistry,
                                                           PolicyArchive policyArchive,
                                                           ContractNegotiationStore negotiationStore,
                                                           ParticipantAgentService agentService) {

        var token = ClaimToken.Builder.newInstance().build();
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(UUID.randomUUID().toString()).build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(token));

        var agent = mock(ParticipantAgent.class);
        var agreement = mock(ContractAgreement.class);
        var providerId = "ProviderId";

        when(agreement.getProviderId()).thenReturn(providerId);
        when(agent.getIdentity()).thenReturn(providerId);


        dispatcherRegistry.register(getTestDispatcher());
        when(policyArchive.findPolicyForContract(matches("contractId"))).thenReturn(mock(Policy.class));
        when(negotiationStore.findContractAgreement("contractId")).thenReturn(agreement);
        when(agentService.createFor(token)).thenReturn(agent);
        eventRouter.register(TransferProcessEvent.class, eventSubscriber);

        var transferRequest = createTransferRequest();

        var initiateResult = service.initiateTransfer(transferRequest);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessInitiated.class)));
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessProvisioned.class)));
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessRequested.class)));
        });

        var dataAddress = DataAddress.Builder.newInstance().type("test").build();
        var startMessage = TransferStartMessage.Builder.newInstance()
                .processId("dataRequestId")
                .protocol("any")
                .counterPartyAddress("http://any")
                .dataAddress(dataAddress)
                .build();

        protocolService.notifyStarted(startMessage, tokenRepresentation);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            ArgumentCaptor<EventEnvelope<TransferProcessStarted>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
            verify(eventSubscriber, times(4)).on(captor.capture());
            assertThat(captor.getValue()).isNotNull()
                    .extracting(EventEnvelope::getPayload)
                    .extracting(TransferProcessStarted::getDataAddress)
                    .usingRecursiveComparison().isEqualTo(dataAddress);
        });

        var transferProcess = initiateResult.getContent();
        service.complete(transferProcess.getId()).orElseThrow(f -> new EdcException("Transfer cannot be completed: " + f.getFailureDetail()));

        await().atMost(TIMEOUT)
                .untilAsserted(() -> {
                    verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessCompleted.class)));
                });

        service.deprovision(transferProcess.getId());

        await().atMost(TIMEOUT).untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessDeprovisioned.class)));
        });
    }

    @Test
    void shouldTerminateOnInvalidPolicy(TransferProcessService service, EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        dispatcherRegistry.register(getTestDispatcher());
        eventRouter.register(TransferProcessEvent.class, eventSubscriber);
        var transferRequest = createTransferRequest();

        service.initiateTransfer(transferRequest);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessInitiated.class)));
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessTerminated.class)));
        });
    }

    @Test
    void shouldDispatchEventOnTransferProcessTerminated(TransferProcessService service,
                                                        EventRouter eventRouter,
                                                        RemoteMessageDispatcherRegistry dispatcherRegistry,
                                                        PolicyArchive policyArchive) {

        when(policyArchive.findPolicyForContract(matches("contractId"))).thenReturn(mock(Policy.class));
        dispatcherRegistry.register(getTestDispatcher());
        eventRouter.register(TransferProcessEvent.class, eventSubscriber);
        var transferRequest = createTransferRequest();

        var initiateResult = service.initiateTransfer(transferRequest);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessInitiated.class)));
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessProvisioned.class)));
            verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessRequested.class)));
        });

        service.terminate(new TerminateTransferCommand(initiateResult.getContent().getId(), "any reason"));

        await().atMost(TIMEOUT).untilAsserted(() -> verify(eventSubscriber, atLeastOnce()).on(argThat(isEnvelopeOf(TransferProcessTerminated.class))));
    }

    @Test
    void shouldDispatchEventOnTransferProcessFailure(TransferProcessService service, EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        dispatcherRegistry.register(getTestDispatcher());
        eventRouter.register(TransferProcessEvent.class, eventSubscriber);
        var transferRequest = createTransferRequest();

        service.initiateTransfer(transferRequest);

        await().atMost(TIMEOUT).untilAsserted(() -> verify(eventSubscriber).on(argThat(isEnvelopeOf(TransferProcessTerminated.class))));
    }

    private TransferRequest createTransferRequest() {
        return TransferRequest.Builder.newInstance()
                .id("dataRequestId")
                .assetId("assetId")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .protocol("test")
                .counterPartyAddress("http://an/address")
                .contractId("contractId")
                .build();
    }

}
