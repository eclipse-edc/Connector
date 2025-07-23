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

package org.eclipse.edc.connector.controlplane;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.defaults.callback.CallbackRegistryImpl;
import org.eclipse.edc.connector.controlplane.defaults.storage.assetindex.InMemoryAssetIndex;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractdefinition.InMemoryContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractnegotiation.InMemoryContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.defaults.storage.policydefinition.InMemoryPolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.defaults.storage.transferprocess.InMemoryTransferProcessStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.profile.DataspaceProfileContextRegistryImpl;
import org.eclipse.edc.connector.controlplane.query.asset.AssetPropertyLookup;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;

/**
 * Provides default service implementations for fallback
 */
@Extension(value = ControlPlaneDefaultServicesExtension.NAME)
public class ControlPlaneDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Control Plane Default Services";
    private InMemoryAssetIndex assetIndex;
    private InMemoryContractDefinitionStore contractDefinitionStore;
    @Inject
    private Clock clock;
    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        criterionOperatorRegistry.registerPropertyLookup(new AssetPropertyLookup());
    }

    @Provider(isDefault = true)
    public AssetIndex defaultAssetIndex() {
        return getAssetIndex();
    }

    @Provider(isDefault = true)
    public DataAddressResolver defaultDataAddressResolver() {
        return getAssetIndex();
    }

    @Provider(isDefault = true)
    public ContractDefinitionStore defaultContractDefinitionStore() {
        return getContractDefinitionStore();
    }

    @Provider(isDefault = true)
    public ContractNegotiationStore defaultContractNegotiationStore() {
        return new InMemoryContractNegotiationStore(clock, criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public TransferProcessStore defaultTransferProcessStore() {
        return new InMemoryTransferProcessStore(clock, criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public PolicyDefinitionStore defaultPolicyStore() {
        return new InMemoryPolicyDefinitionStore(criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public CallbackRegistry defaultCallbackRegistry() {
        return new CallbackRegistryImpl();
    }

    @Provider(isDefault = true)
    public DataspaceProfileContextRegistry dataspaceProfileContextRegistry() {
        return new DataspaceProfileContextRegistryImpl();
    }

    private ContractDefinitionStore getContractDefinitionStore() {
        if (contractDefinitionStore == null) {
            contractDefinitionStore = new InMemoryContractDefinitionStore(criterionOperatorRegistry);
        }
        return contractDefinitionStore;
    }

    private InMemoryAssetIndex getAssetIndex() {
        if (assetIndex == null) {
            assetIndex = new InMemoryAssetIndex(criterionOperatorRegistry);
        }
        return assetIndex;
    }
}
