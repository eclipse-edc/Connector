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

package org.eclipse.edc.connector.controlplane.contract;

import org.eclipse.edc.connector.controlplane.contract.negotiation.command.handlers.TerminateNegotiationCommandHandler;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.connector.controlplane.contract.ContractNegotiationCommandExtension.NAME;

/**
 * Adds a {@link CommandHandlerRegistry} to the context and registers the
 * handlers the core provides.
 */
@Extension(value = NAME)
public class ContractNegotiationCommandExtension implements ServiceExtension {

    public static final String NAME = "Contract Negotiation command handlers";

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    private ContractNegotiationStore store;

    @Inject
    private CommandHandlerRegistry registry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        registry.register(new TerminateNegotiationCommandHandler(store));
    }

}
