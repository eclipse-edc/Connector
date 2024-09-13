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

package org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;

import java.util.UUID;


public class TestFunctions {

    public static StsClient createClient(String id, String secretAlias) {
        return createClient(id, secretAlias, id, secretAlias, "did:example:subject");
    }

    public static StsClient createClient(String id, String secretAlias, String clientId) {
        return createClient(id, secretAlias, clientId, secretAlias, "did:example:subject");
    }

    public static StsClient createClient(String id, String secretAlias, String clientId, String publicKeyReference, String did) {
        return createClientBuilder(id)
                .clientId(clientId)
                .name(UUID.randomUUID().toString())
                .secretAlias(secretAlias)
                .publicKeyReference(publicKeyReference)
                .did(did)
                .privateKeyAlias(UUID.randomUUID().toString()).build();
    }

    public static StsClient.Builder createClientBuilder(String id) {
        return StsClient.Builder.newInstance()
                .id(id)
                .name(UUID.randomUUID().toString());
    }

    public static StsClient createClient(String id) {
        return createClient(id, UUID.randomUUID().toString());
    }

}
