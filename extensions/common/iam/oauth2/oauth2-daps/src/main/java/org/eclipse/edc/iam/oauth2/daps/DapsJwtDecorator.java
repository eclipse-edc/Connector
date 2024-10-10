/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.iam.oauth2.daps;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.TokenDecorator;

@Deprecated(since = "0.10.0")
public class DapsJwtDecorator implements TokenDecorator {

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        return tokenParameters.claims("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                .claims("@type", "ids:DatRequestToken");
    }
}
