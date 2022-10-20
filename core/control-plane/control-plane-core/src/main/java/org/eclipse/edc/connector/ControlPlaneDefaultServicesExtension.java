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

package org.eclipse.edc.connector;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.defaults.storage.assetindex.InMemoryAssetIndex;
import org.eclipse.edc.connector.defaults.storage.contractdefinition.InMemoryContractDefinitionStore;
import org.eclipse.edc.connector.defaults.storage.contractnegotiation.InMemoryContractNegotiationStore;
import org.eclipse.edc.connector.defaults.storage.policydefinition.InMemoryPolicyDefinitionStore;
import org.eclipse.edc.connector.defaults.storage.transferprocess.InMemoryTransferProcessStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.util.concurrency.LockManager;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides default service implementations for fallback
 */
@Extension(value = ControlPlaneDefaultServicesExtension.NAME)
public class ControlPlaneDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Control Plane Default Services";
    private InMemoryAssetIndex assetIndex;
    private InMemoryContractDefinitionStore contractDefinitionStore;

    @Override
    public String name() {
        return NAME;
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
