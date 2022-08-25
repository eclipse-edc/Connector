/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.core.controlplane;

import org.eclipse.dataspaceconnector.core.controlplane.listener.AssetEventListener;
import org.eclipse.dataspaceconnector.core.controlplane.listener.PolicyDefinitionEventListener;
import org.eclipse.dataspaceconnector.core.controlplane.service.AssetServiceImpl;
import org.eclipse.dataspaceconnector.core.controlplane.service.CatalogServiceImpl;
import org.eclipse.dataspaceconnector.core.controlplane.service.PolicyDefinitionServiceImpl;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetService;
import org.eclipse.dataspaceconnector.spi.catalog.service.CatalogService;
import org.eclipse.dataspaceconnector.spi.contract.definition.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetObservableImpl;
import org.eclipse.dataspaceconnector.spi.observe.policydefinition.PolicyDefinitionObservableImpl;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionService;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.time.Clock;

public class ControlPlaneCoreExtension implements ServiceExtension {

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private AssetLoader assetLoader;

    @Inject
    private PolicyDefinitionStore policyStore;

    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcher;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return "Control Plane core services";
    }

    @Provider
    public AssetService assetService() {
        var assetObservable = new AssetObservableImpl();
        assetObservable.registerListener(new AssetEventListener(clock, eventRouter));

        return new AssetServiceImpl(assetIndex, assetLoader, contractNegotiationStore, transactionContext, assetObservable);
    }

    @Provider
    public CatalogService catalogService() {
        return new CatalogServiceImpl(dispatcher);
    }

    @Provider
    public PolicyDefinitionService policyDefinitionService() {
        var policyDefinitionObservable = new PolicyDefinitionObservableImpl();
        policyDefinitionObservable.registerListener(new PolicyDefinitionEventListener(clock, eventRouter));

        return new PolicyDefinitionServiceImpl(transactionContext, policyStore, contractDefinitionStore, policyDefinitionObservable);
    }

}
