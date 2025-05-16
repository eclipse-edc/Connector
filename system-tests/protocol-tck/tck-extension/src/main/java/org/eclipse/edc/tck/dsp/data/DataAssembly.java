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
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessSuspended;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.tck.dsp.guard.Trigger;
import org.eclipse.edc.tck.dsp.recorder.StepRecorder;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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


    private static final Set<String> AGREEMENT_IDS = Set.of(
            "ATP0101", "ATP0102", "ATP0103", "ATP0104", "ATP0105",
            "ATP0201", "ATP0202", "ATP0203", "ATP0204", "ATP0205",
            "ATP0301", "ATP0302", "ATP0303", "ATP0304", "ATP0305", "ATP0306",
            "ATPC0101", "ATPC0102", "ATPC0103", "ATPC0104", "ATPC0105",
            "ATPC0201", "ATPC0202", "ATPC0203", "ATPC0204", "ATPC0205",
            "ATPC0301", "ATPC0302", "ATPC0303", "ATPC0304", "ATPC0305", "ATPC0306");

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
                createTrigger(ContractNegotiationOffered.class, "ACNC0101", contractNegotiation -> {
                    contractNegotiation.transitionAccepting();
                    contractNegotiation.setPending(false);
                }),
                createTrigger(ContractNegotiationAgreed.class, "ACNC0101", contractNegotiation -> {
                    contractNegotiation.transitionVerifying();
                    contractNegotiation.setPending(false);
                }),
                createTrigger(ContractNegotiationOffered.class, "ACNC0102", contractNegotiation -> {
                    contractNegotiation.transitionRequesting();
                    contractNegotiation.setPending(false);
                }),
                createTrigger(ContractNegotiationOffered.class, "ACNC0103", ContractNegotiation::transitionTerminating),
                createTrigger(ContractNegotiationAgreed.class, "ACNC0104", contractNegotiation -> {
                    contractNegotiation.transitionVerifying();
                    contractNegotiation.setPending(false);
                }),
                createTrigger(ContractNegotiationRequested.class, "ACNC0202", ContractNegotiation::transitionTerminating),
                createTrigger(ContractNegotiationAgreed.class, "ACNC0203", ContractNegotiation::transitionTerminating),
                createTrigger(ContractNegotiationOffered.class, "ACNC0205", contractNegotiation -> {
                    contractNegotiation.transitionAccepting();
                    contractNegotiation.setPending(false);
                }),
                createTrigger(ContractNegotiationAgreed.class, "ACNC0206", contractNegotiation -> {
                    contractNegotiation.transitionVerifying();
                    contractNegotiation.setPending(false);
                }),
                createTrigger(ContractNegotiationOffered.class, "ACNC0304", contractNegotiation -> {
                    contractNegotiation.transitionAccepting();
                    contractNegotiation.setPending(false);
                }),
                createTrigger(ContractNegotiationOffered.class, "ACNC0305", contractNegotiation -> {
                    contractNegotiation.transitionAccepting();
                    contractNegotiation.setPending(false);
                }),
                createTrigger(ContractNegotiationOffered.class, "ACNC0306", contractNegotiation -> {
                    contractNegotiation.transitionAccepting();
                    contractNegotiation.setPending(false);
                })

        );
    }

    public static StepRecorder<TransferProcess> createTransferProcessRecorder() {
        var recorder = new StepRecorder<TransferProcess>();

        recordProvider01TransferSequences(recorder);
        recordProvider02TransferSequences(recorder);
        recordProvider03TransferSequences(recorder);

        recordConsumer01TransferSequences(recorder);
        recordConsumer02TransferSequences(recorder);
        recordConsumer03TransferSequences(recorder);

        return recorder.repeat();
    }

    private static void recordProvider01TransferSequences(StepRecorder<TransferProcess> recorder) {
        recorder.record("ATP0101", TransferProcess::transitionStarting);
        recorder.record("ATP0102", TransferProcess::transitionStarting);
        recorder.record("ATP0103", TransferProcess::transitionStarting);
        recorder.record("ATP0104", TransferProcess::transitionStarting);
        recorder.record("ATP0105", TransferProcess::transitionTerminating);

    }

    private static void recordProvider02TransferSequences(StepRecorder<TransferProcess> recorder) {
        recorder.record("ATP0201", TransferProcess::transitionStarting);
        recorder.record("ATP0202", TransferProcess::transitionStarting);
        recorder.record("ATP0203", TransferProcess::transitionStarting);
        recorder.record("ATP0204", TransferProcess::transitionStarting);
    }

    private static void recordProvider03TransferSequences(StepRecorder<TransferProcess> recorder) {
        recorder.record("ATP0301", TransferProcess::transitionStarting);
        recorder.record("ATP0302", TransferProcess::transitionStarting);
        recorder.record("ATP0303", TransferProcess::transitionStarting);
        recorder.record("ATP0304", TransferProcess::transitionStarting);
        recorder.record("ATP0305", TransferProcess::transitionStarting);
        recorder.record("ATP0306", TransferProcess::transitionStarting);
    }

    private static void recordConsumer01TransferSequences(StepRecorder<TransferProcess> recorder) {
        recorder.record("ATPC0101", TransferProcess::transitionRequesting);
        recorder.record("ATPC0102", TransferProcess::transitionRequesting);
        recorder.record("ATPC0103", TransferProcess::transitionRequesting);
        recorder.record("ATPC0104", TransferProcess::transitionRequesting);
        recorder.record("ATPC0105", TransferProcess::transitionRequesting);
    }

    private static void recordConsumer02TransferSequences(StepRecorder<TransferProcess> recorder) {
        recorder.record("ATPC0201", TransferProcess::transitionRequesting);
        recorder.record("ATPC0202", TransferProcess::transitionRequesting);
        recorder.record("ATPC0203", TransferProcess::transitionRequesting);
        recorder.record("ATPC0204", TransferProcess::transitionRequesting);
        recorder.record("ATPC0205", TransferProcess::transitionRequesting);
    }

    private static void recordConsumer03TransferSequences(StepRecorder<TransferProcess> recorder) {
        recorder.record("ATPC0301", TransferProcess::transitionRequesting);
        recorder.record("ATPC0302", TransferProcess::transitionRequesting);
        recorder.record("ATPC0303", TransferProcess::transitionRequesting);
        recorder.record("ATPC0304", TransferProcess::transitionRequesting);
        recorder.record("ATPC0305", TransferProcess::transitionRequesting);
        recorder.record("ATPC0306", TransferProcess::transitionRequesting);
    }

    public static List<Trigger<TransferProcess>> createTransferProcessTriggers() {
        return List.of(
                createTransferTrigger(TransferProcessStarted.class, "ATP0101", TransferProcess::transitionTerminating),
                createTransferTrigger(TransferProcessStarted.class, "ATP0102", TransferProcess::transitionCompleting),
                createTransferTrigger(TransferProcessStarted.class, "ATP0103", (process) -> process.transitionSuspending("suspending")),
                createTransferTrigger(TransferProcessSuspended.class, "ATP0103", (process) -> process.transitionTerminating("terminating")),
                createTransferTrigger(TransferProcessStarted.class, "ATP0104", suspendResumeTrigger()),
                createTransferTrigger(TransferProcessSuspended.class, "ATP0104", TransferProcess::transitionStarting),
                createTransferTrigger(TransferProcessInitiated.class, "ATP0205", (process) -> process.setPending(true)),
                createTransferTrigger(TransferProcessStarted.class, "ATPC0201", TransferProcess::transitionTerminating),
                createTransferTrigger(TransferProcessStarted.class, "ATPC0202", TransferProcess::transitionCompleting),
                createTransferTrigger(TransferProcessStarted.class, "ATPC0203", (process) -> process.transitionSuspending("suspending")),
                createTransferTrigger(TransferProcessSuspended.class, "ATPC0203", TransferProcess::transitionTerminating),
                createTransferTrigger(TransferProcessStarted.class, "ATPC0204", suspendResumeTrigger()),
                createTransferTrigger(TransferProcessSuspended.class, "ATPC0204", TransferProcess::transitionStarting),
                createTransferTrigger(TransferProcessRequested.class, "ATPC0205", (process) -> process.transitionTerminating("error"))
        );
    }

    public static Consumer<TransferProcess> suspendResumeTrigger() {
        var count = new AtomicInteger(0);
        return (process) -> {
            if (count.get() == 0) {
                count.incrementAndGet();
                process.transitionSuspending("suspending");
            } else if (count.get() == 1) {
                count.set(0);
                process.transitionCompleting();
            }
        };
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
