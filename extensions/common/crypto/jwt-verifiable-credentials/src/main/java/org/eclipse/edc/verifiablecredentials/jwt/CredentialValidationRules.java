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

package org.eclipse.edc.verifiablecredentials.jwt;

import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules a credential JWT is validated with.
 */
final class CredentialValidationRules {

    private CredentialValidationRules() {
    }

    /**
     * Returns the rules for a VCDM 1.1 credential JWT (with a {@code vc} claim): the rules registered for
     * {@link Constants#JWT_VC_TOKEN_CONTEXT}, and the key must belong to the {@code iss} claim.
     *
     * @param rulesRegistry the registry of the token validation rules
     * @param keyId the {@code kid} header of the credential JWT
     * @return the validation rules
     */
    static List<TokenValidationRule> vcdm11(TokenValidationRulesRegistry rulesRegistry, @Nullable String keyId) {
        var rules = new ArrayList<>(rulesRegistry.getRules(Constants.JWT_VC_TOKEN_CONTEXT));
        rules.add(new IssuerKeyIdValidationRule(keyId));
        return rules;
    }
}
