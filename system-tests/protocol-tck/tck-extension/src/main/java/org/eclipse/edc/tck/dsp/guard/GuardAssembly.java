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

package org.eclipse.edc.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAccepted;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationOffered;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessSuspended;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.tck.dsp.recorder.StepRecorder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;

/**
 * Assembles guard for the TCK scenarios.
 */
public class GuardAssembly {
    private GuardAssembly() {
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
                .record("ACN0303", GuardAssembly::noop);

        recorder.record("ACN0304", ContractNegotiation::transitionOffering)
                .record("ACN0304", GuardAssembly::noop);

    }

    public static List<Trigger<ContractNegotiation>> createNegotiationTriggers() {
        return List.of(
                createTrigger(ContractNegotiationOffered.class, "ACN0205", ContractNegotiation::transitionTerminating),
                createTrigger(ContractNegotiationAccepted.class, "ACN0206", ContractNegotiation::transitionTerminating),

                createTrigger(ContractNegotiationAccepted.class, "ACN0303", cn -> {
                    cn.setPending(true);
                }),

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
                }),
                createTrigger(ContractNegotiationAgreed.class, "ACNC0306", contractNegotiation -> {
                    contractNegotiation.setPending(true);
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
        recorder.record("ATP0105", tp -> tp.transitionTerminating("error"));

    }

    private static void recordProvider02TransferSequences(StepRecorder<TransferProcess> recorder) {
        recorder.record("ATP0201", TransferProcess::transitionStarting);
        recorder.record("ATP0202", TransferProcess::transitionStarting);
        recorder.record("ATP0203", TransferProcess::transitionStarting);
        recorder.record("ATP0204", TransferProcess::transitionStarting);
    }

    private static void recordProvider03TransferSequences(StepRecorder<TransferProcess> recorder) {
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
                createTransferTrigger(TransferProcessStarted.class, "ATP0101", tp -> tp.transitionTerminating("error")),
                createTransferTrigger(TransferProcessStarted.class, "ATP0102", TransferProcess::transitionCompleting),
                createTransferTrigger(TransferProcessStarted.class, "ATP0103", (process) -> process.transitionSuspending("suspending")),
                createTransferTrigger(TransferProcessSuspended.class, "ATP0103", (process) -> process.transitionTerminating("terminating")),
                createTransferTrigger(TransferProcessStarted.class, "ATP0104", suspendResumeTrigger()),
                createTransferTrigger(TransferProcessSuspended.class, "ATP0104", TransferProcess::transitionStarting),
                createTransferTrigger(TransferProcessInitiated.class, "ATP0205", (process) -> process.setPending(true)),
                createTransferTrigger(TransferProcessInitiated.class, "ATP0301", (process) -> process.setPending(true)),
                createTransferTrigger(TransferProcessInitiated.class, "ATP0302", (process) -> process.setPending(true)),
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


    private static void noop(ContractNegotiation cn) {

    }
}
