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

package org.eclipse.edc.connector.controlplane.contract.negotiation.command.handlers;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.InitiateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.command.CommandHandler;
import org.eclipse.edc.spi.command.CommandResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.telemetry.Telemetry;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;

/**
 * Initiates a contract negotiation for the given provider offer. The offer will have been obtained from a previous
 * contract offer request sent to the provider.
 */
public class InitiateNegotiationCommandHandler implements CommandHandler<InitiateNegotiationCommand> {

    private final ContractNegotiationStore store;
    private final ContractNegotiationObservable observable;
    private final Telemetry telemetry;
    private final Monitor monitor;

    public InitiateNegotiationCommandHandler(ContractNegotiationStore store, ContractNegotiationObservable observable,
                                             Telemetry telemetry, Monitor monitor) {
        this.store = store;
        this.observable = observable;
        this.telemetry = telemetry;
        this.monitor = monitor;
    }

    @Override
    public Class<InitiateNegotiationCommand> getType() {
        return InitiateNegotiationCommand.class;
    }

    /**
     * Initiates a new {@link ContractNegotiation}. The ContractNegotiation is created and persisted, which moves it to
     * state REQUESTING.
     *
     * @param command the command;
     * @return success if initialized, failure otherwise.
     */
    @WithSpan
    @Override
    public CommandResult handle(InitiateNegotiationCommand command) {
        var request = command.getRequest();
        var participantContext = command.getParticipantContext();

        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(command.getEntityId())
                .protocol(request.getProtocol())
                .counterPartyId(request.getProviderId())
                .counterPartyAddress(request.getCounterPartyAddress())
                .callbackAddresses(request.getCallbackAddresses())
                .traceContext(telemetry.getCurrentTraceContext())
                .participantContextId(participantContext.getParticipantContextId())
                .type(CONSUMER)
                .build();

        negotiation.addContractOffer(request.getContractOffer());
        negotiation.transitionInitial();
        var save = store.save(negotiation);
        if (save.failed()) {
            return CommandResult.notExecutable(save.getFailureDetail());
        }

        monitor.debug(() -> "[%s] %s %s is now in state %s".formatted(this.getClass().getSimpleName(),
                negotiation.getClass().getSimpleName(), negotiation.getId(), negotiation.stateAsString()));

        observable.invokeForEach(l -> l.initiated(negotiation));

        return CommandResult.success(negotiation);
    }

}
