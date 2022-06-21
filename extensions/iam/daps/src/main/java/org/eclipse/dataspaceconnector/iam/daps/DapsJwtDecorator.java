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

package org.eclipse.dataspaceconnector.iam.daps;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.JwtDecorator;

public class DapsJwtDecorator implements JwtDecorator {
    @Override
    public void decorate(JWSHeader.Builder header, JWTClaimsSet.Builder claimsSet) {
        claimsSet.claim("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                .claim("@type", "ids:DatRequestToken");
    }
}
