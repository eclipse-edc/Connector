/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials.spi.validation;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;

import java.util.Set;

/**
 * Trusted issuer registry.
 */
public interface TrustedIssuerRegistry {

    String WILDCARD = "*";

    /**
     * Register a supported type for a trusted issuer.
     *
     * @param issuer         the issuer
     * @param credentialType supported credential type for this issuer
     */
    void register(Issuer issuer, String credentialType);

    /**
     * Get the supported types for a given issuer.
     *
     * @param issuer the issuer
     * @return set of supported credential types for this issuer.
     */
    Set<String> getSupportedTypes(Issuer issuer);

}

