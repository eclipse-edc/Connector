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

package org.eclipse.edc.iam.decentralizedclaims.ldp;

import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifierRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.iam.decentralizedclaims.ldp.DcpLdpVerifierExtension.JSON_2020_SIGNATURE_SUITE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DcpLdpVerifierExtensionTest {

    private final CredentialVerifierRegistry credentialVerifierRegistry = mock();
    private final SignatureSuiteRegistry signatureSuiteRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(CredentialVerifierRegistry.class, credentialVerifierRegistry);
        context.registerService(SignatureSuiteRegistry.class, signatureSuiteRegistry);
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void initialize_shouldRegisterLdpVerifierAndSignatureSuite(DcpLdpVerifierExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(credentialVerifierRegistry).register(isA(LdpVerifier.class));
        verify(signatureSuiteRegistry).register(eq(JSON_2020_SIGNATURE_SUITE), isA(Jws2020SignatureSuite.class));
    }
}
