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

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.listener.TransferProcessEventListener;
import org.eclipse.edc.connector.controlplane.transfer.processors.TransferProcessorsImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessors;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.statemachine.StateMachineConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;

import java.time.Clock;
import java.util.Collections;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides core data transfer services to the system.
 */
@Extension(value = TransferCoreExtension.NAME)
public class TransferCoreExtension implements ServiceExtension {

    public static final String NAME = "Transfer Core";

    @SettingContext("edc.transfer")
    @Configuration
    private StateMachineConfiguration stateMachineConfiguration;

    @Inject
    private TransferProcessObservable observable;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private PolicyArchive policyArchive;
    @Inject
    private DataFlowController dataFlowController;
    @Inject
    private DataAddressStore dataAddressStore;
    @Inject
    private TransferProcessStore store;
    @Inject
    private Monitor monitor;
    @Inject
    private DataAddressResolver addressResolver;
    @Inject
    private ProtocolWebhookResolver protocolWebhookResolver;
    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var builderFactory = Json.createBuilderFactory(Collections.emptyMap());
        typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        typeTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(builderFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, JSON_LD));

        observable.registerListener(new TransferProcessEventListener(eventRouter));
    }

    @Provider
    public TransferProcessors transferProcessors() {
        var entityRetryProcessConfiguration = stateMachineConfiguration.entityRetryProcessConfiguration();
        var entityRetryProcessFactory = new EntityRetryProcessFactory(monitor, clock, entityRetryProcessConfiguration);
        return new TransferProcessorsImpl(policyArchive, entityRetryProcessFactory, dataFlowController, dataAddressStore,
                observable, store, monitor, addressResolver, protocolWebhookResolver, dispatcherRegistry);
    }

}
