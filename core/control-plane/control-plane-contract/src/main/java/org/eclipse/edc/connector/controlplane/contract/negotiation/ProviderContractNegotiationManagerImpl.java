/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - extended method implementation
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - refactor
 *       ZF Friedrichshafen AG - fixed contract validity issue
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractNegotiationAck;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.statemachine.StateMachineManager;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.OFFERING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;

/**
 * Implementation of the {@link ProviderContractNegotiationManager}.
 */
public class ProviderContractNegotiationManagerImpl extends AbstractContractNegotiationManager implements ProviderContractNegotiationManager {

    private ProviderContractNegotiationManagerImpl() {
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processNegotiationsInState(OFFERING, this::processOffering))
                .processor(processNegotiationsInState(REQUESTED, this::processRequested))
                .processor(processNegotiationsInState(ACCEPTED, this::processAccepted))
                .processor(processNegotiationsInState(AGREEING, this::processAgreeing))
                .processor(processNegotiationsInState(VERIFIED, this::processVerified))
                .processor(processNegotiationsInState(FINALIZING, this::processFinalizing))
                .processor(processNegotiationsInState(TERMINATING, this::processTerminating));
    }

    @Override
    protected ContractNegotiation.Type type() {
        return PROVIDER;
    }

    /**
     * Processes {@link ContractNegotiation} in state OFFERING. Tries to send the current offer to the
     * respective consumer. If this succeeds, the ContractNegotiation is transitioned to state OFFERED. Else,
     * it is transitioned to OFFERING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @WithSpan
    private boolean processOffering(ContractNegotiation negotiation) {

        var callbackAddress = protocolWebhookRegistry.resolve(negotiation.getProtocol());

        if (callbackAddress != null) {

            var lastContractOffer = negotiation.getLastContractOffer();
            var offer = ContractOffer.Builder.newInstance().id(lastContractOffer.getId())
                    .policy(lastContractOffer.getPolicy().toBuilder().type(PolicyType.OFFER).build())
                    .assetId(lastContractOffer.getAssetId())
                    .build();

            var messageBuilder = ContractOfferMessage.Builder.newInstance()
                    .contractOffer(offer)
                    .callbackAddress(callbackAddress.url());

            return dispatch(messageBuilder, negotiation, ContractNegotiationAck.class, "[Provider] send offer")
                    .onSuccess(this::transitionToOffered)
                    .onFailure((n, throwable) -> transitionToOffering(n))
                    .onFinalFailure((n, throwable) -> transitionToTerminating(n, format("Failed to send offer to consumer: %s", throwable.getMessage())))
                    .execute();
        } else {
            transitionToTerminated(negotiation, "No callback address found for protocol: %s".formatted(negotiation.getProtocol()));
            return true;
        }
    }

    /**
     * Processes {@link ContractNegotiation} in state REQUESTED. It transitions to AGREEING, because the automatic agreement.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processRequested(ContractNegotiation negotiation) {
        transitionToAgreeing(negotiation);
        return true;
    }

    /**
     * Processes {@link ContractNegotiation} in state ACCEPTED. It transitions to AGREEING.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processAccepted(ContractNegotiation negotiation) {
        transitionToAgreeing(negotiation);
        return true;
    }

    /**
     * Processes {@link ContractNegotiation} in state CONFIRMING. Tries to send a contract agreement to the respective
     * consumer. If this succeeds, the ContractNegotiation is transitioned to state CONFIRMED. Else, it is transitioned
     * to CONFIRMING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @WithSpan
    private boolean processAgreeing(ContractNegotiation negotiation) {
        var agreement = Optional.ofNullable(negotiation.getContractAgreement())
                .orElseGet(() -> {
                    var lastOffer = negotiation.getLastContractOffer();

                    return ContractAgreement.Builder.newInstance()
                            .contractSigningDate(clock.instant().getEpochSecond())
                            .providerId(participantId)
                            .consumerId(negotiation.getCounterPartyId())
                            .policy(lastOffer.getPolicy().toBuilder().type(PolicyType.CONTRACT).build())
                            .assetId(lastOffer.getAssetId())
                            .build();
                });


        var callbackAddress = protocolWebhookRegistry.resolve(negotiation.getProtocol());

        if (callbackAddress != null) {
            var messageBuilder = ContractAgreementMessage.Builder.newInstance()
                    .callbackAddress(callbackAddress.url())
                    .contractAgreement(agreement);

            return dispatch(messageBuilder, negotiation, Object.class, "[Provider] send agreement")
                    .onSuccess((n, result) -> transitionToAgreed(n, agreement))
                    .onFailure((n, throwable) -> transitionToAgreeing(n))
                    .onFinalFailure((n, throwable) -> transitionToTerminating(n, format("Failed to send agreement to consumer: %s", throwable.getMessage())))
                    .execute();
        } else {
            transitionToTerminated(negotiation, "No callback address found for protocol: %s".formatted(negotiation.getProtocol()));
            return true;
        }
    }

    /**
     * Processes {@link ContractNegotiation} in state VERIFIED. It transitions to FINALIZING to make the finalization process start.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processVerified(ContractNegotiation negotiation) {
        transitionToFinalizing(negotiation);
        return true;
    }

    /**
     * Processes {@link ContractNegotiation} in state OFFERING. Tries to send the current offer to the
     * respective consumer. If this succeeds, the ContractNegotiation is transitioned to state OFFERED. Else,
     * it is transitioned to OFFERING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @WithSpan
    private boolean processFinalizing(ContractNegotiation negotiation) {
        var messageBuilder = ContractNegotiationEventMessage.Builder.newInstance()
                .type(ContractNegotiationEventMessage.Type.FINALIZED)
                .policy(negotiation.getContractAgreement().getPolicy());

        return dispatch(messageBuilder, negotiation, Object.class, "[Provider] send finalization")
                .onSuccess((n, result) -> transitionToFinalized(n))
                .onFailure((n, throwable) -> transitionToFinalizing(n))
                .onFinalFailure((n, throwable) -> transitionToTerminating(n, format("Failed to send finalization to consumer: %s", throwable.getMessage())))
                .execute();
    }

    public static class Builder extends AbstractContractNegotiationManager.Builder<ProviderContractNegotiationManagerImpl> {

        private Builder() {
            super(new ProviderContractNegotiationManagerImpl());
        }

        public static Builder newInstance() {
            return new Builder();
        }

    }
}
