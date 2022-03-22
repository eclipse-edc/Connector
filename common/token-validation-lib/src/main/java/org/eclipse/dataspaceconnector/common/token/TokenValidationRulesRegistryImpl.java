/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.common.token;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory implementation of {@link TokenValidationRulesRegistry}
 */
public class TokenValidationRulesRegistryImpl implements TokenValidationRulesRegistry {

    private final List<TokenValidationRule> rules = new ArrayList<>();

    @Override
    public void addRule(TokenValidationRule rule) {
        rules.add(rule);
    }

    @Override
    public List<TokenValidationRule> getRules() {
        return new ArrayList<>(rules);
    }
}
