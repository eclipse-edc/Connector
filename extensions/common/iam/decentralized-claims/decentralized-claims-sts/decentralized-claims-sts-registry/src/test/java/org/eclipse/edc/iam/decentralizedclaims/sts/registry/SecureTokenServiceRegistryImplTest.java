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

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecureTokenServiceRegistryImplTest {

    private final SecureTokenServiceRegistryImpl registry = new SecureTokenServiceRegistryImpl();

    @Test
    void resolve_whenRegistered_shouldReturnImplementation() {
        var sts = mock(SecureTokenService.class);
        registry.register("oauth", sts);

        assertThat(registry.resolve("oauth")).isSameAs(sts);
    }

    @Test
    void resolve_whenNotRegistered_shouldReturnNull() {
        assertThat(registry.resolve("embedded")).isNull();
    }

    @Test
    void register_whenTypeAlreadyExists_shouldOverwrite() {
        var first = mock(SecureTokenService.class);
        var second = mock(SecureTokenService.class);
        registry.register("oauth", first);
        registry.register("oauth", second);

        assertThat(registry.resolve("oauth")).isSameAs(second);
    }
}
