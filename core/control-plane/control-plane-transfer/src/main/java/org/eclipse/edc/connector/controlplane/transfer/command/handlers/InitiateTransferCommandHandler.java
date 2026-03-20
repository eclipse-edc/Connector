/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.InitiateTransferCommand;
import org.eclipse.edc.spi.command.CommandHandler;
import org.eclipse.edc.spi.command.CommandResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.telemetry.Telemetry;

import java.time.Clock;
import java.util.Optional;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;

/**
 * Initiates a transfer process on the consumer side.
 */
public class InitiateTransferCommandHandler implements CommandHandler<InitiateTransferCommand> {

    private final PolicyArchive policyArchive;
    private final TransferProcessStore store;
    private final DataAddressStore dataAddressStore;
    private final TransferProcessObservable observable;
    private final Clock clock;
    private final Telemetry telemetry;
    private final Monitor monitor;

    public InitiateTransferCommandHandler(PolicyArchive policyArchive, TransferProcessStore store,
                                          DataAddressStore dataAddressStore, TransferProcessObservable observable,
                                          Clock clock, Telemetry telemetry, Monitor monitor) {
        this.policyArchive = policyArchive;
        this.store = store;
        this.dataAddressStore = dataAddressStore;
        this.observable = observable;
        this.clock = clock;
        this.telemetry = telemetry;
        this.monitor = monitor;
    }

    @Override
    public Class<InitiateTransferCommand> getType() {
        return InitiateTransferCommand.class;
    }

    @Override
    public CommandResult handle(InitiateTransferCommand command) {
        var transferRequest = command.getRequest();
        var participantContext = command.getParticipantContext();

        var policy = policyArchive.findPolicyForContract(transferRequest.getContractId());
        if (policy == null) {
            return CommandResult.notExecutable("No policy found for contract " + transferRequest.getContractId());
        }

        var process = TransferProcess.Builder.newInstance()
                .id(command.getEntityId())
                .assetId(policy.getTarget())
                .counterPartyAddress(transferRequest.getCounterPartyAddress())
                .contractId(transferRequest.getContractId())
                .protocol(transferRequest.getProtocol())
                .type(CONSUMER)
                .clock(clock)
                .transferType(transferRequest.getTransferType())
                .privateProperties(transferRequest.getPrivateProperties())
                .callbackAddresses(transferRequest.getCallbackAddresses())
                .traceContext(telemetry.getCurrentTraceContext())
                .participantContextId(participantContext.getParticipantContextId())
                .dataplaneMetadata(transferRequest.getDataplaneMetadata())
                .build();

        var dataAddressStorage = Optional.ofNullable(transferRequest.getDataDestination())
                .map(it -> dataAddressStore.store(it, process))
                .orElse(StoreResult.success());

        return  dataAddressStorage
                .compose(v -> update(process))
                .onSuccess(v -> observable.invokeForEach(l -> l.initiated(process)))
                .flatMap(r -> {
                    if (r.succeeded()) {
                        return CommandResult.success(process);
                    } else {
                        return CommandResult.notExecutable("Failed to initiate Transfer Process: " + r.getFailureDetail());
                    }
                });
    }

    protected StoreResult<Void> update(TransferProcess entity) {
        return store.save(entity)
                .onSuccess(ignored -> {
                    monitor.debug(() -> "[%s] %s %s is now in state %s".formatted(this.getClass().getSimpleName(),
                            entity.getClass().getSimpleName(), entity.getId(), entity.stateAsString()));
                });
    }
}
