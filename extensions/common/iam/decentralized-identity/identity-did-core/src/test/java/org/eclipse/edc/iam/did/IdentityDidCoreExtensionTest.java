/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.did;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class IdentityDidCoreExtensionTest {

    private static final String DID = "did:test:participant";
    private final DidResolver resolver = mock();

    @BeforeEach
    void setUp(TestExtensionContext context) {
        context.registerService(KeyParserRegistry.class, mock());
        context.registerService(Clock.class, Clock.systemUTC());
        when(resolver.getMethod()).thenReturn("test");
        when(resolver.resolve(anyString())).thenReturn(Result.success(DidDocument.Builder.newInstance().id(DID).build()));
    }

    @Test
    void initialize_shouldCacheDidDocuments(TestExtensionContext context, ObjectFactory objectFactory) {
        objectFactory.constructInstance(IdentityDidCoreExtension.class).initialize(context);
        var registry = context.getService(DidResolverRegistry.class);
        registry.register(resolver);

        registry.resolve(DID);
        registry.resolve(DID);

        verify(resolver, times(1)).resolve(DID);
    }

    @Test
    void initialize_whenCacheSizeIsZero_shouldNotCacheDidDocuments(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.fromMap(Map.of("edc.did.resolver.cache.size", "0")));
        objectFactory.constructInstance(IdentityDidCoreExtension.class).initialize(context);
        var registry = context.getService(DidResolverRegistry.class);
        registry.register(resolver);

        registry.resolve(DID);
        registry.resolve(DID);

        verify(resolver, times(2)).resolve(DID);
    }
}
