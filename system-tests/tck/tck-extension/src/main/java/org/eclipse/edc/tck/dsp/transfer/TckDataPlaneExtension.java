/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.transfer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public class TckDataPlaneExtension implements ServiceExtension {
    
    @Inject
    private DataPlaneAuthorizationService authorizationService;
    @Inject
    private PublicEndpointGeneratorService generatorService;

    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        generatorService.addGeneratorFunction("HttpData", address -> Endpoint.url("http://example.com"));

        try {
            var key = new ECKeyGenerator(Curve.P_256)
                    .keyID("sign-key")
                    .generate();
            vault.storeSecret("private-key", key.toJSONString());
            vault.storeSecret("public-key", key.toPublicJWK().toJSONString());
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }


    }
}
