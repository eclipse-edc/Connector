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

package org.eclipse.edc.iam.oauth2.daps;

import org.eclipse.edc.spi.iam.TokenDecorator;
import org.eclipse.edc.spi.iam.TokenParameters;

/**
 * Token decorator that sets the {@code scope} claim on the token that is used on DSP request egress
 *
 * @deprecated In future releases this will be replaced by a config value "edc.iam.token.scope"
 */
@Deprecated(since = "0.1.0")
public class DapsTokenDecorator implements TokenDecorator {
    private final String scope;

    public DapsTokenDecorator(String configuredScope) {
        this.scope = configuredScope;
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParametersBuilder) {
        return tokenParametersBuilder.scope(scope);
    }
}
