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

import org.eclipse.edc.identitytrust.TrustedIssuerRegistry;
import org.eclipse.edc.identitytrust.model.Issuer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple, memory-based implementation of a {@link TrustedIssuerRegistry}
 */
public class DefaultTrustedIssuerRegistry implements TrustedIssuerRegistry {
    private final Map<String, Issuer> store = new HashMap<>();

    @Override
    public void addIssuer(Issuer issuer) {
        store.put(issuer.id(), issuer);
    }

    @Override
    public Issuer getById(String id) {
        return store.get(id);
    }

    @Override
    public Collection<Issuer> getTrustedIssuers() {
        return store.values();
    }
}
