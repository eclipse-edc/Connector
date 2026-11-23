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

package org.eclipse.edc.iam.did.web;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.did.web.resolution.WebDidResolver;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class WebDidExtensionTest {

    private final EdcHttpClient httpClient = mock();
    private final DidResolverRegistry resolverRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(EdcHttpClient.class, httpClient);
        context.registerService(DidResolverRegistry.class, resolverRegistry);
        context.registerService(TypeManager.class, mock());
        when(httpClient.withCallTimeout(any())).thenReturn(httpClient);
        when(httpClient.withMaxRetries(anyInt())).thenReturn(httpClient);
    }

    @Test
    void initialize_shouldApplyDefaultLimits(ServiceExtensionContext context, ObjectFactory objectFactory) {
        objectFactory.constructInstance(WebDidExtension.class).initialize(context);

        verify(httpClient).withCallTimeout(Duration.parse(WebDidResolver.DEFAULT_CALL_TIMEOUT));
        verify(httpClient).withMaxRetries(WebDidResolver.DEFAULT_MAX_RETRIES);
        verify(resolverRegistry).register(isA(WebDidResolver.class));
    }

    @Test
    void initialize_shouldApplyConfiguredLimits(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.iam.did.web.timeout.call", "PT3S",
                "edc.iam.did.web.retries.max", "0")));

        objectFactory.constructInstance(WebDidExtension.class).initialize(context);

        verify(httpClient).withCallTimeout(Duration.ofSeconds(3));
        verify(httpClient).withMaxRetries(0);
    }

    @Test
    void initialize_withDnsOverHttps_shouldApplyLimitsToDnsClient(ServiceExtensionContext context, ObjectFactory objectFactory) {
        EdcHttpClient dnsClient = mock();
        when(httpClient.withDns("https://dns.example.com")).thenReturn(dnsClient);
        when(dnsClient.withCallTimeout(any())).thenReturn(dnsClient);
        when(dnsClient.withMaxRetries(anyInt())).thenReturn(dnsClient);
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(WebDidExtension.DNS_OVER_HTTPS, "https://dns.example.com")));
        when(context.getSetting(WebDidExtension.DNS_OVER_HTTPS, null)).thenReturn("https://dns.example.com");

        objectFactory.constructInstance(WebDidExtension.class).initialize(context);

        verify(dnsClient).withCallTimeout(Duration.parse(WebDidResolver.DEFAULT_CALL_TIMEOUT));
        verify(dnsClient).withMaxRetries(WebDidResolver.DEFAULT_MAX_RETRIES);
    }
}
