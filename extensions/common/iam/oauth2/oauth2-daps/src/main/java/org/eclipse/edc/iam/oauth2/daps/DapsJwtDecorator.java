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

import org.eclipse.edc.token.spi.JwtDecorator;

import java.util.Map;

import static java.util.Collections.emptyMap;

public class DapsJwtDecorator implements JwtDecorator {

    @Override
    public Map<String, Object> claims() {
        return Map.of(
                "@context", "https://w3id.org/idsa/contexts/context.jsonld",
                "@type", "ids:DatRequestToken"
        );
    }

    @Override
    public Map<String, Object> headers() {
        return emptyMap();
    }
}
