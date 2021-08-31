/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.iam.did.service;

/**
 *
 */
public interface TestDids {

// Resolve ION/IdentityHub discrepancy
//    String HUB_URL_DID = "{\n" +
//            "    \"id\": \"did:ion:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA\",\n" +
//            "    \"@context\":  \"https://www.w3.org/ns/did/v1\",\n" +
//            "    \"services\": [\n" +
//            "      {\n" +
//            "        \"id\": \"IdentityHub\",\n" +
//            "        \"type\": \"IdentityHub\",\n" +
//            "        \"serviceEndpoint\": {\n" +
//            "          \"@context\": \"schema.identity.foundation/hub\",\n" +
//            "          \"@type\": \"UserServiceEndpoint\",\n" +
//            "          \"locations\": [\"https://myhub.com\"]\n" +
//            "        }\n" +
//            "      }\n" +
//            "    ]\n" +
//            "  }";

    String HUB_URL_DID = "{\n" +
            "    \"id\": \"did:ion:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA\",\n" +
            "    \"@context\":  \"https://www.w3.org/ns/did/v1\",\n" +
            "    \"services\": [\n" +
            "      {\n" +
            "        \"id\": \"IdentityHub\",\n" +
            "        \"type\": \"IdentityHub\",\n" +
            "        \"serviceEndpoint\":\"https://myhub.com\""+
            "      }\n" +
            "    ]\n" +
            "  }";
}
