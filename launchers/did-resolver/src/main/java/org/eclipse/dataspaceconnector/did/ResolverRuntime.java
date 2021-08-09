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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.did;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.ion.IonClient;
import org.eclipse.dataspaceconnector.iam.ion.IonClientImpl;
import org.eclipse.dataspaceconnector.iam.ion.IonException;
import org.eclipse.dataspaceconnector.iam.ion.crypto.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.ion.dto.PublicKeyDescriptor;
import org.eclipse.dataspaceconnector.iam.ion.dto.ServiceDescriptor;
import org.eclipse.dataspaceconnector.iam.ion.model.AnchorRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResolverRuntime {

    public static void main(String[] args) {

        Map<String, String> keyAsMap;
        try {

            IonClient client = new IonClientImpl();
            var pair = KeyPairFactory.generateKeyPair();

            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {
            };

            keyAsMap = mapper.readValue(pair.getPublicKey().toJSONString(), typeRef);

            var pk = PublicKeyDescriptor.Builder.create()
                    .publicKeyJwk(keyAsMap)
                    .id("my-key-1")
                    .type("EcdsaSecp256k1VerificationKey2019")
                    .purposes("authentication")
                    .build();

            var sd = ServiceDescriptor.Builder.create()
                    .id("myservice-1")
                    .type("LinkedDomains")
                    .serviceEndpoint("https://this.goes.nowhere/myservice-1")
                    .build();

            var did = client.createDid(pk, Collections.singletonList(sd), "testnet");
            var request = did.create(null);
            var anchorRequest = AnchorRequest.Builder.create()
                    .requestBody(request)
                    .build();


            try {
                client.submit(anchorRequest);
                var uri = did.getUri();
                System.out.printf("** RESOLVING URI %s **%n", did.getUriShort());
                client.resolve(uri);

            } catch (IonException ex) {
                ex.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
