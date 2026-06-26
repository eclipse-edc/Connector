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

package org.eclipse.edc.iam.decentralizedclaims.sts.registry;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
class SecureTokenServiceRegistryExtensionTest {

    @Test
    void secureTokenServiceRegistry_shouldReturnSameInstance(SecureTokenServiceRegistryExtension extension) {
        assertThat(extension.secureTokenServiceRegistry())
                .isInstanceOf(SecureTokenServiceRegistryImpl.class)
                .isSameAs(extension.secureTokenServiceRegistry());
    }

    @Test
    void secureTokenService_shouldProvideDelegatingService(SecureTokenServiceRegistryExtension extension, ServiceExtensionContext context) {
        assertThat(extension.secureTokenService()).isInstanceOf(DelegatingSecureTokenService.class);
    }
}
