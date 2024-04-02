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

package org.eclipse.edc.iam.identitytrust.core.defaults;

import org.eclipse.edc.iam.identitytrust.spi.model.Issuer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTrustedIssuerRegistryTest {

    private final DefaultTrustedIssuerRegistry registry = new DefaultTrustedIssuerRegistry();

    @Test
    void addIssuer() {
        var issuer = new Issuer("test-id", Map.of());
        registry.addIssuer(issuer);
        assertThat(registry.getTrustedIssuers()).containsExactly(issuer);
    }

    @Test
    void addIssuer_exists_shouldReplace() {
        var issuer = new Issuer("test-id", Map.of());
        var issuer2 = new Issuer("test-id", Map.of("new-key", "new-val"));
        registry.addIssuer(issuer);
        registry.addIssuer(issuer2);
        assertThat(registry.getTrustedIssuers()).containsExactly(issuer2);
    }

    @Test
    void getById() {
        var issuer = new Issuer("test-id", Map.of());
        registry.addIssuer(issuer);
        assertThat(registry.getById("test-id")).isEqualTo(issuer);
    }

    @Test
    void getById_notFound() {
        assertThat(registry.getById("nonexistent-id")).isNull();
    }

    @Test
    void getTrustedIssuers() {
        var issuer = new Issuer("test-id", Map.of());
        var issuer2 = new Issuer("test-id2", Map.of("new-key", "new-val"));
        registry.addIssuer(issuer);
        registry.addIssuer(issuer2);

        assertThat(registry.getTrustedIssuers()).containsExactlyInAnyOrder(issuer2, issuer);
    }
}