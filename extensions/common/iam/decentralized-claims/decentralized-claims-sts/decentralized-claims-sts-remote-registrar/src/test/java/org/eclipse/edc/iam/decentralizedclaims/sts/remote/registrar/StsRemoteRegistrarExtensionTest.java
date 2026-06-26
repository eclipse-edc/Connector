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

package org.eclipse.edc.iam.decentralizedclaims.sts.remote.registrar;

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenServiceRegistry;
import org.eclipse.edc.iam.decentralizedclaims.sts.remote.RemoteSecureTokenService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.iam.decentralizedclaims.sts.remote.registrar.StsRemoteRegistrarExtension.OAUTH_STS_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class StsRemoteRegistrarExtensionTest {

    private final SecureTokenServiceRegistry registry = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(SecureTokenServiceRegistry.class, registry);
    }

    @Test
    void initialize_shouldRegisterRemoteStsUnderOauthType(StsRemoteRegistrarExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(registry).register(eq(OAUTH_STS_TYPE), any(RemoteSecureTokenService.class));
    }
}
