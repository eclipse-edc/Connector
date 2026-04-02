/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.logic.authorization;

import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignalingAuthorizationRegistryImplTest {

    private final SignalingAuthorizationRegistry registry = new SignalingAuthorizationRegistryImpl();

    @Test
    void register_shouldStoreAuthorizationByType() {
        var auth = authorizationOf("oauth2");

        registry.register(auth);

        assertThat(registry.findByType("oauth2")).isSameAs(auth);
    }

    @Test
    void register_shouldOverwriteExistingEntryWithSameType() {
        var first = authorizationOf("oauth2");
        var second = authorizationOf("oauth2");

        registry.register(first);
        registry.register(second);

        assertThat(registry.findByType("oauth2")).isSameAs(second);
    }

    @Test
    void getAll_shouldReturnAllRegisteredAuthorizations() {
        var auth1 = authorizationOf("oauth2");
        var auth2 = authorizationOf("api-key");

        registry.register(auth1);
        registry.register(auth2);

        assertThat(registry.getAll()).containsExactlyInAnyOrder(auth1, auth2);
    }

    @Test
    void getAll_shouldReturnEmpty_whenNothingRegistered() {
        assertThat(registry.getAll()).isEmpty();
    }

    @Test
    void findByType_shouldReturnNull_whenTypeNotRegistered() {
        assertThat(registry.findByType("unknown")).isNull();
    }

    @Test
    void findByType_shouldReturnNull_afterRegisteringDifferentType() {
        registry.register(authorizationOf("oauth2"));

        assertThat(registry.findByType("api-key")).isNull();
    }

    private SignalingAuthorization authorizationOf(String type) {
        var auth = mock(SignalingAuthorization.class);
        when(auth.getType()).thenReturn(type);
        return auth;
    }
}
