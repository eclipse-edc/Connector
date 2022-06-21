/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.crypto.helper;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This is not an actual unit test, it is merely a utility to read pem files and get the parameters
 */
public class PemKeyReaderUtility {

    @Test
    @Disabled
    void readPemFile() {
        var jwk1 = parsePemAsJwk("/home/paul/dev/ion-demo/keys2/consumer-public.pem");
        var jwk2 = parsePemAsJwk("/home/paul/dev/ion-demo/keys2/verifier-public.pem");
        var jwk3 = parsePemAsJwk("/home/paul/dev/ion-demo/keys2/provider-public.pem");
        var jwk4 = parsePemAsJwk("/home/paul/dev/ion-demo/keys2/connector3-public.pem");
        var jwk5 = parsePemAsJwk("/home/paul/dev/ion-demo/keys2/connector4-public.pem");
        System.out.println("Public keys: ");
        System.out.printf("consumer: %s%n", jwk1);
        System.out.printf("verifier: %s%n", jwk2);
        System.out.printf("provider: %s%n", jwk3);
        System.out.printf("connector3: %s%n", jwk4);
        System.out.printf("connector4: %s%n", jwk5);
    }


    private JWK parsePemAsJwk(String resourceName) {

        try {
            var pemContents = Files.readString(Path.of(resourceName));
            return ECKey.parseFromPEMEncodedObjects(pemContents);

        } catch (JOSEException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
