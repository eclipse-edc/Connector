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

package org.eclipse.edc.iam.decentralizedclaims.core.defaults;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple, memory-based implementation of a {@link TrustedIssuerRegistry}.
 */
public class DefaultTrustedIssuerRegistry implements TrustedIssuerRegistry {

    private final Map<String, Set<String>> store = new ConcurrentHashMap<>();

    @Override
    public void register(Issuer issuer, String credentialType) {
        store.computeIfAbsent(issuer.id(), k -> new HashSet<>()).add(credentialType);
    }

    @Override
    public Set<String> getSupportedTypes(Issuer issuer) {
        return store.getOrDefault(issuer.id(), Set.of());
    }

}
