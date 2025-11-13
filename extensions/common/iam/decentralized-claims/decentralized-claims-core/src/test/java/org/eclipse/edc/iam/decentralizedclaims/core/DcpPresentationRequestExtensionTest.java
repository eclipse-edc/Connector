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

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.core.defaults.DefaultCredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.lib.DefaultPresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToPresentationResponseMessageTransformer;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.core.DcpPresentationRequestExtension.DCP_CLIENT_CONTEXT;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DcpPresentationRequestExtensionTest {

    private static final String DCP_08_PROPERTY  = "edc.dcp.v08.forced";

    private final TypeTransformerRegistry transformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(SecureTokenService.class, mock());
        context.registerService(DidResolverRegistry.class, mock());
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);
        context.registerService(TypeManager.class, mock());
        context.registerService(EdcHttpClient.class, mock());
        context.registerService(JsonLd.class, mock());

        var config = ConfigFactory.fromMap(Map.of(
                DCP_08_PROPERTY, "false"

        ));
        when(context.getConfig()).thenReturn(config);
        when(transformerRegistry.forContext(DCP_CLIENT_CONTEXT)).thenReturn(transformerRegistry);
    }

    @Test
    void verifyPresentationRequestService(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var service = objectFactory.constructInstance(DcpPresentationRequestExtension.class).presentationRequestService(context);
        assertThat(service).isNotNull().isInstanceOf(DefaultPresentationRequestService.class);
    }

    @Test
    void verifyCredentialServiceClient(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var service = objectFactory.constructInstance(DcpPresentationRequestExtension.class).credentialServiceClient(context);

        assertThat(service).isNotNull().isInstanceOf(DefaultCredentialServiceClient.class);
        verify(transformerRegistry, times(1)).forContext(DCP_CLIENT_CONTEXT);
        verify(transformerRegistry, times(1)).register(isA(JsonObjectToPresentationResponseMessageTransformer.class));
    }
}
