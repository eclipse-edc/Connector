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

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.core.controlplane.defaults.assetindex.InMemoryAssetIndex;
import org.eclipse.dataspaceconnector.core.controlplane.defaults.contractdefinition.InMemoryContractDefinitionStore;
import org.eclipse.dataspaceconnector.core.controlplane.defaults.negotiationstore.InMemoryContractNegotiationStore;
import org.eclipse.dataspaceconnector.core.controlplane.defaults.policystore.InMemoryPolicyDefinitionStore;
import org.eclipse.dataspaceconnector.core.controlplane.defaults.transferprocessstore.InMemoryTransferProcessStore;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides default service implementations for fallback
 */
public class ControlPlaneDefaultServicesExtension implements ServiceExtension {

    private InMemoryAssetIndex assetIndex;
    private InMemoryContractDefinitionStore contractDefinitionStore;

    @Override
    public String name() {
        return "Control Plane Default Services";
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
        return new InMemoryContractNegotiationStore();
    }

    @Provider(isDefault = true)
    public TransferProcessStore defaultTransferProcessStore() {
        return new InMemoryTransferProcessStore();
    }

    @Provider(isDefault = true)
    public PolicyDefinitionStore defaultPolicyStore() {
        return new InMemoryPolicyDefinitionStore(new LockManager(new ReentrantReadWriteLock(true)));
    }

    private ContractDefinitionStore getContractDefinitionStore() {
        if (contractDefinitionStore == null) {
            contractDefinitionStore = new InMemoryContractDefinitionStore();
        }
        return contractDefinitionStore;
    }

    private InMemoryAssetIndex getAssetIndex() {
        if (assetIndex == null) {
            assetIndex = new InMemoryAssetIndex();
        }
        return assetIndex;
    }
}
