/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.core;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.iam.identitytrust.service.IdentityAndTrustService;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.identitytrust.core.IdentityAndTrustExtension.DCP_CLIENT_CONTEXT;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class IdentityAndTrustExtensionTest {

    private static final String CONNECTOR_DID_PROPERTY = "edc.iam.issuer.id";
    private static final String CLEANUP_PERIOD = "edc.sql.store.jti.cleanup.period";
    private final JtiValidationStore storeMock = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(SecureTokenService.class, mock());
        context.registerService(TypeManager.class, new JacksonTypeManager());
        context.registerService(JtiValidationStore.class, storeMock);
        context.registerService(ExecutorInstrumentation.class, ExecutorInstrumentation.noop());
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);

        var config = ConfigFactory.fromMap(Map.of(
                CONNECTOR_DID_PROPERTY, "did:web:test",
                CLEANUP_PERIOD, "1"
        ));
        when(context.getConfig()).thenReturn(config);
        when(transformerRegistry.forContext(DCP_CLIENT_CONTEXT)).thenReturn(transformerRegistry);
    }

    @Test
    void verifyCorrectService(ServiceExtensionContext context, ObjectFactory objectFactory) {


        var is = objectFactory.constructInstance(IdentityAndTrustExtension.class).createIdentityService(context);

        assertThat(is).isInstanceOf(IdentityAndTrustService.class);
    }

    @Test
    void assertReaperThreadRunning(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var extension = objectFactory.constructInstance(IdentityAndTrustExtension.class);
        extension.initialize(context);
        extension.start();

        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> verify(storeMock, atLeastOnce()).deleteExpired());
    }

}
