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

package org.eclipse.edc.tasks.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationOffered;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessResumed;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessSuspended;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.tasks.tck.dsp.recorder.StepRecorder;

import java.util.List;
import java.util.function.Consumer;

public class GuardAssembly {

    public static StepRecorder<ContractNegotiation> createNegotiationRecorder(ContractNegotiationGuardTask tckTask) {
        var recorder = new StepRecorder<ContractNegotiation>();

        record01NegotiationSequences(recorder, tckTask);
        record02NegotiationSequences(recorder, tckTask);
        record03NegotiationSequences(recorder, tckTask);

        recordC01NegotiationSequences(recorder, tckTask);
        recordC02NegotiationSequences(recorder, tckTask);
        recordC03NegotiationSequences(recorder, tckTask);

        return recorder.repeat();
    }

    public static List<Trigger<ContractNegotiation>> createNegotiationTriggers(ContractNegotiationGuardTask tckTask) {
        return List.of(
                createTrigger(ContractNegotiationOffered.class, "ACN0205", tckTask::sendTermination),
                createTrigger(ContractNegotiationOffered.class, "ACNC0101", tckTask::sendAccept),
                createTrigger(ContractNegotiationOffered.class, "ACNC0102", tckTask::contractRequest),
                createTrigger(ContractNegotiationOffered.class, "ACNC0103", tckTask::sendTermination),
                createTrigger(ContractNegotiationRequested.class, "ACNC0202", tckTask::sendTermination),
                createTrigger(ContractNegotiationOffered.class, "ACNC0205", tckTask::sendAccept),
                createTrigger(ContractNegotiationOffered.class, "ACNC0304", tckTask::sendAccept),
                createTrigger(ContractNegotiationOffered.class, "ACNC0305", tckTask::sendAccept),
                createTrigger(ContractNegotiationOffered.class, "ACNC0306", tckTask::sendAccept));
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

    private static void record01NegotiationSequences(StepRecorder<ContractNegotiation> recorder, ContractNegotiationGuardTask tckTask) {
        recorder.sequence("ACN0101").intercept(tckTask::sendOffer)
                .proceed();

        recorder.sequence("ACN0102")
                .intercept(tckTask::sendOffer)
                .proceed()
                .intercept(tckTask::sendTermination)
                .proceed();

        recorder.sequence("ACN0103")
                .intercept(tckTask::sendOffer)
                .proceed()
                .intercept(tckTask::sendAgreement)
                .proceed()
                .intercept(tckTask::sendFinalize)
                .proceed();

        recorder.sequence("ACN0104")
                .intercept(tckTask::sendAgreement)
                .proceed()
                .intercept(tckTask::sendFinalize)
                .proceed();
    }

    private static void record02NegotiationSequences(StepRecorder<ContractNegotiation> recorder, ContractNegotiationGuardTask tckTask) {
        recorder.sequence("ACN0201")
                .intercept(tckTask::sendTermination)
                .proceed();

        recorder.sequence("ACN0203")
                .intercept(tckTask::sendAgreement)
                .proceed();

        recorder.sequence("ACN0204")
                .intercept(tckTask::sendOffer)
                .proceed();

        recorder.sequence("ACN0205")
                .intercept(tckTask::sendOffer)
                .proceed()
                .proceed();

        recorder.sequence("ACN0206")
                .intercept(tckTask::sendOffer)
                .proceed()
                .intercept(tckTask::sendTermination)
                .proceed();

        recorder.sequence("ACN0207").intercept(tckTask::sendAgreement)
                .proceed()
                .intercept(tckTask::sendTermination)
                .proceed();
    }

    private static void record03NegotiationSequences(StepRecorder<ContractNegotiation> recorder, ContractNegotiationGuardTask tckTask) {
        recorder.sequence("ACN0301")
                .intercept(tckTask::sendAgreement)
                .proceed()
                .intercept(tckTask::sendFinalize)
                .proceed();

        recorder.sequence("ACN0302")
                .intercept(tckTask::sendOffer)
                .proceed();

        recorder.sequence("ACN0303")
                .intercept(tckTask::sendOffer)
                .proceed()
                .skip();

        recorder.sequence("ACN0304").intercept(tckTask::sendOffer)
                .proceed()
                .skip();

    }

    private static void recordC01NegotiationSequences(StepRecorder<ContractNegotiation> recorder, ContractNegotiationGuardTask tckTask) {

        recorder.sequence("ACNC0104")
                .proceed()
                .proceed()
                .intercept(tckTask::sendVerification)
                .proceed();
    }

    private static void recordC02NegotiationSequences(StepRecorder<ContractNegotiation> recorder, ContractNegotiationGuardTask tckTask) {
        recorder.sequence("ACNC0203")
                .proceed()
                .proceed()
                .intercept(tckTask::sendTermination)
                .proceed();

        recorder.sequence("ACNC0204")
                .proceed()
                .proceed()
                .skip();

        recorder.sequence("ACNC0206")
                .proceed()
                .proceed()
                .intercept(tckTask::sendVerification)
                .proceed();
    }

    private static void recordC03NegotiationSequences(StepRecorder<ContractNegotiation> recorder, ContractNegotiationGuardTask tckTask) {


        recorder.sequence("ACNC0302")
                .proceed()
                .proceed()
                .skip();

        recorder.sequence("ACNC0303")
                .proceed()
                .proceed()
                .skip();

        recorder.sequence("ACNC0306")
                .proceed()
                .proceed()
                .proceed()
                .skip();
    }

    public static List<Trigger<TransferProcess>> createTransferProcessTriggers(TransferProcessGuardTask tckTask) {
        return List.of(
                createTransferTrigger(TransferProcessStarted.class, "ATP0101", tckTask::terminateDataFlow),
                createTransferTrigger(TransferProcessStarted.class, "ATP0102", tckTask::completeDataFlow),
                createTransferTrigger(TransferProcessStarted.class, "ATP0103", tckTask::suspendDataFlow),
                createTransferTrigger(TransferProcessSuspended.class, "ATP0103", tckTask::terminateDataFlow),
                createTransferTrigger(TransferProcessStarted.class, "ATP0104", tckTask.suspendAndResumeDataFlow()),
                createTransferTrigger(TransferProcessSuspended.class, "ATP0104", tckTask::resumingRequestedDataFlow),
                createTransferTrigger(TransferProcessResumed.class, "ATP0104", tckTask::resumeDataFlow),
                createTransferTrigger(TransferProcessStarted.class, "ATPC0201", tckTask::terminateDataFlow),
                createTransferTrigger(TransferProcessStarted.class, "ATPC0202", tckTask::completeDataFlow),
                createTransferTrigger(TransferProcessStarted.class, "ATPC0203", tckTask::suspendDataFlow),
                createTransferTrigger(TransferProcessSuspended.class, "ATPC0203", tckTask::terminateDataFlow),
                createTransferTrigger(TransferProcessStarted.class, "ATPC0204", tckTask.suspendAndResumeDataFlow()),
                createTransferTrigger(TransferProcessSuspended.class, "ATPC0204", tckTask::resumeDataFlow),
                createTransferTrigger(TransferProcessRequested.class, "ATPC0205", tckTask::terminateDataFlow)
        );
    }

    public static StepRecorder<TransferProcess> createTransferProcessRecorder(TransferProcessGuardTask tckTask) {
        var recorder = new StepRecorder<TransferProcess>();

        recordProvider01TransferSequences(recorder, tckTask);
        recordProvider02TransferSequences(recorder);
        recordProvider03TransferSequences(recorder);

        return recorder.repeat();
    }

    private static void recordProvider01TransferSequences(StepRecorder<TransferProcess> recorder, TransferProcessGuardTask tckTask) {
        recorder.sequence("ATP0105")
                .intercept(tckTask::terminateDataFlow)
                .proceed();
    }

    private static void recordProvider02TransferSequences(StepRecorder<TransferProcess> recorder) {
        recorder.sequence("ATP0205").skip()
                .proceed();
    }

    private static void recordProvider03TransferSequences(StepRecorder<TransferProcess> recorder) {
        recorder.sequence("ATP0301").skip();
        recorder.sequence("ATP0302").skip();
    }

}
