/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.tck.dsp.data;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAccepted;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationOffered;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.tck.dsp.guard.Trigger;
import org.eclipse.edc.tck.dsp.recorder.StepRecorder;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;

/**
 * Assembles data for the TCK scenarios.
 */
public class DataAssembly {
    private static final Set<String> ASSET_IDS = Set.of("ACN0101", "ACN0102", "ACN0103", "ACN0104",
            "ACN0201", "ACN0202", "ACN0203", "ACN0204", "ACN0205", "ACN0206", "ACN0207",
            "ACN0301", "ACN0302", "ACN0303", "ACN0304");


    private static final Set<String> AGREEMENT_IDS = Set.of("ATP0101", "ATPC0101");

    private static final String POLICY_ID = "P123";
    private static final String CONTRACT_DEFINITION_ID = "CD123";

    private DataAssembly() {
    }

    public static Set<Asset> createAssets() {
        var assets = ASSET_IDS.stream().map(DataAssembly::createAsset).collect(toSet());

        assets.add(Asset.Builder.newInstance().id("ATP0101")
                .dataAddress(DataAddress.Builder.newInstance().type("HttpData").build()).build());
        return assets;
    }

    public static Set<PolicyDefinition> createPolicyDefinitions() {
        var permission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("http://www.w3.org/ns/odrl/2/use").build())
                .build();
        return Set.of(PolicyDefinition.Builder.newInstance()
                .id(POLICY_ID)
                .policy(Policy.Builder.newInstance().permission(permission).build())
                .build());
    }

    public static Set<ContractDefinition> createContractDefinitions() {
        return Set.of(ContractDefinition.Builder.newInstance()
                .id(CONTRACT_DEFINITION_ID)
                .accessPolicyId(POLICY_ID)
                .contractPolicyId(POLICY_ID)
                .build());
    }


    public static Set<ContractNegotiation> createContractNegotiations() {
        return AGREEMENT_IDS.stream().map(DataAssembly::createContractNegotiation).collect(toSet());
    }

    public static StepRecorder<ContractNegotiation> createNegotiationRecorder() {
        var recorder = new StepRecorder<ContractNegotiation>();

        record01NegotiationSequences(recorder);
        record02NegotiationSequences(recorder);
        record03NegotiationSequences(recorder);

        recordC01NegotiationSequences(recorder);

        return recorder.repeat();
    }

    private static void recordC01NegotiationSequences(StepRecorder<ContractNegotiation> recorder) {
    }

    private static void record01NegotiationSequences(StepRecorder<ContractNegotiation> recorder) {
        recorder.record("ACN0101", ContractNegotiation::transitionOffering);

        recorder.record("ACN0102", ContractNegotiation::transitionOffering)
                .record("ACN0102", ContractNegotiation::transitionTerminating);

        recorder.record("ACN0103", ContractNegotiation::transitionOffering)
                .record("ACN0103", ContractNegotiation::transitionAgreeing)
                .record("ACN0103", ContractNegotiation::transitionFinalizing);

        recorder.record("ACN0104", ContractNegotiation::transitionAgreeing)
                .record("ACN0104", ContractNegotiation::transitionFinalizing);
    }

    private static void record02NegotiationSequences(StepRecorder<ContractNegotiation> recorder) {
        recorder.record("ACN0201", ContractNegotiation::transitionTerminating);

        recorder.record("ACN0202", ContractNegotiation::transitionRequested);

        recorder.record("ACN0203", ContractNegotiation::transitionAgreeing);

        recorder.record("ACN0204", ContractNegotiation::transitionOffering);

        recorder.record("ACN0205", ContractNegotiation::transitionOffering);

        recorder.record("ACN0206", contractNegotiation -> {
            // only transition if in requested
            if (contractNegotiation.getState() == REQUESTED.code()) {
                contractNegotiation.transitionOffering();
            }
        });

        recorder.record("ACN0207", ContractNegotiation::transitionAgreeing)
                .record("ACN0207", ContractNegotiation::transitionTerminating);
    }

    private static void record03NegotiationSequences(StepRecorder<ContractNegotiation> recorder) {
        recorder.record("ACN0301", ContractNegotiation::transitionAgreeing)
                .record("ACN0301", ContractNegotiation::transitionFinalizing);

        recorder.record("ACN0302", ContractNegotiation::transitionOffering);
        recorder.record("ACN0303", ContractNegotiation::transitionOffering)
                // TODO hack for making the process sleep once 03 tests are supported this can be removed
                .record("ACN0303", ContractNegotiation::transitionTerminating);


        recorder.record("ACN0304", ContractNegotiation::transitionOffering);
    }

    public static List<Trigger<ContractNegotiation>> createNegotiationTriggers() {
        return List.of(
                createTrigger(ContractNegotiationOffered.class, "ACN0205", ContractNegotiation::transitionTerminating),
                createTrigger(ContractNegotiationAccepted.class, "ACN0206", ContractNegotiation::transitionTerminating),
                createTrigger(ContractNegotiationOffered.class, "C0101", contractNegotiation -> {
                    contractNegotiation.transitionAccepting();
                    contractNegotiation.setPending(false);
                }),
                createTrigger(ContractNegotiationAgreed.class, "C0101", contractNegotiation -> {
                    contractNegotiation.transitionVerifying();
                    contractNegotiation.setPending(false);
                })

        );
    }

    public static List<Trigger<TransferProcess>> createTransferProcessTriggers() {
        return List.of(
                createTransferTrigger(TransferProcessStarted.class, "ATP0101", TransferProcess::transitionTerminating)
        );
    }


    private static <E extends ContractNegotiationEvent> Trigger<ContractNegotiation> createTrigger(Class<E> type,
                                                                                                   String assetId,
                                                                                                   Consumer<ContractNegotiation> action) {
        return new Trigger<>(event -> {
            if (event.getClass().equals(type)) {
                return assetId.equals(((ContractNegotiationEvent) event).getLastContractOffer().getAssetId());
            }
            return false;
        }, action);
    }

    private static <E extends TransferProcessEvent> Trigger<TransferProcess> createTransferTrigger(Class<E> type,
                                                                                                   String agreementId,
                                                                                                   Consumer<TransferProcess> action) {
        return new Trigger<>(event -> {
            if (event.getClass().equals(type)) {
                return agreementId.equals(((TransferProcessEvent) event).getContractId());
            }
            return false;
        }, action);
    }

    private static Asset createAsset(String id) {
        return Asset.Builder.newInstance()
                .id(id)
                .dataAddress(DataAddress.Builder.newInstance().type("HTTP").build())
                .build();
    }

    private static ContractNegotiation createContractNegotiation(String id) {

        return ContractNegotiation.Builder.newInstance()
                .contractAgreement(ContractAgreement.Builder.newInstance()
                        .id(id)
                        .providerId("providerId")
                        .consumerId("TCK_PARTICIPANT")
                        .assetId("ATP0101")
                        .contractSigningDate(System.currentTimeMillis())
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .type(ContractNegotiation.Type.PROVIDER)
                .state(ContractNegotiationStates.FINALIZED.code())
                .counterPartyId("counterPartyId")
                .counterPartyAddress("https://test.com")
                .protocol("test")
                .build();
    }
}
