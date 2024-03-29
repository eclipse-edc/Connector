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

package org.eclipse.edc.edr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.edr.defaults.InMemoryEndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.defaults.VaultEndpointDataReferenceCache;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.edr.EndpointDataReferenceStoreDefaultServicesExtension.EDC_EDR_VAULT_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class EndpointDataReferenceStoreDefaultServicesExtensionTest {

    private final Vault vault = mock();

    private final TypeManager typeManager = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(Vault.class, vault);
        context.registerService(TypeManager.class, typeManager);
    }

    @Test
    void initializeTheCache(ServiceExtensionContext context, EndpointDataReferenceStoreDefaultServicesExtension extension) {

        var cache = extension.endpointDataReferenceCache(context);

        assertThat(cache).isInstanceOf(VaultEndpointDataReferenceCache.class);
    }

    @Test
    void initializeTheCache_withPath(ServiceExtensionContext context, EndpointDataReferenceStoreDefaultServicesExtension extension) {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(eq(EDC_EDR_VAULT_PATH), any())).thenReturn("path/");
        when(typeManager.getMapper()).thenReturn(new ObjectMapper());

        var cache = extension.endpointDataReferenceCache(context);

        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        assertThat(cache).isInstanceOf(VaultEndpointDataReferenceCache.class);

        cache.put("id", DataAddress.Builder.newInstance().type("type").build());

        verify(vault).storeSecret(argThat(s -> s.startsWith("path/")), any());
    }

    @Test
    void initializeTheStore(ServiceExtensionContext context, EndpointDataReferenceStoreDefaultServicesExtension extension) {

        var store = extension.endpointDataReferenceEntryStore();

        assertThat(store).isInstanceOf(InMemoryEndpointDataReferenceEntryIndex.class);
    }

}
