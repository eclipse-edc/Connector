/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.edr.store;

import org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.store.defaults.InMemoryEndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.store.defaults.VaultEndpointDataReferenceCache;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(EndpointDataReferenceStoreDefaultServicesExtension.NAME)
public class EndpointDataReferenceStoreDefaultServicesExtension implements ServiceExtension {

    @Setting(description = "Directory/Path where to store EDRs in the vault for vaults that supports hierarchical structuring.", key = "edc.edr.vault.path", required = false)
    private String vaultPath = "";
    protected static final String NAME = "Endpoint Data Reference Core Default Services Extension";

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;


    @Provider(isDefault = true)
    public EndpointDataReferenceCache endpointDataReferenceCache() {
        return new VaultEndpointDataReferenceCache(vault, vaultPath, typeManager.getMapper());
    }

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public EndpointDataReferenceEntryIndex endpointDataReferenceEntryStore() {
        return new InMemoryEndpointDataReferenceEntryIndex(criterionOperatorRegistry);
    }
}
