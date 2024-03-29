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
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(EndpointDataReferenceStoreExtension.NAME)
public class EndpointDataReferenceStoreDefaultServicesExtension implements ServiceExtension {

    public static final String DEFAULT_EDR_VAULT_PATH = "";
    @Setting(value = "Directory/Path where to store EDRs in the vault for vaults that supports hierarchical structuring.", defaultValue = DEFAULT_EDR_VAULT_PATH)
    public static final String EDC_EDR_VAULT_PATH = "edc.edr.vault.path";
    protected static final String NAME = "Endpoint Data Reference Core Default Services Extension";

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;


    @Provider(isDefault = true)
    public EndpointDataReferenceCache endpointDataReferenceCache(ServiceExtensionContext context) {
        var vaultDirectory = context.getConfig().getString(EDC_EDR_VAULT_PATH, DEFAULT_EDR_VAULT_PATH);
        return new VaultEndpointDataReferenceCache(vault, vaultDirectory, typeManager.getMapper());
    }

    @Provider(isDefault = true)
    public EndpointDataReferenceEntryIndex endpointDataReferenceEntryStore() {
        return new InMemoryEndpointDataReferenceEntryIndex(criterionOperatorRegistry);
    }
}
