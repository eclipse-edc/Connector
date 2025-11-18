/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.transfer;

import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.AddProvisionedResourceCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.DeprovisionCompleteCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.edr.DataAddressToEndpointDataReferenceTransformer;
import org.eclipse.edc.connector.controlplane.transfer.listener.TransferProcessEventListener;
import org.eclipse.edc.connector.controlplane.transfer.provision.DeprovisionResponsesHandler;
import org.eclipse.edc.connector.controlplane.transfer.provision.ProvisionResponsesHandler;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.security.ParticipantVault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.time.Clock;

/**
 * Provides core data transfer services to the system.
 */
@Extension(value = TransferCoreExtension.NAME)
public class TransferCoreExtension implements ServiceExtension {

    public static final String NAME = "Transfer Core";

    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private ProvisionManager provisionManager;

    @Inject
    private TransferProcessObservable observable;

    @Inject
    private PolicyArchive policyArchive;

    @Inject
    private CommandHandlerRegistry registry;

    @Inject
    private DataAddressResolver addressResolver;

    @Inject
    private ParticipantVault vault;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private Clock clock;

    @Inject
    private TypeManager typeManager;

    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        registerTypes(typeManager);

        typeTransformerRegistry.register(new DataAddressToEndpointDataReferenceTransformer());

        observable.registerListener(new TransferProcessEventListener(eventRouter));

        var provisionResponsesHandler = new ProvisionResponsesHandler(observable, monitor, vault, typeManager);
        var deprovisionResponsesHandler = new DeprovisionResponsesHandler(observable, monitor, vault);

        registry.register(new AddProvisionedResourceCommandHandler(transferProcessStore, provisionResponsesHandler));
        registry.register(new DeprovisionCompleteCommandHandler(transferProcessStore, deprovisionResponsesHandler));
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(ProvisionedContentResource.class);
        typeManager.registerTypes(DeprovisionedResource.class);
    }
}
