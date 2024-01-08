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

package org.eclipse.edc.token.spi;

import java.util.List;

/**
 * Registry for {@link TokenValidationRule}.
 */
public interface TokenValidationRulesRegistry {
    String DEFAULT_CONTEXT = "default";

    void addRule(String context, TokenValidationRule rule);

    List<TokenValidationRule> getRules(String context);
}
