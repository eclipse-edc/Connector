/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.auth;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;

@Extension(value = HashicorpVaultAuthenticationExtension.NAME)
public class HashicorpVaultAuthenticationExtension implements ServiceExtension {
    
    public static final String NAME = "Hashicorp Vault Authentication";
    
    @Configuration
    private HashicorpVaultSettings config;
    
    @Provider(isDefault = true)
    public HashicorpVaultTokenProvider tokenProvider() {
        return new HashicorpVaultTokenProviderImpl(config.token());
    }
}
